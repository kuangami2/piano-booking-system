import request from '../utils/request'

// 鉴权
export function login(data) {
  return request.post('/auth/login', data)
}

// 用户管理
export function getUsers(params) {
  return request.get('/admin/users', { params })
}
export function setMember(id, isMember) {
  return request.put(`/admin/users/${id}/member`, { isMember })
}
export function resetUserPassword(id, newPassword) {
  return request.put(`/admin/users/${id}/password`, { newPassword })
}

// 琴房管理
export function getRooms() {
  return request.get('/admin/rooms')
}
export function createRoom(data) {
  return request.post('/admin/rooms', data)
}
export function updateRoom(id, data) {
  return request.put(`/admin/rooms/${id}`, data)
}
export function deleteRoom(id) {
  return request.delete(`/admin/rooms/${id}`)
}

// 预约管理
export function getBookings(params) {
  return request.get('/admin/bookings', { params })
}
export function cancelBooking(id) {
  return request.put(`/admin/bookings/${id}/cancel`)
}

// 规则参数
export function getRules() {
  return request.get('/admin/rules')
}
export function saveRules(rules) {
  return request.put('/admin/rules', rules)
}

// 信用管理
export function getCreditLogs(params) {
  return request.get('/admin/credits/logs', { params })
}
export function deductCredit(userId, data) {
  return request.put(`/admin/credits/${userId}/deduct`, data)
}
export function restoreCredit(userId, data) {
  return request.put(`/admin/credits/${userId}/restore`, data)
}

// 内容管理
export function getBanners() {
  return request.get('/admin/banners')
}
export function addBanner(data) {
  return request.post('/admin/banners', data)
}
export function updateBanner(id, data) {
  return request.put(`/admin/banners/${id}`, data)
}
export function deleteBanner(id) {
  return request.delete(`/admin/banners/${id}`)
}
export function getNotices() {
  return request.get('/admin/notices')
}
export function addNotice(data) {
  return request.post('/admin/notices', data)
}
export function updateNotice(id, data) {
  return request.put(`/admin/notices/${id}`, data)
}
export function deleteNotice(id) {
  return request.delete(`/admin/notices/${id}`)
}
export function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/admin/upload/image', form)
}

// 反馈处理
export function getFeedback(params) {
  return request.get('/admin/feedback', { params })
}
export function handleFeedback(id, status) {
  return request.put(`/admin/feedback/${id}/status`, { status })
}

// 统计
export function getStatsOverview() {
  return request.get('/admin/stats/overview')
}
export function getStatsUsage(params) {
  return request.get('/admin/stats/usage', { params })
}
