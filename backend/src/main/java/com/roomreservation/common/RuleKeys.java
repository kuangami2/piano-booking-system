package com.roomreservation.common;

/**
 * 规则参数键，与 rule_config 表对应
 */
public interface RuleKeys {

    /** 最小可约单位，分钟 */
    String BOOKING_MIN_UNIT = "booking.minUnit";

    /** 单次预约最大时长，分钟 */
    String BOOKING_MAX_DURATION = "booking.maxDurationMin";

    /** 提前预约天数 */
    String BOOKING_ADVANCE_DAYS = "booking.advanceDays";

    /** 每周最大预约次数 */
    String BOOKING_WEEKLY_LIMIT = "booking.weeklyBookLimit";

    /** 每周最大退约次数 */
    String BOOKING_WEEKLY_CANCEL_LIMIT = "booking.weeklyCancelLimit";

    /** 信用初始值 */
    String CREDIT_INITIAL = "credit.initialValue";

    /** 信用上限 */
    String CREDIT_MAX = "credit.maxValue";

    /** 信用暂停阈值，低于该值暂停预约 */
    String CREDIT_LOW = "credit.lowThreshold";

    /** 信用每日自然恢复值 */
    String CREDIT_DAILY_RESTORE = "credit.dailyRestore";
}
