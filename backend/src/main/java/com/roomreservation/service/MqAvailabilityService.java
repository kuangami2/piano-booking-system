package com.roomreservation.service;

import com.roomreservation.common.EnhanceProperties;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.stereotype.Service;

/**
 * MQ 可用性探测：总开关关闭或 broker 不可达时，事件走同步降级。
 * 正常时缓存 10 秒、故障后静默 30 秒，避免每次发布都等连接超时。
 */
@Service
public class MqAvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(MqAvailabilityService.class);
    private static final long UP_CACHE_MS = 10_000L;
    private static final long DOWN_SILENCE_MS = 30_000L;

    @Resource
    private EnhanceProperties props;
    @Resource
    private ConnectionFactory connectionFactory;

    private volatile long upUntil = 0L;
    private volatile long downUntil = 0L;
    private volatile boolean lastKnownUp = false;

    /** 总开关，关闭后不做任何探测 */
    public boolean enabled() {
        return props.isMqEnabled();
    }

    /** 探测 broker 是否可达 */
    public boolean reachable() {
        if (!enabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now < downUntil) {
            return false;
        }
        if (now < upUntil) {
            return true;
        }
        try (Connection connection = connectionFactory.createConnection()) {
            lastKnownUp = connection.isOpen();
            if (lastKnownUp) {
                upUntil = now + UP_CACHE_MS;
            }
            return lastKnownUp;
        } catch (Exception e) {
            markDown(e.getMessage());
            return false;
        }
    }

    /** 投递或探测失败后调用，短时间内不再重试 */
    public void markDown(String reason) {
        downUntil = System.currentTimeMillis() + DOWN_SILENCE_MS;
        upUntil = 0L;
        lastKnownUp = false;
        log.warn("RabbitMQ 不可达，事件转同步降级：{}", reason);
    }

    /** 最近一次探测结果，供概览接口展示 */
    public boolean lastKnownUp() {
        return lastKnownUp;
    }
}
