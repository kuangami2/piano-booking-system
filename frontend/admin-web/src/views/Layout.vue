<template>
  <el-container class="layout">
    <el-aside width="212px" class="aside">
      <div class="logo">琴房预约 · 管理端</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#0b1c33"
        text-color="rgba(255,255,255,0.68)"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/dashboard">首页统计</el-menu-item>
        <el-menu-item index="/users">用户管理</el-menu-item>
        <el-menu-item index="/rooms">琴房管理</el-menu-item>
        <el-menu-item index="/bookings">预约管理</el-menu-item>
        <el-menu-item index="/rules">规则参数</el-menu-item>
        <el-menu-item index="/credits">信用管理</el-menu-item>
        <el-menu-item index="/content">内容管理</el-menu-item>
        <el-menu-item index="/feedback">反馈处理</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title }}</span>
        <el-dropdown @command="onCommand">
          <span class="user">{{ userLabel }} ▾</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ user ? user.username : '' }}</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null')
  } catch (e) {
    return null
  }
})
const userLabel = computed(() => (user.value && user.value.name) || '管理员')

function onCommand(command) {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.aside {
  background: #0b1c33;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 1;
}
.page-title {
  font-size: 17px;
  font-weight: 600;
}
.user {
  cursor: pointer;
  color: #333;
  outline: none;
}
.main {
  background: #f0f2f5;
}
</style>
