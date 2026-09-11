# AGENTS.md，供协作 agent 进仓阅读

## 项目
琴房预约系统，三人协作，需求定稿 v1 见 docs/需求说明.md，设计见 docs/架构说明.md，接口契约见 docs/接口清单.md v0.6，任务分配见 docs/任务分配.md。

## 目录与负责人
- docs：共享开发文档，组长维护。
- backend：后端，Spring Boot 3.5，包 com.roomreservation，以管理平台脚手架为骨架，原名 3+3 脚手架融合管理平台；林义洋负责预约核心、冲突检测、空出提醒、信用与规则。
- frontend：frontend/user-web 用户端 5173，刘善宁；frontend/admin-web 管理端 5174，孙业腾。
- tools：验收与自测脚本。

## 分支约定
Git Flow：feature 分支开发，并入 develop 集成，验证后合并 master 发布。当前 develop 含 v1 与阶段 A、B 全部成果，master 为发布主线。

## 提交规范
格式：模块加冒号加内容，如 booking 实现冲突检测。
动手与提交前先 git pull。

## 构建命令
后端 backend：先执行 backend/sql/room_reservation.sql 建库，数据源配置见 src/main/resources/application.yaml，默认 127.0.0.1:3306/room_reservation、root、root，服务端口 9090；运行 RoomReservationApplication 或 mvn spring-boot:run，空库首启自动补种子数据，种子账号 admin/admin123、demo/123456。
前端：frontend/user-web 与 frontend/admin-web，各自 npm install 后 npm run dev，/api 代理到 9090。

## 集成与验收流程
1. 每日集成：组长 fetch 检查组员 feature 分支，冲突先沟通再合并；合入 master 前在 develop 集成跑通。
2. 管理端接口加 @RequireRole("admin")，全部按 docs/接口清单.md 实现。
3. 契约变更先更新 docs/接口清单.md 再实现，不得静默改接口。
4. 验收演示脚本 tools/demo_test.ps1 在 Windows PowerShell 运行，场景见 docs/验收演示清单.md。
5. develop 同步：组长在 master 集成验证后推送 origin/develop，组员以 develop 为集成基线。
6. 接口冻结：新接口先评审并写入 docs/接口清单.md 标注冻结版本，未冻结不得实现。
7. v1.1 阶段 A 与阶段 B 已合入 develop，阶段 C 候选；所有增强以配置开关控制，Redis 等依赖不可用时 v1 主链路必须可运行。
8. 用户端阶段 B 页面由 .env.local 开关控制，不入库；本地开关示例见 frontend/user-web/.env.example。
