# 后端工程 room-reservation-backend

琴房预约管理系统后端，Spring Boot 3.5 加 MyBatis-Plus 3.5.9 加 MySQL，以管理平台脚手架为骨架改造。

## 目录结构
- src/main/java/com/roomreservation：common 公共返回与常量，config 配置与拦截，controller 路由，entity 实体，mapper 数据层，service 业务层，utils 工具。
- src/main/resources/application.yaml：数据源与端口配置。
- sql/room_reservation.sql：建库建表与种子数据脚本。

## 建库与运行
1. 建库：MySQL 执行 sql/room_reservation.sql，生成 room_reservation 库与全部表。
2. 改配置：application.yaml 的 username、password 改为本机 MySQL 账号，ip 默认 127.0.0.1。
3. 启动：IDEA 打开 backend 目录运行 RoomReservationApplication，或 mvn spring-boot:run，端口 9090。
4. 种子账号：admin 密码 admin123 管理员，demo 密码 123456 普通用户；空库首次启动自动补种子数据。

## 鉴权约定
- 登录返回 token，请求头携带 token；公开接口为注册、登录、找回密码。
- 统一响应 {code,msg,data}，code 200 成功、400 参数、401 未登录、403 无权限、500 系统错误。

## 数据库
- 库名 room_reservation，11 张表：sys_user、room、instrument、rule_config、booking、watch、credit_log、feedback、message、notice、banner，见 docs/schema.sql。
