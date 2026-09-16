# 管理端 admin-web（已实现，孙业腾）

琴房预约管理后台，Vue3 + Vite + Element Plus。对接 docs/接口清单.md 第八至十五节管理端接口。

## 运行
1. 后端已启动于 127.0.0.1:9090（先执行 backend/sql/room_reservation.sql 建库）。
2. npm install
3. npm run dev，浏览器访问 http://127.0.0.1:5174
4. 登录种子管理员 admin / admin123
5. npm test 运行组件测试（规则页参数编辑回归）

## 已实现页面（与页面清单第二节一一对应）
| 路由 | 页面 | 主要功能 |
| --- | --- | --- |
| /login | Login.vue | 管理员登录，非 admin 角色拒绝 |
| /dashboard | Dashboard.vue | 概览卡片 + 琴房使用率区间明细（自绘条形） |
| /users | Users.vue | 用户搜索分页、会员开关、重置密码 |
| /rooms | Rooms.vue | 琴房增删改、对内对外、可约时间窗、乐器明细整体覆盖 |
| /bookings | Bookings.vue | 按日期/琴房/用户/状态查询，管理员取消 |
| /rules | Rules.vue | 规则参数按预约/信用/活跃度/风控分组编辑，批量保存即生效 |
| /credits | Credits.vue | 用户信用查询、人工减分/恢复、流水查看 |
| /content | Content.vue | 轮播与公告增删改、图片上传 |
| /feedback | Feedback.vue | 反馈列表与状态处理 |
| /events | Events.vue | MQ 运行模式、outbox 与死信状态、事件计数概览（阶段 C） |

## 目录
- src/api/admin.js：全部管理端接口封装，复用 utils/request.js（自动带 token、统一 Result 处理）。
- src/router：布局路由 + 登录守卫。
- src/views：布局 Layout 与各页面组件。
- src/utils/format.js：分钟转 HH:mm 等格式化。

## 约定
- /api 前缀经 vite 代理到后端 9090，token 存 localStorage 键 token。
- 统计图表未引入 ECharts，使用 Element 组件与自绘条形，避免额外依赖。
