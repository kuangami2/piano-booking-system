# 管理端 admin-web

琴房预约管理后台，Vue3 加 Vite 加 Element Plus。

## 运行
1. 后端已启动于 127.0.0.1:9090。
2. npm install
3. npm run dev，访问 http://127.0.0.1:5174

## 目录
- src/api：接口封装，request.js 为 axios 实例，自动带 token 并统一处理响应。
- src/router：路由表，页面占位待开发。
- src/views：页面组件。

## 约定
- 请求 /api 前缀经 vite 代理到后端 9090。
- 登录后 token 存 localStorage 键 token，请求头携带 token。
- 管理端接口见 docs/接口清单.md 第八至十五节，接口方法需管理员角色。
- 统计图表可用 ECharts，按需引入。
- 种子管理员 admin/admin123。
