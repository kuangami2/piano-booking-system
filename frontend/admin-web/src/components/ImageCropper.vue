<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="640px"
    append-to-body
    @update:model-value="onVisibleChange"
    @closed="reset"
  >
    <input ref="fileRef" type="file" accept="image/png,image/jpeg,image/webp" hidden @change="onPick" />
    <div v-if="!imageSrc" class="pick-row">
      <el-button type="primary" @click="choose">选择图片</el-button>
      <span class="tip">支持 jpg、png、webp，不超过 {{ maxSizeMB }}MB</span>
    </div>
    <template v-else>
      <div class="stage">
        <VueCropper
          ref="cropperRef"
          :img="imageSrc"
          :auto-crop="true"
          :auto-crop-width="outputWidth"
          :auto-crop-height="outputHeight"
          :fixed="true"
          :fixed-number="ratio"
          :center-box="true"
          :can-move="true"
          :can-move-box="true"
          :info="false"
          output-type="jpeg"
        />
      </div>
      <p class="tip">
        输出 {{ outputWidth }}×{{ outputHeight }}，比例 {{ ratio[0] }}:{{ ratio[1] }}；拖动或缩放选区调整裁剪范围
      </p>
    </template>
    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button v-if="imageSrc" @click="choose">重新选择</el-button>
      <el-button type="primary" :loading="uploading" :disabled="!imageSrc" @click="submit">确认并上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
// 图片裁剪上传组件：比例与输出尺寸由调用方给定，上传动作通过 uploader 注入，便于双端复用。
// 注意：user-web 与 admin-web 各有一份同源副本，改动请同步另一工程。
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '裁剪并上传图片' },
  ratio: { type: Array, default: () => [21, 9] },
  outputWidth: { type: Number, default: 1260 },
  outputHeight: { type: Number, default: 540 },
  maxSizeMB: { type: Number, default: 5 },
  uploader: { type: Function, required: true }
})
const emit = defineEmits(['update:modelValue', 'success'])

const fileRef = ref(null)
const cropperRef = ref(null)
const imageSrc = ref('')
const uploading = ref(false)

function onVisibleChange(value) {
  emit('update:modelValue', value)
}

function choose() {
  fileRef.value?.click()
}

function reset() {
  imageSrc.value = ''
  uploading.value = false
}

function onPick(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!/^image\/(png|jpeg|webp)$/.test(file.type)) {
    ElMessage.warning('仅支持 jpg、png、webp 格式')
    return
  }
  if (file.size > props.maxSizeMB * 1024 * 1024) {
    ElMessage.warning('图片不能超过 ' + props.maxSizeMB + 'MB')
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    imageSrc.value = String(reader.result || '')
  }
  reader.onerror = () => ElMessage.error('读取图片失败，请重试')
  reader.readAsDataURL(file)
}

function submit() {
  if (!cropperRef.value) return
  uploading.value = true
  cropperRef.value.getCropBlob(async (blob) => {
    try {
      const url = await props.uploader(blob)
      if (!url) throw new Error('上传未返回图片地址')
      ElMessage.success('图片已上传')
      emit('success', url)
      emit('update:modelValue', false)
    } catch (e) {
      ElMessage.error(e?.message || '上传失败，请重试')
    } finally {
      uploading.value = false
    }
  })
}
</script>

<style scoped>
.pick-row { display: flex; align-items: center; gap: 12px; }
.stage { height: 360px; background: #f5f8f8; border-radius: 10px; overflow: hidden; }
.tip { margin: 10px 0 0; color: #71808c; font-size: 13px; line-height: 1.7; }
</style>
