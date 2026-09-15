package com.roomreservation.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.entity.EventOutbox;
import com.roomreservation.event.BookingCancelledPayload;
import com.roomreservation.event.EventEnvelope;
import com.roomreservation.mapper.EventOutboxMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 事件总线单元测试：本地消息表落库、降级标记、投递成功置 sent 与失败计数。
 */
@ExtendWith(MockitoExtension.class)
class EventBusServiceTest {

    @Mock private EventOutboxMapper eventOutboxMapper;
    @Mock private MqAvailabilityService mqAvailability;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private EventBusService service;

    /**
     * 纯单元测试没有 Spring 上下文，LambdaUpdateWrapper 的 set 需要实体元数据。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, EventOutbox.class);
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    private BookingCancelledPayload payload() {
        return new BookingCancelledPayload(5, 1, 10, "B101", "2026-09-16", 600, 690);
    }

    private EventEnvelope envelope() {
        return new EventEnvelope("e1", "booking.cancelled", "2026-09-15 23:00:00", 1, payload());
    }

    @Test
    @DisplayName("MQ 可用时按 normal 落本地消息表并受理异步投递")
    void writesOutboxAsNormalWhenReachable() {
        when(mqAvailability.reachable()).thenReturn(true);

        boolean async = service.publish("booking.cancelled", "booking.cancelled", payload());

        assertThat(async).isTrue();
        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxMapper).insert(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("normal");
        assertThat(captor.getValue().getStatus()).isEqualTo("pending");
        assertThat(captor.getValue().getEventId()).isNotBlank();
    }

    @Test
    @DisplayName("MQ 不可用时按 degraded 落表并交由调用方同步降级")
    void writesOutboxAsDegradedWhenUnavailable() {
        when(mqAvailability.reachable()).thenReturn(false);

        boolean async = service.publish("booking.cancelled", "booking.cancelled", payload());

        assertThat(async).isFalse();
        ArgumentCaptor<EventOutbox> captor = ArgumentCaptor.forClass(EventOutbox.class);
        verify(eventOutboxMapper).insert(captor.capture());
        assertThat(captor.getValue().getSource()).isEqualTo("degraded");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("投递成功把记录置为 sent")
    void marksSentAfterDelivery() {
        EventEnvelope envelope = envelope();

        boolean ok = service.deliver(9L, envelope, "booking.cancelled");

        assertThat(ok).isTrue();
        verify(rabbitTemplate).convertAndSend("room.events", "booking.cancelled", envelope);
        verify(eventOutboxMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("投递失败标记 broker 不可达并累计重试次数")
    void marksDownAndCountsRetryOnFailure() {
        EventOutbox record = new EventOutbox();
        record.setId(9L);
        record.setRetryCount(0);
        when(eventOutboxMapper.selectById(9L)).thenReturn(record);
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(eq("room.events"), eq("booking.cancelled"), any(Object.class));

        boolean ok = service.deliver(9L, envelope(), "booking.cancelled");

        assertThat(ok).isFalse();
        verify(mqAvailability).markDown(anyString());
        verify(eventOutboxMapper).update(isNull(), any());
    }
}
