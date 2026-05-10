export interface ChatSocketEvent<T = unknown> {
  eventType: string
  userId: number
  payload: T
}

export function createChatSocket(onMessage: (event: ChatSocketEvent) => void) {
  const token = localStorage.getItem('authToken')
  if (!token) {
    return null
  }

  const configuredApiBase = import.meta.env.VITE_API_BASE_URL
  const wsBase = configuredApiBase
    ? configuredApiBase
        .replace(/^http:\/\//, 'ws://')
        .replace(/^https:\/\//, 'wss://')
        .replace(/\/api\/v1\/?$/, '')
    : `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}`
  const socket = new WebSocket(`${wsBase}/ws/chat?token=${encodeURIComponent(token)}`)

  socket.addEventListener('message', (message) => {
    try {
      onMessage(JSON.parse(message.data) as ChatSocketEvent)
    } catch {
      // Ignore malformed websocket frames from local development tools.
    }
  })

  return socket
}
