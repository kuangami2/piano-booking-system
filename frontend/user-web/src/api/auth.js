import request from '../utils/request'
export const login = (payload) => request.post('/auth/login', payload)
export const register = (payload) => request.post('/auth/register', payload)
export const profile = () => request.get('/auth/profile')
export const changePassword = (payload) => request.put('/auth/password', payload)
export const forgotPassword = (payload) => request.post('/auth/forgot', payload)
export const resetPassword = (payload) => request.post('/auth/reset', payload)
