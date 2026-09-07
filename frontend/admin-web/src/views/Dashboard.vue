<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.key" :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-num">{{ overview[card.key] ?? '-' }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="usage-card">
      <template #header>
        <div class="usage-head">
          <span>琴房使用率明细</span>
          <div>
            <el-date-picker
              v-model="range"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
              style="width: 260px; margin-right: 10px"
            />
            <el-button type="primary" @click="loadUsage">查询</el-button>
          </div>
        </div>
      </template>
      <div v-loading="usageLoading">
        <el-empty v-if="usageRows.length === 0" description="暂无琴房数据" />
        <div v-for="row in usageRows" :key="row.roomId" class="usage-row">
          <span class="r-name">{{ row.name }}</span>
          <div class="bar-bg">
            <div class="bar" :class="{ hot: row.rate >= 60 }" :style="{ width: Math.min(row.rate, 100) + '%' }" />
          </div>
          <span class="r-num">{{ row.usedMinutes }} 分钟 / {{ row.rate }}%</span>
        </div>
        <div class="usage-note">
          口径：区间内有效预约时长（booked、finished）÷ 琴房可约窗容量；今日与本周卡片统计有效预约，累计卡片含全部历史记录。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getStatsOverview, getStatsUsage } from '../api/admin'
import { fmtDate } from '../utils/format'

const cards = [
  { key: 'roomCount', label: '琴房数（启用）' },
  { key: 'todayCount', label: '今日预约数' },
  { key: 'weekCount', label: '本周预约数' },
  { key: 'totalCount', label: '累计预约记录' }
]

const overview = ref({})
const range = ref([fmtDate(new Date(Date.now() - 6 * 24 * 3600 * 1000)), fmtDate(new Date())])
const usageRows = ref([])
const usageLoading = ref(false)

async function loadOverview() {
  try {
    const res = await getStatsOverview()
    overview.value = res.data || {}
  } catch (e) {
    // 提示已由拦截器处理
  }
}

async function loadUsage() {
  if (!range.value || range.value.length !== 2) {
    return
  }
  usageLoading.value = true
  try {
    const res = await getStatsUsage({ from: range.value[0], to: range.value[1] })
    usageRows.value = (res.data && res.data.list) || []
  } catch (e) {
    usageRows.value = []
  } finally {
    usageLoading.value = false
  }
}

onMounted(() => {
  loadOverview()
  loadUsage()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-num {
  font-size: 34px;
  font-weight: 700;
  color: #1c4d8c;
}
.stat-label {
  margin-top: 6px;
  color: #666;
}
.usage-card {
  margin-top: 16px;
}
.usage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.usage-row {
  display: flex;
  align-items: center;
  margin-bottom: 14px;
}
.r-name {
  width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bar-bg {
  flex: 1;
  height: 18px;
  background: #eef1f6;
  border-radius: 9px;
  overflow: hidden;
  margin: 0 12px;
}
.bar {
  height: 100%;
  border-radius: 9px;
  background: #409eff;
  transition: width 0.3s;
}
.bar.hot {
  background: #f56c6c;
}
.r-num {
  width: 140px;
  text-align: right;
  color: #555;
  font-variant-numeric: tabular-nums;
}
.usage-note {
  margin-top: 10px;
  color: #999;
  font-size: 12px;
}
</style>
