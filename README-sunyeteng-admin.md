# 孙业腾·管理端实现说明

本分支 feature/sunyeteng-admin 为孙业腾（管理端页面及对应后端接口与统计）的完整实现，仅新增了
backend controller/admin 与 service/admin 子包、backend UploadController 相关新文件、frontend/admin-web
目录内容，未改动组长与另一位组员已有文件。

## 一、做了什么

### 后端（接口清单第八至十五节全部 8 组 + 图片访问）
- admin users：用户列表（keyword/page）、设会员、重置密码
- admin rooms：全量列表（含乐器）、新增/修改（乐器整体覆盖）、删除（有预约或乐器返回 409）
- admin bookings：按日期/琴房/用户/状态分页查询（附琴房名与用户信息）、管理员取消（释放时段并通知关注者）
- admin rules：全量读取、批量更新，保存后 refreshCache 即时生效
- admin credit：流水查询（附用户与经办人）、人工减分/恢复（低于阈值自动暂停预约，逻辑复用组长规则读取）
- admin content：轮播与公告增删改、图片上传（返回 /api/uploads/xxx，公开可访问）
- admin feedback：列表（附用户信息）与状态处理
- admin stats：首页概览（琴房数/今日/本周/累计）与琴房使用率明细（区间有效预约时长 ÷ 可约容量）
- 全部管理接口方法均标注 @RequireRole("admin")

### 前端 frontend/admin-web（页面清单第二节 9 页全部实现）
登录 /login、首页统计 /dashboard、用户 /users、琴房 /rooms、预约 /bookings、
规则 /rules、信用 /credits、内容 /content、反馈 /feedback，含侧边布局、路由登录守卫、接口封装。

## 二、新增文件一览
- backend/src/main/java/com/roomreservation/controller/admin/ 下 9 个控制器
- backend/src/main/java/com/roomreservation/service/admin/ 下 8 个 Service
- frontend/admin-web/src/api/admin.js、src/views/ 下 10 个页面（Layout 与 9 页）、src/utils/format.js、router 重写

## 三、怎么看效果
1. 建库并启动后端（backend 目录执行 SQL 后运行 RoomReservationApplication，端口 9090）。
2. 启动前端：cd frontend/admin-web && npm install && npm run dev
3. 浏览器打开 http://127.0.0.1:5174，用 admin / admin123 登录，按左侧菜单逐页操作。

## 四、验证要点（对应任务分配验收）
- 每页请求头都自动携带 token，无权限返回 403 文案。
- 预约管理取消后，关注该时段的用户可在用户端消息中心收到空出提醒。
- 规则保存后立即影响新预约（如 maxDuration、weeklyBookLimit）。
- 信用减到低于阈值（默认 60）后，该用户再预约返回“信用分过低…已暂停”。

## 五、统计口径说明
- 今日/本周预约数 = 当天/本周内 book_date 落在区间的 booked + finished 记录。
- 累计预约记录 = 全部历史预约（含取消）。
- 使用率 = 区间内有效预约分钟数 ÷（琴房可约时间窗分钟 × 天数），上限 100%，保留 1 位小数。
- 轮播与公告状态：normal 展示 / disabled 停用；用户端公共接口仍只取 normal。

## 六、图片上传说明
- 上传接口 POST /api/admin/upload/image（multipart，字段 file，≤5MB，jpg/png/gif/webp）。
- 文件保存在后端运行目录 uploads/ 下，公开访问 GET /api/uploads/{文件名}（@AuthAccess 放行）。

## 七、同步回你本机仓库（未推送、未合并）
在本机原仓库 /Users/andye/room-reservation 执行：

```bash
git fetch /Users/andye/.codex/visualizations/2026/09/07/01a07a94-dcf2-74c3-a070-be61cca89712/room-reservation feature/sunyeteng-admin:feature/sunyeteng-admin
git switch feature/sunyeteng-admin
```

确认无误后再按小组规范合并 develop / 推送远端。
