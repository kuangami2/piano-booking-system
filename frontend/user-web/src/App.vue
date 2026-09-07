<template>
  <div v-if="route.meta.public" class="public-page"><router-view /></div>
  <el-container v-else class="app-shell">
    <el-header class="topbar">
      <div class="brand" @click="router.push('/home')"><span class="brand-mark">♫</span><span>琴房预约</span></div>
      <div class="top-actions">
        <el-badge :value="unread" :hidden="!unread" class="message-badge">
          <el-button text @click="router.push('/messages')">消息</el-button>
        </el-badge>
        <el-dropdown @command="handleCommand">
          <span class="user-menu">{{ user?.name || user?.username || '用户' }}⌄</span>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="profile">个人中心</el-dropdown-item><el-dropdown-item command="logout" divided>退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container>
      <el-aside :width="collapsed ? '64px' : '220px'" class="sidebar">
        <div class="collapse" @click="collapsed = !collapsed">{{ collapsed ? '☰' : '收起菜单' }}</div>
        <el-menu :default-active="route.path" router :collapse="collapsed" class="nav-menu">
          <el-menu-item index="/home"><span>⌂</span><template #title>首页</template></el-menu-item>
          <el-menu-item index="/rooms"><span>♬</span><template #title>琴房浏览</template></el-menu-item>
          <el-menu-item index="/bookings"><span>▣</span><template #title>我的预约</template></el-menu-item>
          <el-menu-item index="/watches"><span>♡</span><template #title>我的关注</template></el-menu-item>
          <el-menu-item index="/messages"><span>✉</span><template #title>消息中心</template></el-menu-item>
          <el-menu-item v-if="rankingEnabled" index="/ranking/activity"><span>↗</span><template #title>活跃度排行</template></el-menu-item>
          <el-menu-item index="/profile"><span>◎</span><template #title>个人中心</template></el-menu-item>
          <el-menu-item index="/feedback"><span>☷</span><template #title>意见反馈</template></el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="main-content"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMessages } from './api/messages'

const route = useRoute()
const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const unread = ref(0)
const collapsed = ref(false)
const rankingEnabled = import.meta.env.VITE_ACTIVITY_RANKING_ENABLED === 'true'

async function refreshUnread() {
  if (!localStorage.getItem('token')) return
  try { unread.value = (await listMessages({ unread: 1, page: 1, size: 1 }))?.total || 0 } catch { unread.value = 0 }
}
function handleCommand(command) {
  if (command === 'profile') router.push('/profile')
  if (command === 'logout') { localStorage.removeItem('token'); localStorage.removeItem('user'); router.push('/login') }
}
onMounted(refreshUnread)
watch(() => route.path, refreshUnread)
</script>

<style>
:root { --primary: #168f83; --primary-dark: #116f68; --ink: #243746; --muted: #71808c; --bg: #f4f8f8; }
* { box-sizing: border-box; }
body { margin: 0; color: var(--ink); background: var(--bg); font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif; }
.app-shell { min-height: 100vh; background: var(--bg); }
.topbar { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 28px; background: #fff; border-bottom: 1px solid #e8f0ef; }
.brand { display: flex; align-items: center; gap: 10px; color: var(--primary-dark); font-size: 21px; font-weight: 700; cursor: pointer; }
.brand-mark { display: grid; place-items: center; width: 34px; height: 34px; color: #fff; background: var(--primary); border-radius: 10px; }
.top-actions, .user-menu { display: flex; align-items: center; gap: 14px; color: var(--ink); cursor: pointer; }
.sidebar { min-height: calc(100vh - 64px); padding: 16px 10px; background: #fff; border-right: 1px solid #e8f0ef; transition: width .2s; }
.collapse { padding: 8px 12px 14px; color: var(--muted); font-size: 13px; cursor: pointer; text-align: center; }
.nav-menu { border-right: 0; }
.nav-menu span { display: inline-block; width: 22px; margin-right: 8px; text-align: center; }
.main-content { max-width: 1400px; width: 100%; margin: 0 auto; padding: 28px clamp(18px, 4vw, 48px); }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 24px; }
.page-title { margin: 0; font-size: 28px; letter-spacing: .02em; }
.page-subtitle { margin: 7px 0 0; color: var(--muted); font-size: 14px; }
.public-page { min-height: 100vh; background: linear-gradient(140deg,#e8f7f4,#f8fbfb 55%,#eef5ff); }
.soft-card { border: 0; border-radius: 16px; box-shadow: 0 8px 28px rgba(25,66,72,.06); }
.empty-wrap { padding: 42px 0; }
@media (max-width: 768px) { .topbar { padding: 0 14px; } .brand { font-size: 18px; } .sidebar { width: 64px !important; padding: 12px 6px; } .collapse { font-size: 0; } .collapse::after { content: '☰'; font-size: 18px; } .nav-menu:not(.el-menu--collapse) { width: 52px; } .nav-menu .el-menu-item { padding: 0 14px !important; } .nav-menu .el-menu-item span { margin: 0; } .main-content { padding: 20px 14px; } .page-head { flex-direction: column; } .page-title { font-size: 24px; } }
</style>
