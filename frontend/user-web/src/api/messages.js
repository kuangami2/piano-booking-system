import request from '../utils/request'
export const listMessages = (params) => request.get('/messages', { params })
export const readMessage = (id) => request.put(`/messages/${id}/read`)
