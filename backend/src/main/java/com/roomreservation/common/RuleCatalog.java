package com.roomreservation.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 规则参数元数据：合法区间、默认值与跨参数依赖规则。
 * 写入校验与读取钳制共用同一份定义，也供管理端复用，避免前后端各写一套范围。
 */
public final class RuleCatalog {

    /** 单个参数的元数据 */
    public record Meta(String key, int min, int max, int defaultValue, String note) {
    }

    private static final Map<String, Meta> METAS = new LinkedHashMap<>();

    private static void put(String key, int min, int max, int defaultValue, String note) {
        METAS.put(key, new Meta(key, min, max, defaultValue, note));
    }

    static {
        put(RuleKeys.BOOKING_MIN_UNIT, 5, 240, 30, "最小可约单位，分钟");
        put(RuleKeys.BOOKING_MAX_DURATION, 30, 720, 240, "单次预约最大时长，分钟");
        put(RuleKeys.BOOKING_ADVANCE_DAYS, 1, 30, 7, "提前预约天数");
        put(RuleKeys.BOOKING_WEEKLY_LIMIT, 1, 50, 3, "每周最大预约次数");
        put(RuleKeys.BOOKING_WEEKLY_CANCEL_LIMIT, 1, 50, 2, "每周最大退约次数");
        put(RuleKeys.CREDIT_INITIAL, 0, 1000, 100, "信用初始值");
        put(RuleKeys.CREDIT_MAX, 1, 1000, 100, "信用上限");
        put(RuleKeys.CREDIT_LOW, 0, 1000, 60, "信用暂停阈值，低于该值暂停预约");
        put(RuleKeys.CREDIT_DAILY_RESTORE, 0, 100, 1, "信用每日自然恢复值");
        put(RuleKeys.ACTIVITY_WEIGHT_LOGIN, 0, 100, 1, "活跃度权重，登录");
        put(RuleKeys.ACTIVITY_WEIGHT_BOOKING, 0, 100, 5, "活跃度权重，预约");
        put(RuleKeys.ACTIVITY_WEIGHT_CANCEL, 0, 100, 3, "活跃度权重，退约计负分");
        put(RuleKeys.ACTIVITY_WEIGHT_WATCH, 0, 100, 2, "活跃度权重，关注");
        put(RuleKeys.ACTIVITY_WEIGHT_MESSAGE_READ, 0, 100, 1, "活跃度权重，消息已读");
        put(RuleKeys.ACTIVITY_WEIGHT_FEEDBACK, 0, 100, 2, "活跃度权重，反馈");
        put(RuleKeys.RISK_NEAR_CANCEL_WINDOW, 1, 10080, 120, "临近取消判定窗口，分钟");
        put(RuleKeys.RISK_NEAR_CANCEL_THRESHOLD, 1, 100, 3, "临近取消达阈值次数");
        put(RuleKeys.RISK_BLACKLIST_DURATION, 1, 10080, 60, "风控黑名单时长，分钟");
    }

    private RuleCatalog() {
    }

    public static Meta of(String key) {
        return METAS.get(key);
    }

    public static Collection<Meta> all() {
        return METAS.values();
    }

    /**
     * 读取时钳制到合法区间：历史脏数据或绕过校验的写入也只会退化为边界值，不会导致崩溃。
     */
    public static int clamp(String key, int value) {
        Meta meta = METAS.get(key);
        if (meta == null) {
            return value;
        }
        if (value < meta.min()) {
            return meta.min();
        }
        return Math.min(value, meta.max());
    }

    /**
     * 保存前校验取值范围，通过返回 null，否则返回可直接展示的错误信息。
     */
    public static String validateRange(String key, int value) {
        Meta meta = METAS.get(key);
        if (meta == null) {
            return "未知参数 " + key;
        }
        if (value < meta.min() || value > meta.max()) {
            return "参数 " + key + " 应在 " + meta.min() + " 到 " + meta.max() + " 之间";
        }
        return null;
    }

    /**
     * 跨参数依赖校验，values 为本次提交后的生效值视图，含未提交参数的现值。
     * 单看每个参数都合法但组合起来会让功能不可用的组合在此拦下。
     */
    public static String validateDependencies(Map<String, Integer> values) {
        Integer maxCredit = values.get(RuleKeys.CREDIT_MAX);
        Integer low = values.get(RuleKeys.CREDIT_LOW);
        Integer initial = values.get(RuleKeys.CREDIT_INITIAL);
        if (maxCredit != null && low != null && low > maxCredit) {
            return "信用暂停阈值不能高于信用上限，否则所有用户都会被暂停预约";
        }
        if (maxCredit != null && initial != null && initial > maxCredit) {
            return "信用初始值不能高于信用上限";
        }
        Integer maxDuration = values.get(RuleKeys.BOOKING_MAX_DURATION);
        Integer minUnit = values.get(RuleKeys.BOOKING_MIN_UNIT);
        if (minUnit != null) {
            // 先挡住越界值，否则下面的取模会因 minUnit 为 0 抛算术异常
            String unitError = validateRange(RuleKeys.BOOKING_MIN_UNIT, minUnit);
            if (unitError != null) {
                return unitError;
            }
        }
        if (maxDuration != null && minUnit != null) {
            if (maxDuration < minUnit) {
                return "单次预约最大时长不能小于最小可约单位，否则无法预约";
            }
            if (maxDuration % minUnit != 0) {
                return "单次预约最大时长需为最小可约单位的整数倍";
            }
        }
        Integer window = values.get(RuleKeys.RISK_NEAR_CANCEL_WINDOW);
        Integer advanceDays = values.get(RuleKeys.BOOKING_ADVANCE_DAYS);
        if (window != null && advanceDays != null && window > advanceDays * 1440L) {
            return "临近取消窗口不能超过可提前预约的总时长";
        }
        return null;
    }
}
