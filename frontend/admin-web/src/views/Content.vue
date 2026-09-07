<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane label="轮播图" name="banner">
        <div class="toolbar">
          <el-button type="primary" @click="openBannerAdd">新增轮播</el-button>
        </div>
        <el-table v-loading="bannerLoading" :data="banners" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="图片" width="140">
            <template #default="{ row }">
              <img
                v-if="row.image && !row.image.startsWith('/img/')"
                :src="row.image"
                class="thumb"
                alt="轮播图"
              />
              <span v-else style="color: #bbb; font-size: 12px">默认图</span>
            </template>
          </el-table-column>
          <el-table-column prop="link" label="跳转链接" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="80" align="center" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'normal' ? 'success' : 'info'" size="small">
                {{ row.status === 'normal' ? '展示' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button type="primary" link @click="openBannerEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="onBannerDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="公告" name="notice">
        <div class="toolbar">
          <el-button type="primary" @click="openNoticeAdd">新增公告</el-button>
        </div>
        <el-table v-loading="noticeLoading" :data="notices" border stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column prop="content" label="内容" min-width="300" show-overflow-tooltip />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'normal' ? 'success' : 'info'" size="small">
                {{ row.status === 'normal' ? '展示' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="发布时间" width="170" align="center" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button type="primary" link @click="openNoticeEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="onNoticeDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 轮播编辑 -->
    <el-dialog v-model="bannerVisible" :title="bannerForm.id ? '编辑轮播' : '新增轮播'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="上传图片" required>
          <div class="upload-box">
            <el-upload
              :show-file-list="false"
              :http-request="uploadReq"
              accept="image/png,image/jpeg,image/gif,image/webp"
            >
              <el-button type="primary" plain>选择图片上传</el-button>
            </el-upload>
            <span class="upload-tip">支持 jpg/png/gif/webp，≤5MB</span>
          </div>
        </el-form-item>
        <el-form-item label="图片预览">
          <img v-if="bannerForm.image && !bannerForm.image.startsWith('/img/')" :src="bannerForm.image" class="preview" alt="预览" />
          <span v-else style="color: #bbb">暂无图片</span>
        </el-form-item>
        <el-form-item label="图片地址">
          <el-input v-model="bannerForm.image" placeholder="上传后自动填入，也可直接填写地址" />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model="bannerForm.link" placeholder="可选" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="bannerForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="bannerForm.status">
            <el-radio value="normal">展示</el-radio>
            <el-radio value="disabled">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bannerVisible = false">取消</el-button>
        <el-button type="primary" :loading="bannerSaving" @click="saveBanner">保存</el-button>
      </template>
    </el-dialog>

    <!-- 公告编辑 -->
    <el-dialog v-model="noticeVisible" :title="noticeForm.id ? '编辑公告' : '新增公告'" width="600px">
      <el-form label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="noticeForm.title" maxlength="100" placeholder="公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="noticeForm.content" type="textarea" :rows="5" maxlength="1000" placeholder="公告内容" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="noticeForm.status">
            <el-radio value="normal">展示</el-radio>
            <el-radio value="disabled">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeVisible = false">取消</el-button>
        <el-button type="primary" :loading="noticeSaving" @click="saveNotice">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addBanner,
  addNotice,
  deleteBanner,
  deleteNotice,
  getBanners,
  getNotices,
  updateBanner,
  updateNotice,
  uploadImage
} from '../api/admin'

const tab = ref('banner')

const banners = ref([])
const bannerLoading = ref(false)
const bannerVisible = ref(false)
const bannerSaving = ref(false)
const emptyBanner = () => ({ id: null, image: '', link: '', sort: 1, status: 'normal' })
const bannerForm = reactive(emptyBanner())

const notices = ref([])
const noticeLoading = ref(false)
const noticeVisible = ref(false)
const noticeSaving = ref(false)
const emptyNotice = () => ({ id: null, title: '', content: '', status: 'normal' })
const noticeForm = reactive(emptyNotice())

async function loadBanners() {
  bannerLoading.value = true
  try {
    const res = await getBanners()
    banners.value = res.data || []
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    bannerLoading.value = false
  }
}

async function loadNotices() {
  noticeLoading.value = true
  try {
    const res = await getNotices()
    notices.value = res.data || []
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    noticeLoading.value = false
  }
}

async function uploadReq(options) {
  try {
    const res = await uploadImage(options.file)
    bannerForm.image = res.data && res.data.url
    ElMessage.success('图片上传成功')
  } catch (e) {
    // 提示已由拦截器处理
  }
}

function openBannerAdd() {
  Object.assign(bannerForm, emptyBanner())
  bannerVisible.value = true
}

function openBannerEdit(row) {
  Object.assign(bannerForm, {
    id: row.id,
    image: row.image || '',
    link: row.link || '',
    sort: row.sort || 0,
    status: row.status || 'normal'
  })
  bannerVisible.value = true
}

async function saveBanner() {
  if (!bannerForm.image || !bannerForm.image.trim()) {
    ElMessage.warning('请上传或填写图片地址')
    return
  }
  bannerSaving.value = true
  try {
    const payload = {
      image: bannerForm.image.trim(),
      link: bannerForm.link || '',
      sort: bannerForm.sort,
      status: bannerForm.status
    }
    if (bannerForm.id) {
      await updateBanner(bannerForm.id, payload)
      ElMessage.success('轮播已更新')
    } else {
      await addBanner(payload)
      ElMessage.success('轮播已新增')
    }
    bannerVisible.value = false
    loadBanners()
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    bannerSaving.value = false
  }
}

async function onBannerDelete(row) {
  try {
    await ElMessageBox.confirm('确认删除该轮播？', '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteBanner(row.id)
    ElMessage.success('已删除')
    loadBanners()
  } catch (e) {
    // 提示已由拦截器处理
  }
}

function openNoticeAdd() {
  Object.assign(noticeForm, emptyNotice())
  noticeVisible.value = true
}

function openNoticeEdit(row) {
  Object.assign(noticeForm, {
    id: row.id,
    title: row.title,
    content: row.content || '',
    status: row.status || 'normal'
  })
  noticeVisible.value = true
}

async function saveNotice() {
  if (!noticeForm.title || !noticeForm.title.trim()) {
    ElMessage.warning('请填写公告标题')
    return
  }
  noticeSaving.value = true
  try {
    const payload = {
      title: noticeForm.title.trim(),
      content: noticeForm.content || '',
      status: noticeForm.status
    }
    if (noticeForm.id) {
      await updateNotice(noticeForm.id, payload)
      ElMessage.success('公告已更新')
    } else {
      await addNotice(payload)
      ElMessage.success('公告已新增')
    }
    noticeVisible.value = false
    loadNotices()
  } catch (e) {
    // 提示已由拦截器处理
  } finally {
    noticeSaving.value = false
  }
}

async function onNoticeDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除公告「${row.title}」？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteNotice(row.id)
    ElMessage.success('已删除')
    loadNotices()
  } catch (e) {
    // 提示已由拦截器处理
  }
}

onMounted(() => {
  loadBanners()
  loadNotices()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}
.thumb {
  width: 100px;
  height: 46px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #eee;
}
.upload-box {
  display: flex;
  align-items: center;
  gap: 10px;
}
.upload-tip {
  color: #999;
  font-size: 12px;
}
.preview {
  max-width: 320px;
  max-height: 120px;
  border: 1px solid #eee;
  border-radius: 4px;
}
</style>
