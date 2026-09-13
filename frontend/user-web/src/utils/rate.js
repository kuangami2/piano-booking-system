/** Throttle high-frequency UI events while preserving the latest call. */
export function throttle(fn, wait = 250) {
  let last = 0; let timer = null; let args; let context
  const invoke = () => { last = Date.now(); timer = null; const a = args; const c = context; args = context = undefined; return fn.apply(c, a) }
  const throttled = function (...next) { args = next; context = this; const remaining = wait - (Date.now() - last); if (remaining <= 0 || last === 0) return invoke(); if (!timer) timer = setTimeout(invoke, remaining) }
  throttled.cancel = () => { if (timer) clearTimeout(timer); timer = null; args = context = undefined }
  return throttled
}
