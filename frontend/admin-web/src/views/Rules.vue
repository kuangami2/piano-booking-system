<template>
  <el-card>
    <template #header>
      <div class="head">
        <span>规则参数</span>
        <div>
          <el-button :loading="saving" type="primary" @click="save">保存全部</el-button>
        </div>
      </div>
    </template>
    <div class="hint">18 条参数按预约、信用、活跃度、风控分组；修改保存后即时生效。</div>
    <el-table v-loading="loading" :data="rows" border :span-method="groupSpan">
      <el-table-column label="分组" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="groupTag[row.group] || 'info'" size="small">{{ row.group }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="key" label="参数键" width="230" />
      <el-table-column label="参数值" width="180" align="center">
        <template #default="{ row }">
          <el-input-number v-model="row.value" :min="0" :max="100000" :precision="0" controls-position="right" />
        </template>
      </el-table-column>
      <el-table-column prop="note" label="说明" min-width="260" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getRules, saveRules } from '../api/admin'

const rows = ref([])
const loading = ref(false)
const saving = ref(false)

const GROUP_ORDER = { 预约: 1, 信用: 2, 活跃度: 3, 风控: 4, 其他: 9 }
const groupTag = { 预约: 'primary', 信用: 'success', 活跃度: 'warning', 风控: 'danger', 其他: 'info' }

function groupOf(key) {
  const prefix = key.split('.')[0]
  return { booking: '预约', credit: '信用', activity: '活跃度', risk: '风控' }[prefix] || '其他'
}

// 第一列按分组合并单元格，展示四组分类
function groupSpan({ rowIndex, columnIndex }) {
  if (columnIndex !== 0) {
    return [1, 1]
  }
  const current = rows.value[rowIndex]
  if (!current || (rowIndex > 0 && rows.value[rowIndex - 1].group === current.group)) {
    return [0, 0]
  }
  let count = 1
  while (rows.value[rowIndex + count] && rows.value[rowIndex + count].group === current.group) {
    count++
  }
  return [count, 1]
}

async function load() {
  loading.value = true
  try {
    const res = await getRules()
    rows.value = (res.data || []).map((r) => ({
      id: r.id,
      key: r.ruleKey,
      note: r.note,
      value: Number(r.ruleValue) || 0,
      group: groupOf(r.ruleKey)
    }))
    rows.value.sort((a, b) =>
      (GROUP_ORDER[a.group] || 9) - (GROUP_ORDER[b.group] || 9) || a.id - b.id
    )
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!rows.value.length) {
    return
  }
  saving.value = true
  try {
    await saveRules(
      rows.value.map((r) => ({
        key: r.key,
        value: String(r.value)
      }))
    )
    ElMessage.success('已保存，规则即时生效')
    load()
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    saving.value = false
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
.hint {
  color: #e6a23c;
  font-size: 13px;
  margin-bottom: 12px;
}
</style>
