<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 150px" @change="search">
        <el-option label="待处理" value="pending" />
        <el-option label="已处理" value="processed" />
        <el-option label="已忽略" value="ignored" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="detail">
            <p><b>用户：</b>{{ row.userName || '-' }}（{{ row.username || '-' }}，ID {{ row.userId }}）</p>
            <p><b>提交时间：</b>{{ row.createdAt }}</p>
            <p><b>完整内容：</b></p>
            <div class="content">{{ row.content }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="用户" width="150">
        <template #default="{ row }">
          {{ row.userName || '-' }}<span v-if="row.username" style="color: #999">（{{ row.username }}）</span>
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容摘要" min-width="300" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="提交时间" width="170" align="center" />
      <el-table-column label="操作" width="200" align="center">
        <template #default="{ row }">
          <el-button
            type="success"
            link
            :disabled="row.status === 'processed'"
            @click="onHandle(row, 'processed')"
          >
            标记已处理
          </el-button>
          <el-button
            type="warning"
            link
            :disabled="row.status === 'ignored'"
            @click="onHandle(row, 'ignored')"
          >
            忽略
          </el-button>
          <el-button
            type="primary"
            link
            :disabled="row.status === 'pending'"
            @click="onHandle(row, 'pending')"
          >
            恢复待处理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @current-change="onPageChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFeedback, handleFeedback } from '../api/admin'

const status = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

const statusTextMap = { pending: '待处理', processed: '已处理', ignored: '已忽略' }
const statusTagMap = { pending: 'warning', processed: 'success', ignored: 'info' }

function statusText(s) {
  return statusTextMap[s] || s
}

function statusTag(s) {
  return statusTagMap[s] || 'info'
}

async function load() {
  loading.value = true
  try {
    const res = await getFeedback({ status: status.value || undefined, page: page.value, size: pageSize })
    rows.value = (res.data && res.data.list) || []
    total.value = Number((res.data && res.data.total) || 0)
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  status.value = ''
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function onHandle(row, target) {
  const actionText = { processed: '标记为已处理', ignored: '标记为忽略', pending: '恢复为待处理' }[target]
  try {
    await ElMessageBox.confirm(`确认将这条反馈${actionText}？`, '操作确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await handleFeedback(row.id, target)
    ElMessage.success('状态已更新')
    load()
  } catch (e) {
    // 提示已由拦截器处理
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
.detail {
  padding: 4px 40px;
  color: #555;
}
.content {
  background: #f7f8fa;
  padding: 10px 14px;
  border-radius: 6px;
  white-space: pre-wrap;
}
</style>
