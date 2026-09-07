// 分钟数转 HH:mm，如 600 -> 10:00
export function minToTime(min) {
  const m = Number(min)
  if (!Number.isFinite(m) || m < 0) {
    return ''
  }
  const h = String(Math.floor(m / 60)).padStart(2, '0')
  const mm = String(Math.floor(m % 60)).padStart(2, '0')
  return h + ':' + mm
}

// Date 转 yyyy-MM-dd
export function fmtDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 后端时间字符串统一显示，如 2026-09-07T10:00:00 -> 2026-09-07 10:00:00
export function fmtDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : ''
}
