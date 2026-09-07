import axios from 'axios'

export class ApiError extends Error {
  constructor(message, { code, data, response, network = false } = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
    this.response = response
    this.network = network
  }
}

const request = axios.create({ baseURL: '/api', timeout: 10000 })

function clearExpiredLogin(code) {
  if (String(code) !== '401') return
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  if (window.location.hash !== '#/login') window.location.hash = '#/login'
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.token = token
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result?.code === '200' || result?.code === 200) return result.data
    clearExpiredLogin(result?.code)
    throw new ApiError(result?.msg || '请求失败', { code: result?.code, data: result?.data, response })
  },
  (error) => {
    const body = error.response?.data
    const serverCode = body?.code || error.response?.status
    clearExpiredLogin(serverCode)
    throw new ApiError(body?.msg || error.message || '网络异常，请稍后重试', {
      code: body?.code || error.response?.status,
      data: body?.data,
      response: error.response,
      network: !error.response
    })
  }
)

export default request
