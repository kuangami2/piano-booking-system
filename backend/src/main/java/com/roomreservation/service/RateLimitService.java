package com.roomreservation.service;

import com.roomreservation.common.Constants;
import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.exception.ServiceException;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段 A 限流：固定窗口计数，Redis 优先，不可用时回退进程内计数
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    @Resource
    private EnhanceProperties props;
    @Resource
    private StringRedisTemplate redisTemplate;

    private final Map<String, long[]> local = new ConcurrentHashMap<>();
    private volatile long redisDownUntil = 0L;

    /**
     * 按用户与操作维度限流，超限抛 429 业务码
     */
    public void check(String scene, Integer userId) {
        if (!props.isRateLimitEnabled() || userId == null) {
            return;
        }
        int limit = Math.max(1, props.getRateLimitPerMinute());
        String key = "rl:" + scene + ":" + userId;
        long count = increase(key);
        if (count > limit) {
            throw new ServiceException(Constants.CODE_429, "操作过于频繁，请稍后再试");
        }
    }

    private long increase(String key) {
        if (redisTemplate != null && System.currentTimeMillis() >= redisDownUntil) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.expire(key, Duration.ofMinutes(1));
                }
                return count == null ? 1L : count;
            } catch (Exception e) {
                redisDownUntil = System.currentTimeMillis() + 30_000L;
                log.warn("Redis 不可用，限流回退进程内：{}", e.getMessage());
            }
        }
        long now = System.currentTimeMillis();
        long[] window = local.computeIfAbsent(key, k -> new long[]{now, 0L});
        synchronized (window) {
            if (now - window[0] >= 60_000L) {
                window[0] = now;
                window[1] = 0L;
            }
            window[1]++;
            return window[1];
        }
    }
}
