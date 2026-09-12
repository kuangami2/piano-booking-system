-- 功能迭代迁移：密码重置令牌表，配合邮箱找回
CREATE TABLE IF NOT EXISTS password_reset (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '重置记录ID',
    user_id INT NOT NULL COMMENT '用户ID',
    token VARCHAR(64) NOT NULL COMMENT '一次性重置令牌',
    expire_at DATETIME NOT NULL COMMENT '过期时间',
    used TINYINT(1) DEFAULT 0 COMMENT '是否已使用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    UNIQUE KEY uk_reset_token (token),
    INDEX idx_reset_user (user_id)
) COMMENT='密码重置令牌表';
