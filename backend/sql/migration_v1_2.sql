-- 功能迭代迁移：注册唯一性，学号与邮箱补唯一索引兜底
-- 执行前先确认无重复值：
-- SELECT student_no FROM sys_user GROUP BY student_no HAVING COUNT(*) > 1;
-- SELECT email FROM sys_user GROUP BY email HAVING COUNT(*) > 1;
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_student_no (student_no) COMMENT '学号唯一，防一人多号';
ALTER TABLE sys_user ADD UNIQUE KEY uk_user_email (email) COMMENT '邮箱唯一，防重复注册';
