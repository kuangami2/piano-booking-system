import request from '../utils/request'
export const listBookings = (params) => request.get('/bookings/mine', { params })
export const createBooking = (payload) => request.post('/bookings', payload)
export const cancelBooking = (id) => request.delete(`/bookings/${id}`)
