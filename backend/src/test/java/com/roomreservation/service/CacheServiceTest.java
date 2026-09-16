package com.roomreservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.common.EnhanceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 阶段 A 缓存服务单元测试：开关穿透、TTL 写入、反序列化兜底、Redis 降级进程内、前缀失效。
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks private CacheService service;

    private EnhanceProperties props;

    @BeforeEach
    void setUp() {
        props = new EnhanceProperties();
        ReflectionTestUtils.setField(service, "props", props);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
    }

    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    @Test
    @DisplayName("开关关闭时读写穿透，不访问 Redis")
    void bypassesWhenDisabled() {
        props.setCacheEnabled(false);

        assertThat(service.get("rooms", LIST_TYPE)).isNull();
        service.put("rooms", List.of("B101"));

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Redis 可用时写入带 TTL 的键")
    void writesToRedisWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.put("rooms", List.of("B101"), 30);

        verify(valueOperations).set(eq("rr:rooms"), anyString(), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("命中缓存时反序列化为业务对象")
    void readsFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rr:rooms")).thenReturn("[\"B101\"]");

        assertThat(service.get("rooms", LIST_TYPE)).containsExactly("B101");
    }

    @Test
    @DisplayName("反序列化失败时删除坏键并返回 null")
    void evictsBrokenValue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rr:bad")).thenReturn("{not-json");

        assertThat(service.get("bad", new TypeReference<Map<String, Object>>() {
        })).isNull();

        verify(redisTemplate).delete("rr:bad");
    }

    @Test
    @DisplayName("Redis 异常时回退进程内缓存，后续仍可命中")
    void fallsBackToLocalCache() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("connection refused"));

        service.put("rooms", List.of("B101"), 60);

        assertThat(service.get("rooms", LIST_TYPE)).containsExactly("B101");
    }

    @Test
    @DisplayName("击穿保护：未命中回源一次，之后走缓存")
    void loadsOnceThenServesFromCache() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rr:k")).thenReturn(null, null, "\"loaded\"");
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> loader = () -> {
            calls.incrementAndGet();
            return "loaded";
        };

        assertThat(service.getOrLoad("k", new TypeReference<String>() {
        }, loader)).isEqualTo("loaded");
        assertThat(service.getOrLoad("k", new TypeReference<String>() {
        }, loader)).isEqualTo("loaded");

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("回源返回空值时不写缓存")
    void skipsCacheWhenLoaderReturnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rr:k")).thenReturn(null);

        assertThat(service.getOrLoad("k", new TypeReference<String>() {
        }, () -> null)).isNull();

        verify(valueOperations, org.mockito.Mockito.never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("按前缀失效：SCAN 收集键后批量删除")
    void evictsByPrefix() {
        Cursor<String> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, false);
        when(cursor.next()).thenReturn("rr:room:1");
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);

        service.evictByPrefix("room:");

        verify(redisTemplate).delete(anyCollection());
    }
}
