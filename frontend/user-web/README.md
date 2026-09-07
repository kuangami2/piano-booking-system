# 用户端 user-web

琴房预约系统用户端，Vue3 加 Vite 加 Element Plus。

## 运行
1. 后端已启动于 127.0.0.1:9090。
2. npm install
3. npm run dev，访问 http://127.0.0.1:5173

## 目录
- src/api：接口封装，request.js 为 axios 实例，自动带 token 并统一处理响应。
- src/router：路由表，页面占位待开发。
- src/views：页面组件。

## 约定
- 请求 /api 前缀经 vite 代理到后端 9090。
- 登录后 token 存 localStorage 键 token，请求头携带 token。
- 页面清单与接口契约见仓库 docs/页面清单.md 与 docs/接口清单.md。
