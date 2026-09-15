package com.roomreservation.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.roomreservation.common.EventTypes;
import com.roomreservation.entity.EventOutbox;
import com.roomreservation.mapper.EventOutboxMapper;
import com.roomreservation.service.MqAvailabilityService;
import com.roomreservation.task.OutboxRetryTask;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端事件概览：运行模式、本地消息表计数、死信深度与事件计数。
 * broker 不可达时 outbox 与 dlq 返回 null 而不报错，页面只提示降级运行。
 */
@Service
public class AdminEventService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private EventOutboxMapper eventOutboxMapper;
    @Resource
    private MqAvailabilityService mqAvailability;
    @Resource
    private OutboxRetryTask outboxRetryTask;
    @Resource
    private RabbitTemplate rabbitTemplate;

    public Map<String, Object> summary() {
        boolean enabled = mqAvailability.enabled();
        boolean reachable = enabled && mqAvailability.reachable();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("mqEnabled", enabled);
        data.put("mode", reachable ? "async" : "degraded");
        data.put("brokerReachable", reachable);
        if (reachable) {
            Map<String, Object> outbox = new LinkedHashMap<>();
            outbox.put("pending", countByStatus(EventTypes.STATUS_PENDING));
            outbox.put("sent", countByStatus(EventTypes.STATUS_SENT));
            data.put("outbox", outbox);
            Map<String, Object> dlq = new LinkedHashMap<>();
            dlq.put("depth", queueDepth());
            dlq.put("redelivered", outboxRetryTask.redeliveredCount());
            data.put("dlq", dlq);
        } else {
            data.put("outbox", null);
            data.put("dlq", null);
        }
        data.put("events", eventCounts());
        data.put("lastEventAt", lastEventAt());
        return data;
    }

    private long countByStatus(String status) {
        Long count = eventOutboxMapper.selectCount(new LambdaQueryWrapper<EventOutbox>()
                .eq(EventOutbox::getStatus, status));
        return count == null ? 0L : count;
    }

    /** 各事件类型累计计数，全部事件含降级记录 */
    private List<Map<String, Object>> eventCounts() {
        List<Map<String, Object>> rows = eventOutboxMapper.selectMaps(new QueryWrapper<EventOutbox>()
                .select("event_type as eventType", "count(*) as cnt")
                .groupBy("event_type"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("eventType", row.get("eventType"));
            item.put("count", toLong(row.get("cnt")));
            result.add(item);
        }
        return result;
    }

    private Object lastEventAt() {
        EventOutbox last = eventOutboxMapper.selectOne(new LambdaQueryWrapper<EventOutbox>()
                .orderByDesc(EventOutbox::getId)
                .last("limit 1"));
        if (last == null || last.getCreatedAt() == null) {
            return null;
        }
        return last.getCreatedAt().format(FORMATTER);
    }

    /** 死信队列深度，broker 不可用时由调用方以 null 呈现 */
    private Long queueDepth() {
        try {
            return rabbitTemplate.execute(channel -> (long) channel.messageCount(EventTypes.DLQ));
        } catch (Exception e) {
            return null;
        }
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
