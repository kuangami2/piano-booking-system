import { beforeEach, describe, expect, it } from 'vitest'
import request, { ApiError } from './request'

const storage = new Map()
beforeEach(() => {
  storage.clear()
  globalThis.localStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key)
  }
  globalThis.window = { location: { hash: '#/home' } }
})

const response = (data, status = 200) => async (config) => ({ data, status, statusText: 'OK', headers: {}, config })

describe('request response handling', () => {
  it('unwraps a successful Result payload', async () => {
    const data = await request.get('/test', { adapter: response({ code: '200', data: { ok: true } }) })
    expect(data).toEqual({ ok: true })
  })

  it('preserves business conflict details', async () => {
    await expect(request.get('/test', { adapter: response({ code: '409', msg: '时段冲突', data: { reason: 'conflict' } }) }))
      .rejects.toMatchObject({ name: 'ApiError', code: '409', message: '时段冲突', data: { reason: 'conflict' } })
  })

  it('clears login state for a 401 Result', async () => {
    storage.set('token', 'expired')
    await expect(request.get('/test', { adapter: response({ code: '401', msg: '登录失效' }) })).rejects.toBeInstanceOf(ApiError)
    expect(storage.has('token')).toBe(false)
    expect(window.location.hash).toBe('#/login')
  })

  it('marks transport failures as network errors', async () => {
    await expect(request.get('/test', { adapter: async () => { throw new Error('connection refused') } }))
      .rejects.toMatchObject({ name: 'ApiError', network: true })
  })
})
