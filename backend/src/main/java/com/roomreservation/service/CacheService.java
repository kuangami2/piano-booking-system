package com.roomreservation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomreservation.common.EnhanceProperties;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段 A 缓存服务：Redis 优先，不可用时回退进程内缓存，整体可用开关关闭
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);
    private static final String PREFIX = "rr:";

    @Resource
    private EnhanceProperties props;
    @Resource
    private ObjectMapper objectMapper;
    @Resource(required = false)
    private StringRedisTemplate redisTemplate;

    private final Map<String, LocalEntry> local = new ConcurrentHashMap<>();
    /** Redis 故障后短时间跳过重试，避免每次请求都等超时 */
    private volatile long redisDownUntil = 0L;

    public boolean enabled() {
        return props.isCacheEnabled();
    }

    public <T> T get(String key, TypeReference<T> type) {
        if (!enabled()) {
            return null;
        }
        String json = read(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("缓存反序列化失败，忽略 key={}", key);
            evict(key);
            return null;
        }
    }

    public void put(String key, Object value) {
        put(key, value, props.getCacheTtlSeconds());
    }

    public void put(String key, Object value, long ttlSeconds) {
        if (!enabled() || value == null) {
            return;
        }
        try {
            write(key, objectMapper.writeValueAsString(value), ttlSeconds);
        } catch (Exception e) {
            log.warn("缓存写入失败，忽略 key={}", key);
        }
    }

    public void evict(String key) {
        if (redisAvailable()) {
            try {
                redisTemplate.delete(PREFIX + key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.remove(PREFIX + key);
    }

    public void evictByPrefix(String prefix) {
        String full = PREFIX + prefix;
        if (redisAvailable()) {
            try {
                Set<String> keys = redisTemplate.keys(full + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.keySet().removeIf(k -> k.startsWith(full));
    }

    private String read(String key) {
        String full = PREFIX + key;
        if (redisAvailable()) {
            try {
                String json = redisTemplate.opsForValue().get(full);
                if (json != null) {
                    return json;
                }
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        LocalEntry entry = local.get(full);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt < System.currentTimeMillis()) {
            local.remove(full);
            return null;
        }
        return entry.json;
    }

    private void write(String key, String json, long ttlSeconds) {
        String full = PREFIX + key;
        if (redisAvailable()) {
            try {
                redisTemplate.opsForValue().set(full, json, java.time.Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        local.put(full, new LocalEntry(json, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    private boolean redisAvailable() {
        return redisTemplate != null && System.currentTimeMillis() >= redisDownUntil;
    }

    private void markRedisDown(Exception e) {
        redisDownUntil = System.currentTimeMillis() + 30_000L;
        log.warn("Redis 不可用，缓存回退进程内：{}", e.getMessage());
    }

    private record LocalEntry(String json, long expireAt) {
    }
}
