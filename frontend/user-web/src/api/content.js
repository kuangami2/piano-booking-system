import request from '../utils/request'
export const getBanners = () => request.get('/banners')
export const getNotices = () => request.get('/notices')
export const getRules = () => request.get('/rules')
