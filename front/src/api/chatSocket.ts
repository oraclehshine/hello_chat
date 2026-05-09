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
  const apiBase = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8083/api/v1'
  const wsBase = apiBase
    .replace(/^http:\/\//, 'ws://')
    .replace(/^https:\/\//, 'wss://')
    .replace(/\/api\/v1\/?$/, '')
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
