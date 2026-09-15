<template>
  <div class="login-page">
    <div class="login-intro"><div class="intro-mark">♫</div><h1>琴房预约</h1><p>让每一次练习，都有合适的空间。</p><div class="intro-note">便捷浏览 · 灵活预约 · 空出提醒</div></div>
    <el-card class="login-card soft-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login"><el-form ref="loginForm" :model="loginData" :rules="loginRules" label-position="top" @submit.prevent="submitLogin"><el-form-item label="账号" prop="username"><el-input v-model="loginData.username" placeholder="请输入登录名" /></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="loginData.password" type="password" show-password placeholder="请输入密码" @keyup.enter="submitLogin" /></el-form-item><el-button type="primary" class="full-btn" :loading="loading" @click="submitLogin">登录</el-button></el-form></el-tab-pane>
        <el-tab-pane label="注册" name="register"><el-form ref="registerForm" :model="registerData" :rules="registerRules" label-position="top"><el-form-item label="登录名" prop="username"><el-input v-model="registerData.username" /></el-form-item><el-form-item label="密码" prop="password"><el-input v-model="registerData.password" type="password" show-password /></el-form-item><el-form-item label="姓名" prop="name"><el-input v-model="registerData.name" /></el-form-item><el-form-item label="学号" prop="studentNo"><el-input v-model="registerData.studentNo" /></el-form-item><el-form-item label="邮箱" prop="email"><el-input v-model="registerData.email" /></el-form-item><el-button type="primary" class="full-btn" :loading="loading" @click="submitRegister">创建账号</el-button></el-form></el-tab-pane>
      </el-tabs>
      <div class="login-links"><el-button link type="primary" @click="dialog = 'forgot'">忘记密码？</el-button><el-button link type="info" @click="dialog = 'reset'">已有重置令牌</el-button></div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="dialog === 'forgot' ? '找回密码' : '重置密码'" width="420px">
      <el-form v-if="dialog === 'forgot'" ref="forgotForm" :model="forgotData" :rules="forgotRules" label-position="top" @submit.prevent="submitForgot">
        <el-form-item label="邮箱" prop="email"><el-input v-model="forgotData.email" placeholder="注册时使用的邮箱" /></el-form-item>
        <el-button type="primary" :loading="recoveryLoading" @click="submitForgot">提交申请</el-button>
      </el-form>
      <el-form v-else ref="resetForm" :model="resetData" :rules="resetRules" label-position="top" @submit.prevent="submitReset">
        <el-form-item label="重置令牌" prop="token"><el-input v-model="resetData.token" placeholder="请输入邮件中的令牌" /></el-form-item>
        <el-form-item label="新密码" prop="newPassword"><el-input v-model="resetData.newPassword" type="password" show-password placeholder="8 至 20 位，含字母和数字" /></el-form-item>
        <el-button type="primary" :loading="recoveryLoading" @click="submitReset">提交重置</el-button>
      </el-form>
    </el-dialog>
  </div>
</template>
<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { forgotPassword, login, register, resetPassword } from '../api/auth'

