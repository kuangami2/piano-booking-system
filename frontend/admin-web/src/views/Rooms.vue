<template>
  <el-card>
    <div class="toolbar">
      <span class="count">共 {{ rows.length }} 间琴房</span>
      <el-button type="primary" @click="openAdd">新增琴房</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="琴房名称" min-width="160" />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.roomType === 'inner' ? 'warning' : 'success'" size="small">
            {{ row.roomType === 'inner' ? '对内' : '对外' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="可约时间窗" width="150" align="center">
        <template #default="{ row }">
          {{ minToTime(row.openStart) }} - {{ minToTime(row.openEnd) }}
        </template>
      </el-table-column>
      <el-table-column label="乐器明细" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.instruments && row.instruments.length">
            {{ instrumentsText(row.instruments) }}
          </span>
          <span v-else style="color: #bbb">无</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="说明 / 注意事项" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'normal' ? 'success' : 'info'" size="small">
            {{ row.status === 'normal' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center">
        <template #default="{ row }">
          <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" link @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑琴房' : '新增琴房'" width="760px" top="6vh">
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="琴房名称" required>
              <el-input v-model="form.name" maxlength="50" placeholder="如 对外琴房 B101" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" required>
              <el-radio-group v-model="form.roomType">
                <el-radio value="inner">对内（会员）</el-radio>
                <el-radio value="outer">对外</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="可约开始" required>
              <el-select v-model="form.openStart" style="width: 100%">
                <el-option v-for="m in timeOptions" :key="m" :label="minToTime(m)" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可约结束" required>
              <el-select v-model="form.openEnd" style="width: 100%">
                <el-option v-for="m in timeOptions" :key="m" :label="minToTime(m)" :value="m" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item v-if="editingId" label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="normal">启用</el-radio>
                <el-radio value="disabled">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12" />
        </el-row>
        <el-form-item label="说明 / 注意事项">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="乐器明细">
          <div class="inst-list">
            <div v-for="(inst, idx) in form.instruments" :key="idx" class="inst-row">
              <el-input v-model="inst.name" placeholder="乐器名称" style="width: 180px" />
              <el-input-number v-model="inst.count" :min="1" :max="999" />
              <el-input v-model="inst.note" placeholder="备注（可选）" style="width: 200px" />
              <el-button type="danger" link @click="form.instruments.splice(idx, 1)">移除</el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addInstrument">+ 添加乐器</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRoom, deleteRoom, getRooms, updateRoom } from '../api/admin'
import { minToTime } from '../utils/format'

const rows = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref(null)

const timeOptions = Array.from({ length: 48 }, (_, i) => i * 30)

const emptyForm = () => ({
  name: '',
  roomType: 'outer',
  openStart: 360,
  openEnd: 1320,
  description: '',
  status: 'normal',
  instruments: [{ name: '', count: 1, note: '' }]
})

const form = reactive(emptyForm())

function instrumentsText(list) {
  return list.map((i) => `${i.name}×${i.count}`).join('、')
}

function addInstrument() {
  form.instruments.push({ name: '', count: 1, note: '' })
}

async function load() {
  loading.value = true
  try {
    const res = await getRooms()
    rows.value = res.data || []
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, emptyForm())
  editingId.value = null
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, emptyForm(), {
    name: row.name,
    roomType: row.roomType,
    openStart: row.openStart,
    openEnd: row.openEnd,
    description: row.description || '',
    status: row.status || 'normal',
    instruments: (row.instruments || []).map((i) => ({ name: i.name, count: i.count, note: i.note || '' }))
  })
  editingId.value = row.id
  dialogVisible.value = true
}

function validate() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写琴房名称')
    return false
  }
  if (form.openStart >= form.openEnd) {
    ElMessage.warning('可约结束时间必须晚于开始时间')
    return false
  }
  return true
}

async function save() {
  if (!validate()) {
    return
  }
  const instruments = form.instruments.map((i) => ({
    name: i.name.trim(),
    count: i.count,
    note: i.note ? i.note.trim() : ''
  }))
  if (instruments.some((i) => !i.name)) {
    ElMessage.warning('存在乐器名称为空的行')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      roomType: form.roomType,
      openStart: form.openStart,
      openEnd: form.openEnd,
      description: form.description,
      instruments
    }
    if (editingId.value) {
      payload.status = form.status
      await updateRoom(editingId.value, payload)
      ElMessage.success('琴房已更新')
    } else {
      await createRoom(payload)
      ElMessage.success('琴房已创建')
    }
    dialogVisible.value = false
    load()
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    saving.value = false
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除琴房「${row.name}」？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteRoom(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 409 等提示已由拦截器处理
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.count {
  color: #666;
}
.inst-list {
  width: 100%;
}
.inst-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}
</style>
