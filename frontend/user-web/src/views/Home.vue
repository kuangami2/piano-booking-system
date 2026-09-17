<template>
  <div>
    <div class="page-head">
      <div>
        <h1 class="page-title">你好，{{ user?.name || user?.username || '同学' }} 👋</h1>
        <p class="page-subtitle">今天也为自己留一段专注的练习时间吧。</p>
      </div>
      <el-button type="primary" @click="router.push('/rooms')">预约琴房</el-button>
    </div>

    <div class="hero-grid">
      <el-card class="soft-card banner-card" shadow="never">
        <el-carousel v-if="banners.length" height="100%">
          <el-carousel-item v-for="item in banners" :key="item.id">
            <a :href="item.link || '#/rooms'" class="banner-link">
              <img :src="item.image" alt="琴房活动" />
              <span v-if="item.image?.includes('banner-default')">舒心练琴，从预约开始</span>
            </a>
          </el-carousel-item>
        </el-carousel>
        <div v-else class="banner-empty" role="status">暂无轮播内容</div>
      </el-card>

      <div class="side-stack">
        <el-card class="soft-card quick-card" shadow="never">
          <template #header><div class="card-title">快捷入口</div></template>
          <div class="quick-grid">
            <el-button v-for="item in quickLinks" :key="item.path" text @click="router.push(item.path)">
              <span class="quick-icon">{{ item.icon }}</span>{{ item.label }}
            </el-button>
          </div>
        </el-card>
        <el-card class="soft-card tip-card" shadow="never">
          <div class="tip-label">预约小贴士</div>
          <p>提前查看空闲时段，退约后关注者会收到站内提醒。</p>
        </el-card>
      </div>
    </div>

    <el-card class="soft-card notice-card" shadow="never">
      <template #header><div class="card-title">系统公告</div></template>
      <el-collapse v-if="notices.length">
        <el-collapse-item v-for="notice in notices" :key="notice.id" :title="notice.title" :name="notice.id">
          <p class="notice-content">{{ notice.content }}</p>
          <small>{{ notice.createdAt || '' }}</small>
        </el-collapse-item>
      </el-collapse>
      <el-empty v-else description="暂无公告" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getBanners, getNotices } from '../api/content'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const banners = ref([])
const notices = ref([])
const quickLinks = [
  { label: '我的预约', path: '/bookings', icon: '▣' },
  { label: '我的关注', path: '/watches', icon: '♡' },
  { label: '消息中心', path: '/messages', icon: '✉' },
  { label: '意见反馈', path: '/feedback', icon: '☷' }
]

onMounted(async () => {
  try {
    [banners.value, notices.value] = await Promise.all([getBanners(), getNotices()])
  } catch (e) {
    ElMessage.warning(e.message)
  }
})
</script>

<style scoped>
.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(250px, 1fr);
  gap: 20px;
  align-items: stretch;
}
.banner-card {
  width: 100%;
  aspect-ratio: 21 / 9;
  align-self: start;
  min-height: 0;
  overflow: hidden;
}
.banner-card :deep(.el-carousel) {
  height: 100%;
}
.banner-card :deep(.el-card__body) {
  height: 100%;
  padding: 0;
}
.banner-link {
  display: block;
  position: relative;
  height: 100%;
  background: #e4f3f1;
}
.banner-link img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.banner-link span {
  position: absolute;
  left: 30px;
  bottom: 24px;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  text-shadow: 0 2px 8px #1238;
}
.banner-empty {
  display: grid;
  place-items: center;
  color: #71808c;
  height: 100%;
  padding: 0;
}
.side-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}
.quick-card {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}
.quick-card :deep(.el-card__header) {
  padding: 14px 20px;
}
.quick-card :deep(.el-card__body) {
  display: flex;
  flex: 1;
  min-height: 0;
  padding: 10px 14px 14px;
}
.card-title {
  font-size: 17px;
  font-weight: 700;
}
.quick-grid {
  display: grid;
  flex: 1;
  width: 100%;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: repeat(2, minmax(54px, 1fr));
  gap: 6px;
}
.quick-grid .el-button {
  width: 100%;
  height: 100%;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #39544f;
}
.quick-icon {
  font-size: 22px;
  color: #168f83;
}
.tip-card {
  flex: 0 0 auto;
  background: linear-gradient(135deg, #f0faf8, #f3f7ff);
}
.tip-card :deep(.el-card__body) {
  padding: 12px 16px;
}
.tip-label {
  color: #168f83;
  font-weight: 700;
}
.tip-card p {
  margin: 4px 0 0;
  color: #667b81;
  font-size: 13px;
  line-height: 1.5;
}
.notice-card {
  margin-top: 20px;
}
.notice-content {
  line-height: 1.8;
  margin: 0 0 6px;
  color: #53676d;
}
@media (max-width: 1399px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
  .side-stack {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(220px, 1fr);
  }
}
@media (max-width: 640px) {
  .side-stack {
    grid-template-columns: 1fr;
  }
  .banner-link span {
    left: 18px;
    bottom: 16px;
    font-size: 18px;
  }
}
</style>
