import { createRouter, createWebHashHistory } from 'vue-router'

// 用户端路由，页面组件由组员按页面清单逐个实现替换占位
const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'Login', component: () => import('../views/Placeholder.vue'), meta: { title: '登录注册' } },
  { path: '/home', name: 'Home', component: () => import('../views/Placeholder.vue'), meta: { title: '首页' } },
  { path: '/rooms', name: 'Rooms', component: () => import('../views/Placeholder.vue'), meta: { title: '琴房列表' } },
  { path: '/rooms/:id', name: 'RoomDetail', component: () => import('../views/Placeholder.vue'), meta: { title: '琴房详情' } },
  { path: '/bookings', name: 'Bookings', component: () => import('../views/Placeholder.vue'), meta: { title: '我的预约' } },
  { path: '/watches', name: 'Watches', component: () => import('../views/Placeholder.vue'), meta: { title: '我的关注' } },
  { path: '/messages', name: 'Messages', component: () => import('../views/Placeholder.vue'), meta: { title: '消息中心' } },
  { path: '/profile', name: 'Profile', component: () => import('../views/Placeholder.vue'), meta: { title: '个人中心' } },
  { path: '/feedback', name: 'Feedback', component: () => import('../views/Placeholder.vue'), meta: { title: '系统反馈' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
