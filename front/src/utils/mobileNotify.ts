let unreadCount = 0
const baseTitle = document.title

export async function prepareMobileNotification() {
  if (!('Notification' in window)) return
  if (Notification.permission === 'default') {
    try {
      await Notification.requestPermission()
    } catch {
      // Ignore permission request failures.
    }
  }
}

function bumpUnreadTitle() {
  unreadCount += 1
  document.title = `(${unreadCount}) ${baseTitle}`
}

export function clearUnreadTitle() {
  unreadCount = 0
  document.title = baseTitle
}

export function notifyNewMessage(title: string, body: string) {
  if (document.visibilityState === 'visible') return
  if ('vibrate' in navigator) {
    navigator.vibrate([120, 60, 120])
  }
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(title, { body, tag: 'hello-chat-message' })
    return
  }
  bumpUnreadTitle()
}
