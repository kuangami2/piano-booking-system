import { describe, expect, it } from 'vitest'
import { minuteToTime, statusLabel, timeRange } from './time'

describe('time utilities', () => {
  it('converts minute values to HH:mm', () => {
    expect(minuteToTime(0)).toBe('00:00')
    expect(minuteToTime(600)).toBe('10:00')
    expect(minuteToTime(1320)).toBe('22:00')
  })

  it('formats time ranges and statuses', () => {
    expect(timeRange({ startMin: 600, endMin: 690 })).toBe('10:00 - 11:30')
    expect(statusLabel('booked')).toBe('已预约')
  })
})
