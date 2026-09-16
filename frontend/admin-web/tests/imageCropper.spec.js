// @vitest-environment jsdom
import { describe, expect, it } from 'vitest'

// 组件与页面接入分离：这里只校验对外契约（默认规格与上传接口），
// 交互效果由接入方在浏览器中验证。
import ImageCropper from '../src/components/ImageCropper.vue'

describe('ImageCropper 裁剪上传组件契约', () => {
  it('默认规格为轮播图 21:9 与 1260×540', () => {
    expect(ImageCropper.props.ratio.default()).toEqual([21, 9])
    expect(ImageCropper.props.outputWidth.default).toBe(1260)
    expect(ImageCropper.props.outputHeight.default).toBe(540)
  })

  it('上传动作由调用方注入且为必填', () => {
    expect(ImageCropper.props.uploader.required).toBe(true)
    expect(ImageCropper.props.uploader.type).toBe(Function)
  })

  it('原图大小上限默认 5MB，比例与尺寸可按场景覆盖', () => {
    expect(ImageCropper.props.maxSizeMB.default).toBe(5)
    expect(ImageCropper.props.modelValue.default).toBe(false)
  })

  it('对外抛出可见性与成功事件', () => {
    expect(ImageCropper.emits).toContain('update:modelValue')
    expect(ImageCropper.emits).toContain('success')
  })
})
