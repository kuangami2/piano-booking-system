package com.roomreservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.common.EventTypes;
import com.roomreservation.entity.EventOutbox;
import com.roomreservation.event.EventEnvelope;
import com.roomreservation.mapper.EventOutboxMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 事件总线：与业务同事务写本地消息表，事务提交后投递 MQ；
 * broker 不可用时返回 false，由调用方走同步降级，并留下 degraded 记录供对账。
 */
@Service
public class EventBusService {

    private static final Logger log = LoggerFactory.getLogger(EventBusService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private EventOutboxMapper outboxMapper;
    @Resource
    private MqAvailabilityService mqAvailability;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 发布事件，返回 true 表示已受理异步投递，false 表示当前不可用需同步降级。
     */
    public boolean publish(String eventType, String routingKey, Object payload) {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        EventEnvelope envelope = new EventEnvelope(eventId, eventType,
                LocalDateTime.now().format(FORMATTER), 1, payload);
        boolean async = mqAvailability.reachable();
        EventOutbox record = new EventOutbox();
        record.setEventId(eventId);
        record.setEventType(eventType);
        record.setRoutingKey(routingKey);
        record.setPayload(writeJson(envelope));
        record.setSource(async ? EventTypes.SOURCE_NORMAL : EventTypes.SOURCE_DEGRADED);
        record.setStatus(EventTypes.STATUS_PENDING);
        record.setRetryCount(0);
        record.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(record);
        if (async) {
            Long id = record.getId();
            afterCommit(() -> deliver(id, envelope, routingKey));
        }
        return async;
    }

    /** 事务提交后投递，避免消息先于业务提交到达消费者 */
    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /** 投递单条记录，成功置 sent，失败累计重试次数留给定时任务 */
    public boolean deliver(Long outboxId, EventEnvelope envelope, String routingKey) {
        try {
            rabbitTemplate.convertAndSend(EventTypes.EXCHANGE, routingKey, envelope);
            outboxMapper.update(null, new LambdaUpdateWrapper<EventOutbox>()
                    .eq(EventOutbox::getId, outboxId)
                    .set(EventOutbox::getStatus, EventTypes.STATUS_SENT)
                    .set(EventOutbox::getSentAt, LocalDateTime.now()));
            return true;
        } catch (Exception e) {
            mqAvailability.markDown(e.getMessage());
            markFailed(outboxId, e.getMessage());
            return false;
        }
    }

    /** 重投 pending 事件，返回成功条数，供定时任务调用 */
    public int retryPending(int maxRetry) {
        List<EventOutbox> records = outboxMapper.selectList(new LambdaQueryWrapper<EventOutbox>()
                .eq(EventOutbox::getStatus, EventTypes.STATUS_PENDING)
                .lt(EventOutbox::getRetryCount, maxRetry)
                .orderByAsc(EventOutbox::getId)
                .last("limit 50"));
        int sent = 0;
        for (EventOutbox record : records) {
            if (!mqAvailability.reachable()) {
                break;
            }
            try {
                EventEnvelope envelope = objectMapper.readValue(record.getPayload(), EventEnvelope.class);
                if (deliver(record.getId(), envelope, record.getRoutingKey())) {
                    sent++;
                }
            } catch (Exception e) {
                markFailed(record.getId(), "解析失败：" + e.getMessage());
            }
        }
        return sent;
    }

    private void markFailed(Long outboxId, String error) {
        EventOutbox current = outboxMapper.selectById(outboxId);
        int retry = current == null || current.getRetryCount() == null ? 0 : current.getRetryCount() + 1;
        String message = error == null ? null : error.substring(0, Math.min(error.length(), 280));
        outboxMapper.update(null, new LambdaUpdateWrapper<EventOutbox>()
                .eq(EventOutbox::getId, outboxId)
                .set(EventOutbox::getRetryCount, retry)
                .set(EventOutbox::getLastError, message));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("事件序列化失败：" + e.getMessage(), e);
        }
    }
}
