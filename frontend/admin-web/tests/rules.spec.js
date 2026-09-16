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

// 与迁移脚本 backend/sql/migration_stage_b.sql 对应：原 9 条 + 阶段 B 新增 9 条
const rows = [
  ['booking.minUnit', '30'], ['booking.maxDurationMin', '240'], ['booking.advanceDays', '7'],
  ['booking.weeklyBookLimit', '3'], ['booking.weeklyCancelLimit', '2'],
  ['credit.initialValue', '100'], ['credit.maxValue', '100'], ['credit.lowThreshold', '60'],
  ['credit.dailyRestore', '1'],
  ['activity.weight.login', '1'], ['activity.weight.booking', '5'], ['activity.weight.cancel', '3'],
  ['activity.weight.watch', '2'], ['activity.weight.messageRead', '1'], ['activity.weight.feedback', '2'],
  ['risk.nearCancelWindowMin', '120'], ['risk.nearCancelThreshold', '3'],
  ['risk.blacklistDurationMin', '60']
].map(([key, value], index) => ({ id: index + 1, ruleKey: key, ruleValue: value, note: key }))

const NEW_KEYS = [
  'activity.weight.login', 'activity.weight.booking', 'activity.weight.cancel',
  'activity.weight.watch', 'activity.weight.messageRead', 'activity.weight.feedback',
  'risk.nearCancelWindowMin', 'risk.nearCancelThreshold', 'risk.blacklistDurationMin'
]

const { getRules, saveRules } = vi.hoisted(() => ({ getRules: vi.fn(), saveRules: vi.fn() }))
vi.mock('../src/api/admin', () => ({ getRules, saveRules }))

import Rules from '../src/views/Rules.vue'

async function flush() {
  await new Promise((resolve) => setTimeout(resolve, 0))
  await nextTick()
}

describe('管理端规则页', () => {
  beforeEach(() => {
    getRules.mockReset()
    saveRules.mockReset()
    getRules.mockResolvedValue({ code: '200', data: rows })
    saveRules.mockResolvedValue({ code: '200', data: rows })
  })

  it('按预约/信用/活跃度/风控分组渲染全部参数，并可将修改后的新参数随保存提交', async () => {
    const wrapper = mount(Rules, { global: { plugins: [ElementPlus] } })
    await flush()

    for (const key of NEW_KEYS) {
      expect(wrapper.text()).toContain(key)
    }
    for (const group of ['预约', '信用', '活跃度', '风控']) {
      expect(wrapper.text()).toContain(group)
    }
    const inputs = wrapper.findAll('.el-input-number input')
    expect(inputs.length).toBe(rows.length)

    const targetRow = wrapper.findAll('tbody tr').find((tr) => tr.text().includes('risk.nearCancelThreshold'))
    expect(targetRow).toBeTruthy()
    await targetRow.find('.el-input-number input').setValue('7')
    await flush()
    await wrapper.find('.el-button--primary').trigger('click')
    await flush()

    expect(saveRules).toHaveBeenCalledTimes(1)
    const payload = saveRules.mock.calls[0][0]
    expect(payload.length).toBe(rows.length)
    expect(payload.find((item) => item.key === 'risk.nearCancelThreshold').value).toBe('7')
    expect(payload.find((item) => item.key === 'activity.weight.booking').value).toBe('5')
  })
})
