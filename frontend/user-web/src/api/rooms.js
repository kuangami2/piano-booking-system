import request from '../utils/request'
export const listRooms = (params) => request.get('/rooms', { params })
export const getRoom = (id) => request.get(`/rooms/${id}`)
export const getFreeSlots = (id, date) => request.get(`/rooms/${id}/free`, { params: { date } })
