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
    <div class="hint">修改保存后即时生效，预约、退约与信用校验按新参数执行。</div>
    <el-table v-loading="loading" :data="rows" border>
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

async function load() {
  loading.value = true
  try {
    const res = await getRules()
    rows.value = (res.data || []).map((r) => ({
      id: r.id,
      key: r.ruleKey,
      note: r.note,
      value: Number(r.ruleValue) || 0
    }))
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
