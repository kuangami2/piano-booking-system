import request from '../utils/request'
export const getActivityRanking = (params) => request.get('/rankings/activity', { params })
