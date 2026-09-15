package com.roomreservation.service;

import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 限流服务单元测试：阈值内放行、超限 429、开关关闭与 Redis 不可用回退。
 */
class RateLimitServiceTest {

    private RateLimitService service;
    private EnhanceProperties props;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new RateLimitService();
        props = new EnhanceProperties();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ReflectionTestUtils.setField(service, "props", props);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    @DisplayName("窗口内首次计数并设置一分钟过期")
    void firstHitSetsOneMinuteExpire() {
        when(valueOperations.increment("rl:booking:1")).thenReturn(1L);

        service.check("booking", 1);

        verify(redisTemplate).expire("rl:booking:1", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("超过阈值抛 429 业务码")
    void rejectsOverLimit() {
        props.setRateLimitPerMinute(2);
        when(valueOperations.increment(anyString())).thenReturn(3L);

        assertThatThrownBy(() -> service.check("booking", 1))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "429");
    }

    @Test
    @DisplayName("限流开关关闭时直接放行")
    void skipsWhenDisabled() {
        props.setRateLimitEnabled(false);

        service.check("booking", 1);
        service.checkIp("login", "172.25.240.1");
    }

    @Test
    @DisplayName("用户为空时不做用户维度限流")
    void skipsNullUser() {
        service.check("booking", null);
    }

    @Test
    @DisplayName("Redis 不可用时回退进程内计数并继续限流")
    void fallsBackToLocalCounterWhenRedisFails() {
        props.setRateLimitPerMinute(1);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("connection refused"));

        service.check("booking", 1);

        assertThatThrownBy(() -> service.check("booking", 1))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "429");
    }

    @Test
    @DisplayName("未登录接口按来源 IP 限流")
    void limitsBySourceIp() {
        props.setAuthRateLimitPerMinute(1);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        assertThatThrownBy(() -> service.checkIp("login", "172.25.240.1"))
                .isInstanceOf(ServiceException.class)
                .hasFieldOrPropertyWithValue("code", "429");
    }
}
