import { createRouter, createWebHashHistory } from 'vue-router'

// 管理端路由，登录校验与页面组件由组员按页面清单逐个实现替换占位
const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('../views/Placeholder.vue'), meta: { title: '管理员登录' } },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Placeholder.vue'), meta: { title: '首页统计' } },
  { path: '/users', name: 'Users', component: () => import('../views/Placeholder.vue'), meta: { title: '用户管理' } },
  { path: '/rooms', name: 'AdminRooms', component: () => import('../views/Placeholder.vue'), meta: { title: '琴房管理' } },
  { path: '/bookings', name: 'AdminBookings', component: () => import('../views/Placeholder.vue'), meta: { title: '预约管理' } },
  { path: '/rules', name: 'Rules', component: () => import('../views/Placeholder.vue'), meta: { title: '规则参数' } },
  { path: '/credits', name: 'Credits', component: () => import('../views/Placeholder.vue'), meta: { title: '信用管理' } },
  { path: '/content', name: 'Content', component: () => import('../views/Placeholder.vue'), meta: { title: '内容管理' } },
  { path: '/feedback', name: 'AdminFeedback', component: () => import('../views/Placeholder.vue'), meta: { title: '反馈处理' } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
