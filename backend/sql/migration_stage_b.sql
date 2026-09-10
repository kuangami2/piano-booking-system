-- 阶段 B 规则参数补种，对已建库执行一次，已存在则仅更新说明
INSERT INTO rule_config (rule_key, rule_value, note) VALUES
('activity.weight.login', '1', '活跃度权重，登录'),
('activity.weight.booking', '5', '活跃度权重，预约'),
('activity.weight.cancel', '3', '活跃度权重，退约计负分'),
('activity.weight.watch', '2', '活跃度权重，关注'),
('activity.weight.messageRead', '1', '活跃度权重，消息已读'),
('activity.weight.feedback', '2', '活跃度权重，反馈'),
('risk.nearCancelWindowMin', '120', '临近取消判定窗口，分钟'),
('risk.nearCancelThreshold', '3', '临近取消达阈值次数'),
('risk.blacklistDurationMin', '60', '风控黑名单时长，分钟')
ON DUPLICATE KEY UPDATE note = VALUES(note);
