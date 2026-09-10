package com.roomreservation.service;

import com.roomreservation.common.Constants;
import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.common.RiskBlockedException;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.dto.RiskStatus;
import com.roomreservation.entity.Booking;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 临近取消风控：窗口内退约计次，达阈值临时限制预约，Redis 优先，不可用时回退进程内
 */
@Service
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BLACKLIST_KEY = "rr:risk:near-cancel:blacklist";
    private static final String EXPIRE_PREFIX = "rr:risk:near-cancel:expire:";
    private static final String COUNT_PREFIX = "rr:risk:near-cancel:count:";

    @Resource
    private EnhanceProperties props;
    @Resource
    private IRuleConfigService ruleConfigService;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    private final Map<Integer, Integer> localCount = new ConcurrentHashMap<>();
    private final Map<Integer, Long> localBlacklist = new ConcurrentHashMap<>();
    private volatile long redisDownUntil = 0L;

    /**
     * 退约时判定：预约开始前窗口内退约计一次临近取消，达阈值加入临时黑名单并重置计数
     */
    public void onCancel(Booking booking) {
        if (!props.isRiskEnabled() || booking == null || booking.getUserId() == null) {
            return;
        }
        int window = ruleConfigService.getInt(RuleKeys.RISK_NEAR_CANCEL_WINDOW, 120);
        int threshold = Math.max(1, ruleConfigService.getInt(RuleKeys.RISK_NEAR_CANCEL_THRESHOLD, 3));
        int duration = Math.max(1, ruleConfigService.getInt(RuleKeys.RISK_BLACKLIST_DURATION, 60));
        LocalDateTime start = LocalDateTime.of(booking.getBookDate(),
                LocalTime.of(booking.getStartMin() / 60, booking.getStartMin() % 60));
        long minutes = Duration.between(LocalDateTime.now(), start).toMinutes();
        if (minutes < 0 || minutes > window) {
            return;
        }
        int count = increaseCount(booking.getUserId(), duration);
        if (count >= threshold) {
            addBlacklist(booking.getUserId(), duration);
            resetCount(booking.getUserId());
        }
    }

    /**
     * 查询本人风控状态
     */
    public RiskStatus status(Integer userId) {
        if (userId == null) {
            return new RiskStatus(false, null, null, 0);
        }
        int count = currentCount(userId);
        Long until = blacklistUntil(userId);
        boolean active = until != null && until > System.currentTimeMillis();
        String untilText = active ? TIME_FORMAT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(until), ZoneId.systemDefault())) : null;
        String reason = active ? "临近预约开始时段的退约次数达到上限" : null;
        return new RiskStatus(active, untilText, reason, count);
    }

    /**
     * 预约创建前检查，命中临时限制抛 409 并带解除时间
     */
    public void checkBeforeBooking(Integer userId) {
        RiskStatus status = status(userId);
        if (status.active()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("until", status.until());
            data.put("reason", status.reason());
            data.put("nearCancelCount", status.nearCancelCount());
            throw new RiskBlockedException(Constants.CODE_409,
                    "临近取消次数过多，预约临时限制至 " + status.until(), data);
        }
    }

    private int increaseCount(Integer userId, int durationMinutes) {
        if (redisAvailable()) {
            try {
                String key = COUNT_PREFIX + userId;
                Long value = redisTemplate.opsForValue().increment(key);
                redisTemplate.expire(key, Duration.ofMinutes(Math.max(durationMinutes, 1)));
                return value == null ? 1 : value.intValue();
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        return localCount.merge(userId, 1, Integer::sum);
    }

    private int currentCount(Integer userId) {
        if (redisAvailable()) {
            try {
                String value = redisTemplate.opsForValue().get(COUNT_PREFIX + userId);
                return value == null ? 0 : Integer.parseInt(value);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        return localCount.getOrDefault(userId, 0);
    }

    private void resetCount(Integer userId) {
        if (redisAvailable()) {
            try {
                redisTemplate.delete(COUNT_PREFIX + userId);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        localCount.remove(userId);
    }

    private void addBlacklist(Integer userId, int durationMinutes) {
        long until = System.currentTimeMillis() + durationMinutes * 60_000L;
        if (redisAvailable()) {
            try {
                redisTemplate.opsForSet().add(BLACKLIST_KEY, String.valueOf(userId));
                redisTemplate.opsForValue().set(EXPIRE_PREFIX + userId, String.valueOf(until),
                        Duration.ofMinutes(Math.max(durationMinutes, 1)));
                return;
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        localBlacklist.put(userId, until);
    }

    private Long blacklistUntil(Integer userId) {
        if (redisAvailable()) {
            try {
                Boolean member = redisTemplate.opsForSet().isMember(BLACKLIST_KEY, String.valueOf(userId));
                if (Boolean.TRUE.equals(member)) {
                    String value = redisTemplate.opsForValue().get(EXPIRE_PREFIX + userId);
                    if (value != null) {
                        return Long.parseLong(value);
                    }
                } else {
                    return null;
                }
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        Long until = localBlacklist.get(userId);
        if (until != null && until <= System.currentTimeMillis()) {
            localBlacklist.remove(userId);
            return null;
        }
        return until;
    }

    private boolean redisAvailable() {
        return System.currentTimeMillis() >= redisDownUntil;
    }

    private void markRedisDown(Exception e) {
        redisDownUntil = System.currentTimeMillis() + 30_000L;
        log.warn("Redis 不可用，风控回退进程内：{}", e.getMessage());
    }
}
