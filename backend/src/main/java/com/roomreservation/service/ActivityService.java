package com.roomreservation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roomreservation.common.EnhanceProperties;
import com.roomreservation.common.RuleKeys;
import com.roomreservation.dto.ActivityRankItem;
import com.roomreservation.entity.SysUser;
import com.roomreservation.mapper.SysUserMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 活跃度计分与排行榜：Redis ZSET 优先，不可用时回退进程内计分，仅单实例演示可用
 */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);
    private static final String WEEK_PREFIX = "rr:rank:activity:week:";
    private static final String MONTH_PREFIX = "rr:rank:activity:month:";

    @Resource
    private EnhanceProperties props;
    @Resource
    private IRuleConfigService ruleConfigService;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource(name = "stringRedisTemplate")
    private StringRedisTemplate redisTemplate;

    /** 降级：period 键到 用户ID 到分值 */
    private final Map<String, Map<Integer, Double>> localScores = new ConcurrentHashMap<>();
    private volatile long lastUpdateAt = 0L;
    private volatile long redisDownUntil = 0L;

    /**
     * 记录一次行为事件，按权重增减分值，正负由权重决定
     */
    public void record(Integer userId, String eventType) {
        if (!props.isActivityEnabled() || userId == null || eventType == null) {
            return;
        }
        double weight = weight(eventType);
        if (weight == 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        String weekKey = WEEK_PREFIX + weekTag(today);
        String monthKey = MONTH_PREFIX + monthTag(today);
        boolean ok = writeRedis(weekKey, userId, weight) && writeRedis(monthKey, userId, weight);
        if (!ok) {
            localAdd(weekKey, userId, weight);
            localAdd(monthKey, userId, weight);
        }
        lastUpdateAt = System.currentTimeMillis();
    }

    /**
     * 排行榜查询，返回 list、me 与 updatedAt
     */
    public Map<String, Object> ranking(String period, int page, int size, Integer currentUserId) {
        boolean month = "month".equalsIgnoreCase(period);
        String key = (month ? MONTH_PREFIX : WEEK_PREFIX) + (month ? monthTag(LocalDate.now()) : weekTag(LocalDate.now()));
        int safePage = Math.max(1, page);
        int safeSize = Math.min(50, Math.max(1, size));
        long start = (long) (safePage - 1) * safeSize;
        long end = start + safeSize - 1;

        List<ActivityRankItem> list = new ArrayList<>();
        Map<String, Object> me = null;
        boolean fromRedis = false;
        if (redisAvailable()) {
            try {
                Set<ZSetOperations.TypedTuple<String>> tuples =
                        redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
                int rank = (int) start + 1;
                if (tuples != null) {
                    for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                        Integer uid = Integer.valueOf(tuple.getValue());
                        list.add(new ActivityRankItem(rank++, uid, null, tuple.getScore() == null ? 0D : tuple.getScore()));
                    }
                }
                fromRedis = true;
                if (currentUserId != null) {
                    Long rank = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(currentUserId));
                    Double score = redisTemplate.opsForZSet().score(key, String.valueOf(currentUserId));
                    me = meMap(rank == null ? 0 : rank + 1, score == null ? 0D : score);
                }
            } catch (Exception e) {
                markRedisDown(e);
                fromRedis = false;
            }
        }
        if (!fromRedis) {
            List<Map.Entry<Integer, Double>> ranked = localRanked(key);
            int rank = 1;
            for (Map.Entry<Integer, Double> entry : ranked) {
                if (rank > end) {
                    break;
                }
                if (rank >= start + 1) {
                    list.add(new ActivityRankItem(rank, entry.getKey(), null, entry.getValue()));
                }
                rank++;
            }
            if (currentUserId != null) {
                int myRank = 0;
                double myScore = 0D;
                int r = 1;
                for (Map.Entry<Integer, Double> entry : ranked) {
                    if (entry.getKey().equals(currentUserId)) {
                        myRank = r;
                        myScore = entry.getValue();
                        break;
                    }
                    r++;
                }
                me = meMap(myRank, myScore);
            }
        }
        fillNames(list);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("me", me);
        data.put("updatedAt", lastUpdateAt == 0 ? null : java.time.format.DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss").format(java.time.LocalDateTime
                        .ofInstant(java.time.Instant.ofEpochMilli(lastUpdateAt), java.time.ZoneId.systemDefault())));
        return data;
    }

    private Map<String, Object> meMap(int rank, double score) {
        Map<String, Object> me = new HashMap<>();
        me.put("rank", rank);
        me.put("score", score);
        return me;
    }

    private void fillNames(List<ActivityRankItem> list) {
        if (list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(ActivityRankItem::userId).distinct().collect(Collectors.toList());
        Map<Integer, String> names = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, ids))
                .stream().collect(Collectors.toMap(SysUser::getId, u -> mask(u.getName())));
        for (int i = 0; i < list.size(); i++) {
            ActivityRankItem item = list.get(i);
            list.set(i, new ActivityRankItem(item.rank(), item.userId(), names.getOrDefault(item.userId(), "匿名用户"), item.score()));
        }
    }

    /**
     * 隐私脱敏：保留首字，其余以星号代替
     */
    private String mask(String name) {
        if (name == null || name.isBlank()) {
            return "匿名用户";
        }
        String trimmed = name.trim();
        if (trimmed.length() == 1) {
            return trimmed + "*";
        }
        return trimmed.charAt(0) + "*".repeat(Math.max(1, trimmed.length() - 1));
    }

    private double weight(String eventType) {
        return switch (eventType) {
            case "login" -> ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_LOGIN, 1);
            case "booking" -> ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_BOOKING, 5);
            case "cancel" -> -ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_CANCEL, 3);
            case "watch" -> ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_WATCH, 2);
            case "messageRead" -> ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_MESSAGE_READ, 1);
            case "feedback" -> ruleConfigService.getInt(RuleKeys.ACTIVITY_WEIGHT_FEEDBACK, 2);
            default -> 0;
        };
    }

    private boolean writeRedis(String key, Integer userId, double weight) {
        if (!redisAvailable()) {
            return false;
        }
        try {
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(userId), weight);
            redisTemplate.expire(key, Duration.ofDays(key.startsWith(WEEK_PREFIX) ? 14 : 62));
            return true;
        } catch (Exception e) {
            markRedisDown(e);
            return false;
        }
    }

    private void localAdd(String key, Integer userId, double weight) {
        localScores.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .merge(userId, weight, Double::sum);
    }

    private List<Map.Entry<Integer, Double>> localRanked(String key) {
        Map<Integer, Double> scores = localScores.getOrDefault(key, Map.of());
        return scores.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<Integer, Double> e) -> e.getValue()).reversed()
                        .thenComparingInt(Map.Entry::getKey))
                .collect(Collectors.toList());
    }

    private boolean redisAvailable() {
        return System.currentTimeMillis() >= redisDownUntil;
    }

    private void markRedisDown(Exception e) {
        redisDownUntil = System.currentTimeMillis() + 30_000L;
        log.warn("Redis 不可用，活跃度计分回退进程内：{}", e.getMessage());
    }

    private String weekTag(LocalDate date) {
        WeekFields wf = WeekFields.ISO;
        return date.get(wf.weekBasedYear()) + "-W" + String.format("%02d", date.get(wf.weekOfWeekBasedYear()));
    }

    private String monthTag(LocalDate date) {
        return DateTimeFormatter.ofPattern("yyyy-MM").format(date);
    }
}
