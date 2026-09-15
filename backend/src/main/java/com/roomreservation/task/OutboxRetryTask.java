package com.roomreservation.task;

import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.common.EventTypes;
import com.roomreservation.service.EventBusService;
import com.roomreservation.service.MqAvailabilityService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
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
     * 死信队列重投：在 channel 上 basicGet 后显式确认。
     * 未达上限则重投回原交换机并确认原消息；达到上限确认丢弃，避免消息滞留反复入队。
     */
    @Scheduled(fixedDelay = 60_000L)
    public void redeliverDlq() {
        if (!mqAvailability.enabled() || !mqAvailability.reachable()) {
            return;
        }
        for (int i = 0; i < DLQ_BATCH; i++) {
            Boolean next = rabbitTemplate.execute(channel -> {
                GetResponse response = channel.basicGet(EventTypes.DLQ, false);
                if (response == null) {
                    return false;
                }
                long deliveryTag = response.getEnvelope().getDeliveryTag();
                int deaths = deathCount(response.getProps());
                if (deaths >= props.getDlqMaxRedelivery()) {
                    channel.basicAck(deliveryTag, false);
                    log.warn("死信重投已达 {} 次上限，确认丢弃消息", deaths);
                    return true;
                }
                try {
                    channel.basicPublish(EventTypes.EXCHANGE, EventTypes.BOOKING_CANCELLED,
                            response.getProps(), response.getBody());
                    channel.basicAck(deliveryTag, false);
                    redelivered.incrementAndGet();
                    return true;
                } catch (Exception e) {
                    channel.basicNack(deliveryTag, false, true);
                    mqAvailability.markDown(e.getMessage());
                    return false;
                }
            });
            if (!Boolean.TRUE.equals(next)) {
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
    private int deathCount(AMQP.BasicProperties properties) {
        if (properties == null || properties.getHeaders() == null) {
            return 0;
        }
        Object value = properties.getHeaders().get("x-death");
        if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object count = first.get("count");
            if (count instanceof Number number) {
                return number.intValue();
            }
        }
        return 0;
    }
}
