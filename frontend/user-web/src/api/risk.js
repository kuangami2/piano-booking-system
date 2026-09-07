import request from '../utils/request'
export const getNearCancelRisk = () => request.get('/risk/near-cancel')
