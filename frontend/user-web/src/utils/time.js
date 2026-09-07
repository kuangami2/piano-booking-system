export function minuteToTime(minutes) {
  if (minutes === null || minutes === undefined || Number.isNaN(Number(minutes))) return '--:--'
  const value = Number(minutes)
  return `${String(Math.floor(value / 60)).padStart(2, '0')}:${String(value % 60).padStart(2, '0')}`
}

export function timeRange(item) {
  return `${minuteToTime(item?.startMin)} - ${minuteToTime(item?.endMin)}`
}

export function todayString() {
  const date = new Date()
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 10)
}

export function statusLabel(status) {
  return ({ booked: '已预约', cancelled: '已取消', finished: '已完成', active: '关注中', notified: '已提醒', pending: '待处理', processed: '已处理', ignored: '已忽略' })[status] || status || '未知'
}
