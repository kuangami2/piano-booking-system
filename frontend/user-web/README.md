# 用户端 user-web

琴房预约系统用户端，Vue3 加 Vite 加 Element Plus。

## 运行
1. 后端已启动于 127.0.0.1:9090。
2. npm install
3. 可选：复制 `.env.example` 为 `.env.local`。后端部署阶段 B v0.5 后，设置 `VITE_ACTIVITY_RANKING_ENABLED=true` 启用排行榜；设置 `VITE_NEAR_CANCEL_RISK_ENABLED=true` 启用预约页临近取消风控提示。两个开关默认关闭，不影响 v1 主流程。
4. npm run dev，访问 http://127.0.0.1:5173

## 目录
- src/api：按业务拆分的接口封装，request.js 为 axios 实例，自动带 token 并统一处理响应。
- src/router：用户端路由和登录鉴权。
- src/views：页面组件。
- src/utils：时间、状态和请求工具。

## 约定
- 请求 /api 前缀经 vite 代理到后端 9090。
- 登录后 token 存 localStorage 键 token，请求头携带 token。
- `npm test` 运行请求处理和时间转换单元测试，`npm run build` 执行生产构建检查。
- 页面清单与接口契约见仓库 docs/页面清单.md 与 docs/接口清单.md。
