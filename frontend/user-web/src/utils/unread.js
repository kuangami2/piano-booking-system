import { ref } from 'vue'
import { listMessages } from '../api/messages'

/** 未读消息数，App 顶栏徽标与消息中心共用，标记已读后调用 refreshUnread 即时更新 */
export const unreadCount = ref(0)

export async function refreshUnread() {
  if (!localStorage.getItem('token')) {
    unreadCount.value = 0
    return
  }
  try {
    unreadCount.value = (await listMessages({ unread: 1, page: 1, size: 1 }))?.total || 0
  } catch {
    unreadCount.value = 0
  }
}