const router = useRouter(); const route = useRoute(); const tab = ref('login'); const loading = ref(false); const recoveryLoading = ref(false); const dialog = ref('')
const loginForm = ref(); const registerForm = ref(); const forgotForm = ref(); const resetForm = ref()
const loginData = reactive({ username: '', password: '' }); const registerData = reactive({ username: '', password: '', name: '', studentNo: '', email: '' })
const forgotData = reactive({ email: '' }); const resetData = reactive({ token: '', newPassword: '' })
const required = (label) => ({ required: true, message: `请输入${label}`, trigger: 'blur' })
const loginRules = { username: required('账号'), password: required('密码') }
const usernameRule = { pattern: /^([A-Za-z][A-Za-z0-9_]{3,19}|\d{10})$/, message: '登录名为 4 至 20 位字母开头，或 10 位学号', trigger: 'blur' }
const passwordRule = { pattern: /^(?=.*[A-Za-z])(?=.*\d)\S{8,20}$/, message: '密码为 8 至 20 位且至少含字母与数字', trigger: 'blur' }
const nameRule = { pattern: /^[\u4e00-\u9fa5A-Za-z]{2,20}$/, message: '姓名为 2 至 20 位汉字或字母', trigger: 'blur' }
const studentNoRule = { pattern: /^\d{10}$/, message: '学号为 10 位数字', trigger: 'blur' }
const registerRules = { username: [required('登录名'), usernameRule], password: [required('密码'), passwordRule], name: [required('姓名'), nameRule], studentNo: [required('学号'), studentNoRule], email: [{ required: true, message: '请输入邮箱', type: 'email', trigger: 'blur' }] }
const forgotRules = { email: [{ required: true, message: '请输入邮箱', type: 'email', trigger: 'blur' }] }
const resetRules = { token: [required('重置令牌')], newPassword: [required('新密码'), passwordRule] }
// 后端校验以后端为准，命中字段错误时直接标在对应输入框下方
function showFieldError(form, e) { const field = e?.data?.field; const item = field && form.value?.fields?.find((f) => f.prop === field); if (item) { item.validateState = 'error'; item.validateMessage = e.message } else { ElMessage.error(e.message) } }
const dialogVisible = computed({ get: () => !!dialog.value, set: (value) => { if (!value) dialog.value = '' } })
async function submitLogin() { if (!(await loginForm.value?.validate().catch(() => false))) return; loading.value = true; try { const data = await login(loginData); localStorage.setItem('token', data.token); localStorage.setItem('user', JSON.stringify(data.user)); ElMessage.success('登录成功'); router.replace(route.query.redirect || '/home') } catch (e) { ElMessage.error(e.message) } finally { loading.value = false } }
async function submitRegister() { if (!(await registerForm.value?.validate().catch(() => false))) return; loading.value = true; try { await register(registerData); ElMessage.success('注册成功，请登录'); tab.value = 'login'; loginData.username = registerData.username } catch (e) { showFieldError(registerForm, e) } finally { loading.value = false } }
async function submitForgot() {
  if (!(await forgotForm.value?.validate().catch(() => false))) return
  recoveryLoading.value = true
  try {
    const data = await forgotPassword({ email: forgotData.email.trim() })
    ElMessage.success(data?.sent === false ? '如果邮箱已注册，重置令牌将发送至邮箱' : '重置令牌已发送，请在 30 分钟内完成重置')
    if (data?.token) {
      resetData.token = data.token
      dialog.value = 'reset'
    }
  } catch (e) {
    ElMessage.error(e.message || '找回密码失败，请稍后重试')
  } finally {
    recoveryLoading.value = false
  }
}
async function submitReset() {
  if (!(await resetForm.value?.validate().catch(() => false))) return
  recoveryLoading.value = true
  try {
    await resetPassword({ token: resetData.token.trim(), newPassword: resetData.newPassword })
    ElMessage.success('密码重置成功，请使用新密码登录')
    dialog.value = ''
    tab.value = 'login'
    loginData.password = ''
    resetData.token = ''
    resetData.newPassword = ''
  } catch (e) {
    ElMessage.error(e.message || '重置密码失败，请检查令牌后重试')
  } finally {
    recoveryLoading.value = false
  }
}
</script>
<style scoped>
.login-page { min-height: 100vh; display: grid; grid-template-columns: minmax(300px, 420px) 420px; align-items: center; justify-content: center; gap: clamp(40px, 8vw, 120px); padding: 32px; }.login-intro { color: #116f68; }.intro-mark { display: grid; place-items: center; width: 68px; height: 68px; color: #fff; background: #168f83; border-radius: 20px; font-size: 40px; box-shadow: 0 12px 30px #168f8340; }.login-intro h1 { margin: 20px 0 8px; font-size: 42px; }.login-intro p { margin: 0; color: #56716f; font-size: 18px; }.intro-note { margin-top: 58px; color: #71908d; font-size: 14px; letter-spacing: .12em; }.login-card { padding: 18px 24px 12px; }.full-btn { width: 100%; height: 42px; }.login-links { margin-top: 14px; text-align: right; }@media(max-width: 760px){.login-page{display:block;padding:36px 18px}.login-intro{text-align:center;margin:12px 0 32px}.intro-mark{margin:auto}.login-intro h1{font-size:32px}.intro-note{margin-top:18px}.login-card{max-width:440px;margin:auto}}
</style>
