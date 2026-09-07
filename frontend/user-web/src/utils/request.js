import axios from 'axios'
import { ElMessage } from 'element-plus'

// axios 实例，统一携带 token 并处理响应
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === '200') {
      return res
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      ElMessage.error('登录已过期，请重新登录')
      window.location.hash = '#/login'
    } else {
      ElMessage.error(error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
