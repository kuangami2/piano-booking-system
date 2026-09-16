// @vitest-environment jsdom
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import ElementPlus from 'element-plus'

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = globalThis.ResizeObserver || ResizeObserverStub
if (!window.matchMedia) {
  window.matchMedia = () => ({
    matches: false,
    addListener() {},
    removeListener() {},
    addEventListener() {},
    removeEventListener() {},
    dispatchEvent() {
      return false
    }
  })
}

const { getEventsSummary } = vi.hoisted(() => ({ getEventsSummary: vi.fn() }))
vi.mock('../src/api/admin', () => ({ getEventsSummary }))

import Events from '../src/views/Events.vue'

async function flush() {
  await new Promise((resolve) => setTimeout(resolve, 0))
  await nextTick()
}

describe('管理端事件概览页', () => {
  beforeEach(() => {
    getEventsSummary.mockReset()
  })

  it('异步模式渲染运行状态、outbox、死信与事件计数', async () => {
    getEventsSummary.mockResolvedValue({
      code: '200',
      data: {
        mqEnabled: true,
        mode: 'async',
        brokerReachable: true,
        outbox: { pending: 2, sent: 10 },
        dlq: { depth: 1, redelivered: 0 },
        events: [{ eventType: 'booking.cancelled', count: 8 }],
        lastEventAt: '2026-09-16 10:00:00'
      }
    })
    const wrapper = mount(Events, { global: { plugins: [ElementPlus] } })
    await flush()

    const text = wrapper.text()
    expect(text).toContain('异步直投')
    expect(text).toContain('退约取消')
    expect(text).toContain('booking.cancelled')
    expect(text).toContain('2026-09-16 10:00:00')
    expect(wrapper.find('.el-alert').exists()).toBe(false)
  })

  it('broker 不可达时提示降级并把不可用字段显示为占位符', async () => {
    getEventsSummary.mockResolvedValue({
      code: '200',
      data: {
        mqEnabled: false,
        mode: 'degraded',
        brokerReachable: false,
        outbox: null,
        dlq: null,
        events: [],
        lastEventAt: null
      }
    })
    const wrapper = mount(Events, { global: { plugins: [ElementPlus] } })
    await flush()

    expect(wrapper.find('.el-alert').exists()).toBe(true)
    expect(wrapper.text()).toContain('同步降级')
    expect(wrapper.text()).toContain('降级运行模式')
    expect(wrapper.text()).toContain('暂无事件记录')
  })
})
