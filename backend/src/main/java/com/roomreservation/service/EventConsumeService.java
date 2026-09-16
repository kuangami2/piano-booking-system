package com.roomreservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.entity.ProcessedEvent;
import com.roomreservation.event.BookingCancelledPayload;
import com.roomreservation.event.EventEnvelope;
import com.roomreservation.mapper.ProcessedEventMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 事件消费：以 processed_event 的 eventId 主键幂等去重，重投不会产生重复提醒。
 */
@Service
public class EventConsumeService {

    private static final Logger log = LoggerFactory.getLogger(EventConsumeService.class);

    @Resource
    private ProcessedEventMapper processedEventMapper;
    @Resource
    private VacancyNotifyService vacancyNotifyService;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 处理退约事件，返回 false 表示此前已处理过，直接确认即可。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean handleBookingCancelled(EventEnvelope envelope) {
        if (envelope == null || envelope.getEventId() == null) {
            return false;
        }
        if (processedEventMapper.selectById(envelope.getEventId()) != null) {
            return false;
        }
        BookingCancelledPayload payload = objectMapper.convertValue(
                envelope.getPayload(), BookingCancelledPayload.class);
        int notified = vacancyNotifyService.notifyWatchers(payload);
        ProcessedEvent record = new ProcessedEvent();
        record.setEventId(envelope.getEventId());
        record.setEventType(envelope.getEventType());
        record.setHandledAt(LocalDateTime.now());
        try {
            processedEventMapper.insert(record);
        } catch (DuplicateKeyException e) {
            // 并发重复投递，另一消费者已处理
            return false;
        }
        log.info("退约事件处理完成，提醒 {} 人，eventId={}", notified, envelope.getEventId());
        return true;
    }
}
