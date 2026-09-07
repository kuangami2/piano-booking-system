# AGENTS.md，供协作 agent 进仓阅读

## 项目
琴房预约系统，三人协作，需求定稿 v1 见 docs/需求说明.md，设计见 docs/架构说明.md，接口契约见 docs/接口清单.md。

## 目录与负责人
- docs：共享开发文档，组长维护。
- backend：后端，Spring Boot 3.5，包 com.roomreservation；林义洋负责预约核心、冲突检测、空出提醒、信用与规则。
- frontend：前端，待搭建；刘善宁用户端、孙业腾管理端。

## 分支约定
Git Flow：feature 分支开发，并入 develop 集成，验证后合并 master 发布。实际主干 master。

## 提交规范
格式：模块加冒号加内容，如 booking 实现冲突检测。
动手与提交前先 git pull。

## 构建命令
后端 backend：先执行 backend/sql/room_reservation.sql 建库，数据源配置见 src/main/resources/application.yaml，默认 127.0.0.1:3306/room_reservation、root、root，服务端口 9090；运行 RoomReservationApplication 或 mvn spring-boot:run，空库首启自动补种子数据，种子账号 admin/admin123、demo/123456。
前端 frontend：待搭建后补充。
