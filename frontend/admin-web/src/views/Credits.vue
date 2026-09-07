<template>
  <div>
    <el-card>
      <template #header><span>用户信用</span></template>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="按登录名 / 姓名 / 学号搜索"
          clearable
          style="width: 260px"
          @keyup.enter="search"
          @clear="search"
        />
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <el-table v-loading="usersLoading" :data="users" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="登录名" width="130" />
        <el-table-column prop="name" label="姓名" width="110" />
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="credit" label="信用分" width="100" align="center" />
        <el-table-column label="预约状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="isPaused(row) ? 'danger' : 'success'" size="small">
              {{ isPaused(row) ? '已暂停' : '正常' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" align="center">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="openOp(row, 'deduct')">人工减分</el-button>
            <el-button type="success" size="small" @click="openOp(row, 'restore')">人工恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="usersTotal"
          :page-size="usersPageSize"
          :current-page="usersPage"
          @current-change="(p) => { usersPage = p; loadUsers() }"
        />
      </div>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <div class="log-head">
          <span>信用流水</span>
          <div>
            <el-select
              v-model="logUserId"
              placeholder="按用户过滤（可选）"
              clearable
              filterable
              style="width: 220px; margin-right: 8px"
            >
              <el-option v-for="u in users" :key="u.id" :label="`${u.name}（${u.username}）`" :value="u.id" />
            </el-select>
            <el-button type="primary" @click="reloadLogs">刷新</el-button>
          </div>
        </div>
      </template>
      <el-table v-loading="logsLoading" :data="logs" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" width="150">
          <template #default="{ row }">
            {{ row.userName || '-' }}<span v-if="row.username" style="color: #999">（{{ row.username }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="变动" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.changeValue > 0 ? '#67c23a' : '#f56c6c', fontWeight: 600 }">
              {{ row.changeValue > 0 ? '+' : '' }}{{ row.changeValue }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="经办人" width="110" align="center" />
        <el-table-column prop="createdAt" label="时间" width="170" align="center" />
      </el-table>
      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="logsTotal"
          :page-size="logsPageSize"
          :current-page="logsPage"
          @current-change="(p) => { logsPage = p; loadLogs() }"
        />
      </div>
    </el-card>

    <el-dialog v-model="opVisible" :title="opMode === 'deduct' ? '人工减分' : '人工恢复加分'" width="440px">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ current.name }}（{{ current.username }}）当前 {{ current.credit }} 分</span>
        </el-form-item>
        <el-form-item label="分数" required>
          <el-input-number v-model="opForm.points" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="opForm.reason" type="textarea" :rows="2" placeholder="操作原因，将记入流水" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="opVisible = false">取消</el-button>
        <el-button type="primary" :loading="opSaving" @click="confirmOp">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { deductCredit, getCreditLogs, getUsers, restoreCredit } from '../api/admin'

const lowThreshold = 60

const keyword = ref('')
const users = ref([])
const usersTotal = ref(0)
const usersPage = ref(1)
const usersPageSize = 10
const usersLoading = ref(false)

const logs = ref([])
const logsTotal = ref(0)
const logsPage = ref(1)
const logsPageSize = 10
const logsLoading = ref(false)
const logUserId = ref('')

const opVisible = ref(false)
const opSaving = ref(false)
const opMode = ref('deduct')
const current = ref({})
const opForm = reactive({ points: 5, reason: '' })

function isPaused(row) {
  return Number(row.credit) < lowThreshold
}

async function loadUsers() {
  usersLoading.value = true
  try {
    const res = await getUsers({ keyword: keyword.value || undefined, page: usersPage.value, size: usersPageSize })
    users.value = (res.data && res.data.list) || []
    usersTotal.value = Number((res.data && res.data.total) || 0)
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    usersLoading.value = false
  }
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res = await getCreditLogs({
      userId: logUserId.value || undefined,
      page: logsPage.value,
      size: logsPageSize
    })
    logs.value = (res.data && res.data.list) || []
    logsTotal.value = Number((res.data && res.data.total) || 0)
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    logsLoading.value = false
  }
}

function search() {
  usersPage.value = 1
  loadUsers()
}

function reset() {
  keyword.value = ''
  usersPage.value = 1
  loadUsers()
}

function reloadLogs() {
  logsPage.value = 1
  loadLogs()
}

function openOp(row, mode) {
  current.value = row
  opMode.value = mode
  opForm.points = 5
  opForm.reason = ''
  opVisible.value = true
}

async function confirmOp() {
  if (!opForm.reason || !opForm.reason.trim()) {
    ElMessage.warning('请填写操作原因')
    return
  }
  opSaving.value = true
  try {
    const payload = { points: opForm.points, reason: opForm.reason.trim() }
    let res
    if (opMode.value === 'deduct') {
      res = await deductCredit(current.value.id, payload)
    } else {
      res = await restoreCredit(current.value.id, payload)
    }
    const credit = res.data && res.data.credit
    const paused = res.data && res.data.paused
    ElMessage.success(
      `${opMode.value === 'deduct' ? '已减分' : '已加分'}，当前信用分 ${credit}${paused ? '，已低于阈值暂停预约' : ''}`
    )
    opVisible.value = false
    loadUsers()
    loadLogs()
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    opSaving.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadLogs()
})
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
.log-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
