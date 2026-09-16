-- 阶段 C：事件总线本地消息表与消费幂等表
-- 幂等，可重复执行；重建库时 schema.sql 已含本脚本内容，无需再跑。

CREATE TABLE IF NOT EXISTS event_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID，全局唯一',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型，如 booking.cancelled',
    routing_key VARCHAR(64) NOT NULL COMMENT '路由键',
    payload TEXT NOT NULL COMMENT '事件信封 JSON',
    source VARCHAR(16) NOT NULL DEFAULT 'normal' COMMENT '来源，normal 或 degraded',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态，pending 或 sent',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '投递重试次数',
    last_error VARCHAR(300) COMMENT '最近一次失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    sent_at DATETIME COMMENT '投递成功时间',
    UNIQUE KEY uk_outbox_event (event_id),
    KEY idx_outbox_status (status, created_at)
) COMMENT='本地消息表，事件与业务同事务落库';

CREATE TABLE IF NOT EXISTS processed_event (
    event_id VARCHAR(64) PRIMARY KEY COMMENT '事件ID，幂等键',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型',
    handled_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '处理时间'
) COMMENT='消费幂等表，重复投递直接丢弃';
