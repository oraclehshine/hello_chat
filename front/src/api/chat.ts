import http from './http'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResponse<T> {
  list: T[]
  page: number
  pageSize: number
  total: number
  hasMore: boolean
}

export interface ChatSummary {
  chatId: number
  targetUserId: number
  targetEmail: string
  targetNickname: string
  targetAvatarUrl: string | null
  lastMessageId: number | null
  lastMessageType: 'text' | 'image' | 'file' | null
  lastMessagePreview: string | null
  lastMessageAt: string | null
  unreadCount: number
  createdAt: string
  updatedAt: string
}

export interface ChatMessage {
  messageId: number
  chatId: number
  senderId: number
  messageType: 'text' | 'image' | 'file'
  content: string
  fileId: number | null
  fileName: string | null
  fileMimeType: string | null
  fileSize: number | null
  recallStatus: number
  messageStatus: number
  sentAt: string
  updatedAt: string
  pinnedAt: string | null
}

export interface UserSearchItem {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
}

export async function listChats() {
  const response = await http.get<ApiResponse<ChatSummary[]>>('/chats')
  return response.data.data
}

export async function createPrivateChat(targetUserId: number) {
  const response = await http.post<ApiResponse<ChatSummary>>('/chats/private', { targetUserId })
  return response.data.data
}

export async function listMessages(chatId: number, page = 1, pageSize = 50) {
  const response = await http.get<ApiResponse<PageResponse<ChatMessage>>>(`/chats/${chatId}/messages`, {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function searchMessages(chatId: number, keyword: string, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<ChatMessage>>>('/messages/search', {
    params: { chatId, keyword, page, pageSize },
  })
  return response.data.data
}

export async function sendMessage(
  chatId: number,
  content: string,
  messageType: ChatMessage['messageType'] = 'text',
  fileId?: number,
) {
  const response = await http.post<ApiResponse<ChatMessage>>(`/chats/${chatId}/messages`, {
    messageType,
    content,
    fileId,
  })
  return response.data.data
}

export async function recallMessage(messageId: number) {
  await http.post(`/messages/${messageId}/recall`)
}

export async function deleteMessage(messageId: number) {
  await http.delete(`/messages/${messageId}`)
}

export async function pinMessage(messageId: number) {
  await http.post(`/messages/${messageId}/pin`)
}

export async function unpinMessage(messageId: number) {
  await http.delete(`/messages/${messageId}/pin`)
}

export async function markChatAsRead(chatId: number) {
  await http.post(`/chats/${chatId}/read`)
}

export async function updateTypingStatus(chatId: number, typing: boolean) {
  await http.post(`/chats/${chatId}/typing`, { typing })
}

export async function searchUsers(keyword: string) {
  const response = await http.get<ApiResponse<PageResponse<UserSearchItem>>>('/users/search', {
    params: { keyword, page: 1, pageSize: 10 },
  })
  return response.data.data.list
}
