package com.roomreservation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.entity.ProcessedEvent;
import com.roomreservation.event.BookingCancelledPayload;
import com.roomreservation.event.EventEnvelope;
import com.roomreservation.mapper.ProcessedEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 事件消费单元测试：首次处理写幂等记录，重复投递不再产生提醒。
 */
@ExtendWith(MockitoExtension.class)
class EventConsumeServiceTest {

    @Mock private ProcessedEventMapper processedEventMapper;
    @Mock private VacancyNotifyService vacancyNotifyService;

    @InjectMocks private EventConsumeService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    private EventEnvelope envelope() {
        BookingCancelledPayload payload = new BookingCancelledPayload(5, 1, 10, "B101", "2026-09-16", 600, 690);
        return new EventEnvelope("e1", "booking.cancelled", "2026-09-15 23:00:00", 1, payload);
    }

    @Test
    @DisplayName("首次处理写幂等记录并触发提醒")
    void handlesFirstTime() {
        when(processedEventMapper.selectById("e1")).thenReturn(null);

        boolean handled = service.handleBookingCancelled(envelope());

        assertThat(handled).isTrue();
        verify(vacancyNotifyService).notifyWatchers(any(BookingCancelledPayload.class));
        verify(vacancyNotifyService).notifyCancelled(any(BookingCancelledPayload.class), eq("self"));
        verify(processedEventMapper).insert(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("重复投递直接丢弃，不再重复提醒")
    void skipsAlreadyProcessed() {
        ProcessedEvent record = new ProcessedEvent();
        record.setEventId("e1");
        when(processedEventMapper.selectById("e1")).thenReturn(record);

        boolean handled = service.handleBookingCancelled(envelope());

        assertThat(handled).isFalse();
        verify(vacancyNotifyService, never()).notifyWatchers(any(BookingCancelledPayload.class));
        verify(processedEventMapper, never()).insert(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("并发重复投递时主键冲突按已处理返回")
    void treatsDuplicateKeyAsProcessed() {
        when(processedEventMapper.selectById("e1")).thenReturn(null);
        when(processedEventMapper.insert(any(ProcessedEvent.class))).thenThrow(new DuplicateKeyException("pk"));

        boolean handled = service.handleBookingCancelled(envelope());

        assertThat(handled).isFalse();
    }

    @Test
    @DisplayName("缺少 eventId 的载荷直接忽略")
    void ignoresInvalidEnvelope() {
        assertThat(service.handleBookingCancelled(null)).isFalse();
        assertThat(service.handleBookingCancelled(new EventEnvelope())).isFalse();
    }
}
