import request from '../utils/request'
export const listWatches = (params) => request.get('/watches/mine', { params })
export const addWatch = (payload) => request.post('/watches', payload)
export const removeWatch = (id) => request.delete(`/watches/${id}`)
