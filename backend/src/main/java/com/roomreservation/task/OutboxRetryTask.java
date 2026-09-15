package com.roomreservation.task;

import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.common.EventTypes;
import com.roomreservation.service.EventBusService;
import com.roomreservation.service.MqAvailabilityService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 投递补偿：重投本地消息表的 pending 记录，并把死信队列的消息按次数上限重投。
 */
@Component
public class OutboxRetryTask {

    private static final Logger log = LoggerFactory.getLogger(OutboxRetryTask.class);
    private static final int DLQ_BATCH = 20;

    /** 本轮进程内累计的死者重投次数，供概览接口展示 */
    private final AtomicLong redelivered = new AtomicLong();

    @Resource
    private EventBusService eventBusService;
    @Resource
    private MqAvailabilityService mqAvailability;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private EnhanceProperties props;

    /**
     * 重投待投递事件，间隔取 app.enhance.outbox-retry-seconds，默认 30 秒。
     */
    @Scheduled(fixedDelayString = "#{${app.enhance.outbox-retry-seconds:30} * 1000}")
    public void retryOutbox() {
        if (!mqAvailability.enabled() || !mqAvailability.reachable()) {
            return;
        }
        int sent = eventBusService.retryPending(props.getOutboxMaxRetry());
        if (sent > 0) {
            log.info("本地消息表重投成功 {} 条", sent);
        }
    }

    /**
     * 死信队列重投，超过 dlq-max-redelivery 次则丢弃，与 outbox 重试各自计数。
     */
    @Scheduled(fixedDelay = 60_000L)
    public void redeliverDlq() {
        if (!mqAvailability.enabled() || !mqAvailability.reachable()) {
            return;
        }
        for (int i = 0; i < DLQ_BATCH; i++) {
            Message message = rabbitTemplate.receive(EventTypes.DLQ);
            if (message == null) {
                return;
            }
            int deaths = deathCount(message);
            if (deaths >= props.getDlqMaxRedelivery()) {
                log.warn("死信重投已达 {} 次上限，丢弃消息", deaths);
                continue;
            }
            try {
                rabbitTemplate.send(EventTypes.EXCHANGE, EventTypes.BOOKING_CANCELLED, message);
                redelivered.incrementAndGet();
            } catch (Exception e) {
                mqAvailability.markDown(e.getMessage());
                return;
            }
        }
    }

    public long redeliveredCount() {
        return redelivered.get();
    }

    /**
     * 读取 x-death 表头里的重投次数，值为 list 且首项含 count。
     */
    private int deathCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get("x-death");
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object count = first.get("count");
            if (count instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }
}
