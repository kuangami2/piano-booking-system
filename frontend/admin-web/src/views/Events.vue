<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>事件与投递概览</span>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </template>
    <div v-loading="loading">
      <el-alert
        v-if="summary && !summary.brokerReachable"
        type="warning"
        :closable="false"
        show-icon
        title="MQ 未启用或不可达，当前为降级运行模式，提醒按同步路径投递"
        style="margin-bottom: 14px"
      />
      <el-row :gutter="16">
        <el-col v-for="item in metrics" :key="item.label" :span="6" style="margin-bottom: 14px">
          <el-card shadow="never" class="metric">
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-value">{{ item.value }}</div>
          </el-card>
        </el-col>
      </el-row>
      <div class="section-title">事件类型计数</div>
      <el-table v-if="events.length" :data="events" border size="small" style="margin-bottom: 14px">
        <el-table-column label="事件类型" min-width="220">
          <template #default="{ row }">{{ eventLabel(row.eventType) }}</template>
        </el-table-column>
        <el-table-column label="原始类型" min-width="180">
          <template #default="{ row }">{{ row.eventType }}</template>
        </el-table-column>
        <el-table-column prop="count" label="累计次数" width="120" align="center" />
      </el-table>
      <el-empty v-else description="暂无事件记录" :image-size="60" />
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getEventsSummary } from '../api/admin'

const EVENT_LABELS = {
  'booking.cancelled': '退约取消'
}

const loading = ref(false)
const summary = ref(null)

const events = computed(() => (summary.value && summary.value.events) || [])

const metrics = computed(() => {
  const s = summary.value
  if (!s) {
    return []
  }
  return [
    { label: '运行模式', value: s.mode === 'async' ? '异步直投' : '同步降级' },
    { label: 'Broker 可达', value: s.brokerReachable ? '是' : '否' },
    { label: 'Outbox 待投', value: s.outbox ? s.outbox.pending : '-' },
    { label: 'Outbox 已投', value: s.outbox ? s.outbox.sent : '-' },
    { label: '死信深度', value: s.dlq && s.dlq.depth != null ? s.dlq.depth : '-' },
    { label: '死信重投', value: s.dlq && s.dlq.redelivered != null ? s.dlq.redelivered : '-' },
    { label: '最近事件时间', value: s.lastEventAt || '-' }
  ]
})

function eventLabel(type) {
  return EVENT_LABELS[type] || type
}

async function load() {
  loading.value = true
  try {
    const res = await getEventsSummary()
    summary.value = res.data || null
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.metric {
  text-align: center;
}
.metric-label {
  color: #888;
  font-size: 13px;
  margin-bottom: 4px;
}
.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: #1c4d8c;
}
.section-title {
  margin: 4px 0 10px;
  font-weight: 600;
}
</style>
