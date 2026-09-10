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

    /** 活跃度权重：登录 */
    String ACTIVITY_WEIGHT_LOGIN = "activity.weight.login";

    /** 活跃度权重：预约 */
    String ACTIVITY_WEIGHT_BOOKING = "activity.weight.booking";

    /** 活跃度权重：退约，计负分 */
    String ACTIVITY_WEIGHT_CANCEL = "activity.weight.cancel";

    /** 活跃度权重：关注 */
    String ACTIVITY_WEIGHT_WATCH = "activity.weight.watch";

    /** 活跃度权重：消息已读 */
    String ACTIVITY_WEIGHT_MESSAGE_READ = "activity.weight.messageRead";

    /** 活跃度权重：反馈 */
    String ACTIVITY_WEIGHT_FEEDBACK = "activity.weight.feedback";

    /** 临近取消判定窗口，分钟 */
    String RISK_NEAR_CANCEL_WINDOW = "risk.nearCancelWindowMin";

    /** 临近取消达阈值次数 */
    String RISK_NEAR_CANCEL_THRESHOLD = "risk.nearCancelThreshold";

    /** 风控黑名单时长，分钟 */
    String RISK_BLACKLIST_DURATION = "risk.blacklistDurationMin";
}
