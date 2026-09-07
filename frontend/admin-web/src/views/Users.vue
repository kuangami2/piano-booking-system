<template>
  <el-card>
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

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="登录名" width="130" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column prop="studentNo" label="学号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
      <el-table-column label="角色" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
            {{ row.role === 'admin' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="会员" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="row.isMember"
            :disabled="row.role === 'admin'"
            @change="(val) => onMemberChange(row, val)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="credit" label="信用分" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'normal' ? 'success' : 'danger'" size="small">
            {{ row.status === 'normal' ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template #default="{ row }">
          <el-button v-if="row.role !== 'admin'" type="primary" link @click="openReset(row)">
            重置密码
          </el-button>
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

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ current.name }}（{{ current.username }}）</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="confirmReset">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getUsers, resetUserPassword, setMember } from '../api/admin'

const keyword = ref('')
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)

const resetVisible = ref(false)
const resetting = ref(false)
const current = ref({})
const newPassword = ref('')

async function load() {
  loading.value = true
  try {
    const res = await getUsers({ keyword: keyword.value || undefined, page: page.value, size: pageSize })
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
  keyword.value = ''
  page.value = 1
  load()
}

function onPageChange(p) {
  page.value = p
  load()
}

async function onMemberChange(row, val) {
  const old = row.isMember
  row.isMember = val
  try {
    await setMember(row.id, val)
    ElMessage.success(val ? '已设为会员' : '已取消会员')
  } catch (e) {
    row.isMember = old
  }
}

function openReset(row) {
  current.value = row
  newPassword.value = ''
  resetVisible.value = true
}

async function confirmReset() {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetting.value = true
  try {
    await resetUserPassword(current.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    resetting.value = false
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
</style>
