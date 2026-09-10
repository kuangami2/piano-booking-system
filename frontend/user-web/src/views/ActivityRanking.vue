<template><div><div class="page-head"><div><h1 class="page-title">活跃度排行</h1><p class="page-subtitle">参与预约、关注和反馈，积累你的校园活跃度。</p></div><el-radio-group v-model="period" @change="load"><el-radio-button value="week">周榜</el-radio-button><el-radio-button value="month">月榜</el-radio-button></el-radio-group></div><el-alert v-if="!enabled" title="排行榜功能待后端 v0.5 接口冻结后开放，当前页面仅作预留。" type="info" show-icon/><el-card v-else class="soft-card" shadow="never"><div v-if="me" class="me-card"><span>我的排名</span><strong>#{{ me.rank }}</strong><span>{{ me.score }} 分</span></div><el-table v-if="items.length" v-loading="loading" :data="items" stripe><el-table-column prop="rank" label="排名" width="90"/><el-table-column prop="name" label="用户"/><el-table-column prop="score" label="活跃分" width="120"/></el-table><el-empty v-else-if="!loading" description="暂无排行数据"/><div class="updated">数据更新时间：{{ updatedAt || '—' }}</div></el-card></div></template>
<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getActivityRanking } from '../api/ranking'

const enabled = import.meta.env.VITE_ACTIVITY_RANKING_ENABLED === 'true'
const period = ref('week')
const items = ref([])
const me = ref(null)
const updatedAt = ref('')
const loading = ref(false)

async function load() {
  if (!enabled) return
  loading.value = true
  try {
    const data = await getActivityRanking({ period: period.value, page: 1, size: 20 })
    items.value = data?.list || []
    me.value = data?.me || null
    updatedAt.value = data?.updatedAt || ''
  } catch {
    items.value = []
    me.value = null
    updatedAt.value = ''
    ElMessage.info('排行榜数据稍后更新')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
<style scoped>.me-card{display:flex;align-items:center;gap:18px;padding:18px 20px;margin-bottom:18px;background:#eff9f7;border-radius:12px;color:#4b6965}.me-card strong{font-size:28px;color:#168f83}.updated{margin-top:18px;color:#99a8ac;font-size:12px}</style>
