package com.roomreservation.service;

import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.common.RiskBlockedException;
import com.roomreservation.dto.RiskStatus;
import com.roomreservation.entity.Booking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 阶段 B 风控服务单元测试：窗口内计次、达阈值拉黑、受限拦截、窗口外与开关关闭不计次、Redis 降级。
 */
@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock private IRuleConfigService ruleConfigService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private RiskService service;

    private EnhanceProperties props;

    @BeforeEach
    void setUp() {
        props = new EnhanceProperties();
        ReflectionTestUtils.setField(service, "props", props);
        lenient().when(ruleConfigService.getInt(anyString(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    /** 预约开始时间距当前 30 分钟，落在默认 120 分钟窗口内 */
    private Booking nearBooking() {
        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        Booking booking = new Booking();
        booking.setUserId(1);
        booking.setBookDate(start.toLocalDate());
        booking.setStartMin(start.getHour() * 60 + start.getMinute());
        return booking;
    }

    /** 预约在明天，超出窗口 */
    private Booking farBooking() {
        Booking booking = new Booking();
        booking.setUserId(1);
        booking.setBookDate(LocalDateTime.now().plusDays(2).toLocalDate());
        booking.setStartMin(600);
        return booking;
    }

    @Test
    @DisplayName("窗口内连续退约达阈值后进入临时黑名单")
    void blocksAfterThreshold() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        for (int i = 0; i < 3; i++) {
            service.onCancel(nearBooking());
        }

        RiskStatus status = service.status(1);
        assertThat(status.active()).isTrue();
        assertThat(status.nearCancelCount()).isEqualTo(3);
        assertThat(status.until()).isNotBlank();
        assertThat(status.reason()).isNotBlank();
    }

    @Test
    @DisplayName("受限期间创建预约抛 409 并携带解除时间与次数")
    void blocksBookingWhenRestricted() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        for (int i = 0; i < 3; i++) {
            service.onCancel(nearBooking());
        }

        assertThatThrownBy(() -> service.checkBeforeBooking(1))
                .isInstanceOf(RiskBlockedException.class)
                .hasFieldOrPropertyWithValue("code", "409")
                .satisfies(e -> assertThat(((RiskBlockedException) e).getData()).asString().contains("until"));
    }

    @Test
    @DisplayName("超出窗口的退约不计临近取消")
    void ignoresCancelOutsideWindow() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));

        service.onCancel(farBooking());

        assertThat(service.status(1).nearCancelCount()).isZero();
        assertThat(service.status(1).active()).isFalse();
    }

    @Test
    @DisplayName("风控开关关闭时不计次")
    void skipsWhenDisabled() {
        props.setRiskEnabled(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        service.onCancel(nearBooking());

        assertThat(service.status(1).active()).isFalse();
    }

    @Test
    @DisplayName("无用户标识时状态查询返回未受限")
    void statusWithoutUserIsInactive() {
        RiskStatus status = service.status(null);

        assertThat(status.active()).isFalse();
        assertThat(status.nearCancelCount()).isZero();
        assertThat(status.until()).isNull();
    }

    @Test
    @DisplayName("Redis 可用时计数写 Redis 并设置过期")
    void countsInRedisWhenAvailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rr:risk:near-cancel:count:1")).thenReturn(1L);

        service.onCancel(nearBooking());

        verify(valueOperations).increment("rr:risk:near-cancel:count:1");
        verify(redisTemplate).expire(eq("rr:risk:near-cancel:count:1"), any(Duration.class));
    }

    @Test
    @DisplayName("Redis 异常时回退进程内计数")
    void fallsBackToLocalCountWhenRedisFails() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("connection refused"));

        service.onCancel(nearBooking());
        service.onCancel(nearBooking());

        assertThat(service.status(1).nearCancelCount()).isEqualTo(2);
    }
}
