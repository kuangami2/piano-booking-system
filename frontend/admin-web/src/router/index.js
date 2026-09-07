import { createRouter, createWebHashHistory } from 'vue-router'
import Layout from '../views/Layout.vue'

// 管理端路由，外层布局 + 业务页面，未登录跳转登录页
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '管理员登录' } },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '首页统计' } },
      { path: 'users', name: 'Users', component: () => import('../views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'rooms', name: 'AdminRooms', component: () => import('../views/Rooms.vue'), meta: { title: '琴房管理' } },
      { path: 'bookings', name: 'AdminBookings', component: () => import('../views/Bookings.vue'), meta: { title: '预约管理' } },
      { path: 'rules', name: 'Rules', component: () => import('../views/Rules.vue'), meta: { title: '规则参数' } },
      { path: 'credits', name: 'Credits', component: () => import('../views/Credits.vue'), meta: { title: '信用管理' } },
      { path: 'content', name: 'Content', component: () => import('../views/Content.vue'), meta: { title: '内容管理' } },
      { path: 'feedback', name: 'AdminFeedback', component: () => import('../views/Feedback.vue'), meta: { title: '反馈处理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// 登录校验：无 token 一律回登录页，已登录访问登录页回首页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    return '/login'
  }
  if (token && to.path === '/login') {
    return '/dashboard'
  }
  return true
})

export default router
