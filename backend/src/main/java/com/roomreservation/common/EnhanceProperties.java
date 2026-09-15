package com.roomreservation.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阶段 A 增强开关，关闭或依赖不可用时回退原路径
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.enhance")
/**
 * 增强功能开关：缓存、限流、活跃度与风控开关及默认阈值。
 */
public class EnhanceProperties {

    /** 缓存总开关 */
    private boolean cacheEnabled = true;

    /** 限流总开关 */
    private boolean rateLimitEnabled = true;

    /** 默认缓存有效期，秒 */
    private long cacheTtlSeconds = 60;

    /** 单用户每分钟操作次数上限 */
    private int rateLimitPerMinute = 10;

    /** 未登录接口按来源 IP 每分钟次数上限 */
    private int authRateLimitPerMinute = 20;

    /** 活跃度计分与排行榜开关 */
    private boolean activityEnabled = true;

    /** 临近取消风控开关 */
    private boolean riskEnabled = true;

    /** 阶段 C：MQ 事件总线开关，关闭或 broker 不可达时退约提醒走同步降级 */
    private boolean mqEnabled = true;

    /** 事件发布超时，毫秒 */
    private long mqPublishTimeoutMs = 500;

    /** 本地消息表重投间隔，秒 */
    private int outboxRetrySeconds = 30;

    /** 本地消息表最大投递重试次数 */
    private int outboxMaxRetry = 10;

    /** 死信队列最大重投次数，与本地消息表重试各自计数 */
    private int dlqMaxRedelivery = 5;
}
