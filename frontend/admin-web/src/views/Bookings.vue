<template>
  <el-card>
    <div class="toolbar">
      <el-date-picker
        v-model="query.date"
        type="date"
        value-format="YYYY-MM-DD"
        placeholder="预约日期"
        clearable
        style="width: 160px"
      />
      <el-select v-model="query.roomId" placeholder="全部琴房" clearable style="width: 190px">
        <el-option v-for="r in roomOptions" :key="r.id" :label="r.name" :value="r.id" />
      </el-select>
      <el-input
        v-model="query.userId"
        placeholder="用户 ID"
        clearable
        style="width: 130px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="已预约" value="booked" />
        <el-option label="已取消" value="cancelled" />
        <el-option label="已完成" value="finished" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="琴房" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.roomName }}</template>
      </el-table-column>
      <el-table-column label="预约用户" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.userName || '-' }}<span v-if="row.username" style="color: #999">（{{ row.username }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="日期" width="120" align="center">
        <template #default="{ row }">{{ row.bookDate }}</template>
      </el-table-column>
      <el-table-column label="时段" width="140" align="center">
        <template #default="{ row }">{{ minToTime(row.startMin) }} - {{ minToTime(row.endMin) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
            {{ statusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" align="center" />
      <el-table-column label="操作" width="100" align="center">
        <template #default="{ row }">
          <el-button v-if="row.status === 'booked'" type="danger" link @click="onCancel(row)">取消</el-button>
          <span v-else style="color: #bbb">—</span>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelBooking, getBookings, getRooms } from '../api/admin'
import { minToTime } from '../utils/format'

const statusMap = {
  booked: { label: '已预约', type: 'primary' },
  cancelled: { label: '已取消', type: 'info' },
  finished: { label: '已完成', type: 'success' }
}

const query = reactive({ date: '', roomId: '', userId: '', status: '' })
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const roomOptions = ref([])

async function loadRooms() {
  try {
    const res = await getRooms()
    roomOptions.value = res.data || []
  } catch (e) {
    roomOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const res = await getBookings({
      date: query.date || undefined,
      roomId: query.roomId || undefined,
      userId: query.userId || undefined,
      status: query.status || undefined,
      page: page.value,
      size: pageSize
    })
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
  query.date = ''
  query.roomId = ''
  query.userId = ''
  query.status = ''
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function onCancel(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消 ${row.roomName} ${row.bookDate} ${minToTime(row.startMin)}-${minToTime(row.endMin)} 的预约？`,
      '取消确认',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  try {
    await cancelBooking(row.id)
    ElMessage.success('已取消，时段释放并通知关注者')
    load()
  } catch (e) {
    // 提示已由拦截器处理
  }
}

onMounted(() => {
  loadRooms()
  load()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
