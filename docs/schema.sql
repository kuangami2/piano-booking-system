-- 琴房预约系统建表脚本
-- 风格：字段与表均带 COMMENT，外键显式声明便于 ER 工具识别关系

CREATE TABLE sys_user (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '登录名',
    password VARCHAR(100) NOT NULL COMMENT '密码，加密存储',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    student_no VARCHAR(20) NOT NULL COMMENT '学号',
    email VARCHAR(100) NOT NULL COMMENT '邮箱，用于找回密码',
    role VARCHAR(10) DEFAULT 'user' COMMENT '角色，user 或 admin',
    is_member TINYINT(1) DEFAULT 0 COMMENT '是否会员，会员可预约对内琴房',
    credit INT DEFAULT 100 COMMENT '信用分',
    openid VARCHAR(64) COMMENT '微信 openid，预留小程序迁移',
    status VARCHAR(10) DEFAULT 'normal' COMMENT '状态，normal 或 banned',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='用户表';

CREATE TABLE room (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '琴房ID',
    name VARCHAR(50) NOT NULL COMMENT '琴房名称',
    room_type VARCHAR(10) NOT NULL COMMENT '类型，inner 对内或 outer 对外',
    open_start INT NOT NULL COMMENT '可约窗起始分钟，360 表示 6 点',
    open_end INT NOT NULL COMMENT '可约窗结束分钟，1320 表示 22 点',
    description VARCHAR(500) COMMENT '琴房说明与注意事项',
    status VARCHAR(10) DEFAULT 'normal' COMMENT '状态'
) COMMENT='琴房表';

CREATE TABLE instrument (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '乐器ID',
    room_id INT NOT NULL COMMENT '所属琴房',
    name VARCHAR(50) NOT NULL COMMENT '乐器名称',
    count INT DEFAULT 1 COMMENT '数量',
    note VARCHAR(200) COMMENT '备注',
    CONSTRAINT fk_inst_room FOREIGN KEY (room_id) REFERENCES room(id)
) COMMENT='乐器表';

CREATE TABLE rule_config (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '规则ID',
    rule_key VARCHAR(50) UNIQUE NOT NULL COMMENT '规则键',
    rule_value VARCHAR(100) NOT NULL COMMENT '规则值',
    note VARCHAR(200) COMMENT '说明'
) COMMENT='规则参数表';

CREATE TABLE booking (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '预约ID',
    user_id INT NOT NULL COMMENT '预约用户',
    room_id INT NOT NULL COMMENT '琴房',
    book_date DATE NOT NULL COMMENT '预约日期',
    start_min INT NOT NULL COMMENT '开始分钟',
    end_min INT NOT NULL COMMENT '结束分钟',
    status VARCHAR(10) DEFAULT 'booked' COMMENT '状态，booked、cancelled 或 finished',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_book_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_book_room FOREIGN KEY (room_id) REFERENCES room(id),
    CONSTRAINT uk_book_slot UNIQUE (room_id, book_date, start_min) COMMENT '并发兜底，同房同日同起点唯一',
    INDEX idx_book_room_date (room_id, book_date),
    INDEX idx_book_user_date (user_id, book_date)
) COMMENT='预约表';

CREATE TABLE watch (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '关注ID',
    user_id INT NOT NULL COMMENT '关注用户',
    room_id INT NOT NULL COMMENT '琴房',
    book_date DATE NOT NULL COMMENT '日期',
    start_min INT NOT NULL COMMENT '开始分钟',
    end_min INT NOT NULL COMMENT '结束分钟',
    status VARCHAR(10) DEFAULT 'active' COMMENT '状态，active 或 notified',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_watch_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_watch_room FOREIGN KEY (room_id) REFERENCES room(id),
    CONSTRAINT uk_watch_slot UNIQUE (user_id, room_id, book_date, start_min) COMMENT '同一用户对同一时段只关注一次',
    INDEX idx_watch_user (user_id),
    INDEX idx_watch_room_date (room_id, book_date)
) COMMENT='空出提醒关注表';

CREATE TABLE credit_log (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '流水ID',
    user_id INT NOT NULL COMMENT '用户',
    change_value INT NOT NULL COMMENT '变动值，负为扣分',
    reason VARCHAR(200) COMMENT '原因',
    operator_id INT COMMENT '经办管理员',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='信用流水表';

CREATE TABLE feedback (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
    user_id INT NOT NULL COMMENT '用户',
    content VARCHAR(500) NOT NULL COMMENT '反馈内容',
    status VARCHAR(10) DEFAULT 'pending' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    CONSTRAINT fk_fb_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='反馈表';

CREATE TABLE message (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    user_id INT NOT NULL COMMENT '接收用户',
    msg_type VARCHAR(20) COMMENT '类型，如空出提醒',
    content VARCHAR(300) NOT NULL COMMENT '内容',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    CONSTRAINT fk_msg_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) COMMENT='站内消息表';

CREATE TABLE notice (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content VARCHAR(1000) COMMENT '内容',
    status VARCHAR(10) DEFAULT 'normal' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '时间'
) COMMENT='公告表';

CREATE TABLE banner (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '轮播ID',
    image VARCHAR(200) NOT NULL COMMENT '图片地址',
    link VARCHAR(200) COMMENT '跳转链接',
    sort INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(10) DEFAULT 'normal' COMMENT '状态'
) COMMENT='轮播图表';
