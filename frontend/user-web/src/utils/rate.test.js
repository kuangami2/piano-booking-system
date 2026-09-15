import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { throttle } from './rate'

describe('rate throttle utility', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('runs the first call immediately', () => {
    const fn = vi.fn()
    const throttled = throttle(fn, 100)

    throttled('first')

    expect(fn).toHaveBeenCalledTimes(1)
    expect(fn).toHaveBeenCalledWith('first')
  })

  it('drops calls inside the window and replays the latest args at the tail', () => {
    const fn = vi.fn()
    const throttled = throttle(fn, 100)

    throttled('first')
    vi.advanceTimersByTime(30)
    throttled('second')
    expect(fn).toHaveBeenCalledTimes(1)

    throttled('third')
    expect(fn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(70)
    expect(fn).toHaveBeenCalledTimes(2)
    expect(fn).toHaveBeenLastCalledWith('third')
  })

  it('runs immediately again once the window has passed', () => {
    const fn = vi.fn()
    const throttled = throttle(fn, 100)

    throttled('a')
    vi.advanceTimersByTime(150)
    throttled('b')

    expect(fn).toHaveBeenCalledTimes(2)
    expect(fn).toHaveBeenLastCalledWith('b')
  })

  it('cancel drops the pending trailing call', () => {
    const fn = vi.fn()
    const throttled = throttle(fn, 100)

    throttled('a')
    vi.advanceTimersByTime(10)
    throttled('b')
    throttled.cancel()
    vi.advanceTimersByTime(500)

    expect(fn).toHaveBeenCalledTimes(1)
    expect(fn).toHaveBeenCalledWith('a')
  })

  it('returns the first call result and keeps the receiver binding', () => {
    const ctx = {
      base: 1,
      add: throttle(function (n) {
        return this.base + n
      }, 50)
    }

    expect(ctx.add(2)).toBe(3)
  })

  it('uses the default 250ms window when wait is omitted', () => {
    const fn = vi.fn()
    const throttled = throttle(fn)

    throttled('a')
    vi.advanceTimersByTime(100)
    throttled('b')
    expect(fn).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(150)
    expect(fn).toHaveBeenCalledTimes(2)
    expect(fn).toHaveBeenLastCalledWith('b')
  })
})
