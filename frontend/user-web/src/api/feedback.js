import request from '../utils/request'
export const submitFeedback = (payload) => request.post('/feedback', payload)
export const listFeedback = (params) => request.get('/feedback/mine', { params })
