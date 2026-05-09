<template>
  <section class="chat-workspace">
    <aside class="chat-sidebar">
      <header class="chat-sidebar-header">
        <div>
          <h2>Chats</h2>
          <p>Private conversations</p>
        </div>
        <button class="icon-button" :disabled="loadingChats" title="Refresh" @click="loadChats">Refresh</button>
      </header>

      <div class="search-box">
        <input v-model="searchKeyword" type="search" placeholder="Search email or nickname" @keyup.enter="handleSearch" />
        <button class="secondary-btn compact" :disabled="searching" @click="handleSearch">
          {{ searching ? '...' : 'Search' }}
        </button>
      </div>

      <div v-if="searchResults.length" class="search-results">
        <button
          v-for="user in searchResults"
          :key="user.userId"
          class="user-result"
          @click="startChat(user.userId)"
        >
          <span class="avatar">{{ initials(user.nickname || user.email) }}</span>
          <span>
            <strong>{{ user.nickname || user.email }}</strong>
            <small>{{ user.email }}</small>
          </span>
        </button>
      </div>

      <div class="conversation-list">
        <button
          v-for="chat in chats"
          :key="chat.chatId"
          class="conversation-item"
          :class="{ active: activeChat?.chatId === chat.chatId }"
          @click="selectChat(chat)"
        >
          <span class="avatar">
            <img v-if="chat.targetAvatarUrl" :src="chat.targetAvatarUrl" alt="" />
            <span v-else>{{ initials(chat.targetNickname || chat.targetEmail) }}</span>
          </span>
          <span class="conversation-main">
            <span class="conversation-title">
              <strong>{{ chat.targetNickname || chat.targetEmail }}</strong>
              <time>{{ chat.lastMessageAt ? formatShortTime(chat.lastMessageAt) : '' }}</time>
            </span>
            <small>{{ chat.lastMessagePreview || 'No messages yet' }}</small>
          </span>
          <span v-if="chat.unreadCount > 0" class="unread-badge">{{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}</span>
        </button>
        <div v-if="!loadingChats && !chats.length" class="empty-list">No conversations yet</div>
      </div>
    </aside>

    <main class="chat-panel">
      <header v-if="activeChat" class="chat-header">
        <span class="avatar">
          <img v-if="activeChat.targetAvatarUrl" :src="activeChat.targetAvatarUrl" alt="" />
          <span v-else>{{ initials(activeChat.targetNickname || activeChat.targetEmail) }}</span>
        </span>
        <div class="chat-header-main">
          <h2>{{ activeChat.targetNickname || activeChat.targetEmail }}</h2>
          <p>{{ typingChatId === activeChat.chatId ? 'Typing...' : activeChat.targetEmail }}</p>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>

      <div v-if="!activeChat" class="empty-chat">
        <h2>Select a chat</h2>
        <p>Search a user by email or nickname to start a private conversation.</p>
      </div>

      <div v-else class="message-area">
        <div class="message-search">
          <input v-model="messageKeyword" type="search" placeholder="Search messages in this chat" @keyup.enter="handleMessageSearch" />
          <button class="secondary-btn compact" :disabled="searchingMessages" @click="handleMessageSearch">
            {{ searchingMessages ? '...' : 'Search' }}
          </button>
          <button v-if="messageKeyword" class="secondary-btn compact" @click="clearMessageSearch">Clear</button>
        </div>
        <div ref="messageListRef" class="message-list" @scroll="handleMessageScroll">
          <div v-if="loadingMessages" class="message-state">Loading messages...</div>
          <div v-else-if="!orderedMessages.length" class="message-state">
            {{ messageKeyword ? 'No matching messages' : 'Say hello to start this conversation' }}
          </div>
          <button
            v-if="!loadingMessages && !messageKeyword && hasMoreMessages"
            class="load-more-messages"
            :disabled="loadingEarlierMessages"
            @click="loadEarlierMessages"
          >
            {{ loadingEarlierMessages ? 'Loading...' : 'Load earlier messages' }}
          </button>
          <article
            v-for="message in orderedMessages"
            :key="message.messageId"
            class="message-bubble"
            :class="{ mine: message.senderId === currentUserId }"
          >
            <p v-if="message.recallStatus === 1" class="recalled">Message recalled</p>
            <template v-else>
              <p v-if="message.messageType === 'image'">
                <button class="image-preview-button" @click="previewImageUrl = message.content">
                  <img class="message-image" :src="message.content" alt="chat image" />
                </button>
              </p>
              <p v-else-if="message.messageType === 'file'">
                <a class="file-card" :href="message.content" target="_blank" rel="noreferrer">
                  <span class="file-icon">FILE</span>
                  <span class="file-info">
                    <strong>{{ message.fileName || fileNameFromUrl(message.content) }}</strong>
                    <small>{{ fileMeta(message) }}</small>
                  </span>
                </a>
              </p>
              <p v-else>{{ message.content }}</p>
            </template>
            <footer>
              <span v-if="message.pinnedAt" class="pin-label">Pinned</span>
              <span>{{ formatTime(message.sentAt) }}</span>
              <span v-if="message.senderId === currentUserId" class="read-label">
                {{ message.messageStatus === 2 ? 'Read' : 'Sent' }}
              </span>
              <button v-if="!message.pinnedAt" @click="handlePin(message.messageId)">Pin</button>
              <button v-else @click="handleUnpin(message.messageId)">Unpin</button>
              <button v-if="message.senderId === currentUserId && message.recallStatus === 0" @click="handleRecall(message.messageId)">
                Recall
              </button>
              <button @click="handleDelete(message.messageId)">Delete</button>
            </footer>
          </article>
        </div>

        <form class="composer" @submit.prevent="handleSend">
          <textarea
            v-model="draft"
            rows="1"
            maxlength="1000"
            placeholder="Type a message"
            @input="handleDraftInput"
            @keydown.enter.exact.prevent="handleSend"
          />
          <input ref="imageInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageSelect" />
          <input ref="fileInput" class="hidden-file-input" type="file" @change="handleFileSelect" />
          <button type="button" class="secondary-btn compact" :disabled="sending" @click="pickImage">Image</button>
          <button type="button" class="secondary-btn compact" :disabled="sending" @click="pickFile">File</button>
          <button class="primary-btn compact" :disabled="sending || !draft.trim()">
            {{ sending ? 'Sending' : 'Send' }}
          </button>
        </form>
      </div>
    </main>

    <button v-if="previewImageUrl" class="image-lightbox" @click="previewImageUrl = ''">
      <img :src="previewImageUrl" alt="preview" />
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  createPrivateChat,
  deleteMessage,
  listChats,
  listMessages,
  markChatAsRead,
  pinMessage,
  recallMessage,
  searchUsers,
  searchMessages,
  sendMessage,
  unpinMessage,
  updateTypingStatus,
  type ChatMessage,
  type ChatSummary,
  type UserSearchItem,
} from '../api/chat'
import { createChatSocket, type ChatSocketEvent } from '../api/chatSocket'
import { uploadFile, uploadImage } from '../api/file'

const chats = ref<ChatSummary[]>([])
const activeChat = ref<ChatSummary | null>(null)
const messages = ref<ChatMessage[]>([])
const searchKeyword = ref('')
const searchResults = ref<UserSearchItem[]>([])
const draft = ref('')
const messageKeyword = ref('')
const searchingMessages = ref(false)
const notice = ref('')
const currentUserId = ref(0)
const loadingChats = ref(false)
const loadingMessages = ref(false)
const loadingEarlierMessages = ref(false)
const searching = ref(false)
const sending = ref(false)
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const messageListRef = ref<HTMLElement | null>(null)
const socket = ref<WebSocket | null>(null)
const typingChatId = ref<number | null>(null)
const shouldStickToBottom = ref(true)
const previewImageUrl = ref('')
const messagePage = ref(1)
const messagePageSize = 30
const hasMoreMessages = ref(false)
let typingStopTimer: number | undefined
let typingIndicatorTimer: number | undefined

const orderedMessages = computed(() => [...messages.value].reverse())

onMounted(async () => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    currentUserId.value = JSON.parse(authData).userId
  }
  connectSocket()
  await loadChats()
})

onBeforeUnmount(() => {
  socket.value?.close()
  clearTypingTimers()
})

function connectSocket() {
  socket.value?.close()
  socket.value = createChatSocket(handleSocketEvent)
}

async function handleSocketEvent(event: ChatSocketEvent) {
  const payload = event.payload as { chatId?: number }
  if (event.eventType === 'typing:update') {
    const typingPayload = event.payload as { chatId?: number; userId?: number; typing?: boolean }
    if (typingPayload.userId === currentUserId.value) return
    if (!activeChat.value || typingPayload.chatId !== activeChat.value.chatId) return
    typingChatId.value = typingPayload.typing ? typingPayload.chatId ?? null : null
    window.clearTimeout(typingIndicatorTimer)
    if (typingPayload.typing) {
      typingIndicatorTimer = window.setTimeout(() => {
        typingChatId.value = null
      }, 3500)
    }
    return
  }
  if (!event.eventType.startsWith('message:')) return
  await loadChats()
  if (activeChat.value && payload.chatId === activeChat.value.chatId) {
    await loadMessages()
    await markActiveChatAsRead()
  }
}

async function loadChats() {
  notice.value = ''
  loadingChats.value = true
  try {
    chats.value = await listChats()
    if (activeChat.value) {
      activeChat.value = chats.value.find((chat) => chat.chatId === activeChat.value?.chatId) ?? activeChat.value
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load chats'
  } finally {
    loadingChats.value = false
  }
}

async function selectChat(chat: ChatSummary) {
  activeChat.value = chat
  typingChatId.value = null
  shouldStickToBottom.value = true
  await loadMessages()
  await markActiveChatAsRead()
}

async function loadMessages() {
  if (!activeChat.value) return
  notice.value = ''
  loadingMessages.value = true
  try {
    messagePage.value = 1
    const page = await listMessages(activeChat.value.chatId, messagePage.value, messagePageSize)
    messages.value = page.list
    hasMoreMessages.value = page.hasMore
    await scrollMessagesToBottom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load messages'
  } finally {
    loadingMessages.value = false
  }
}

async function loadEarlierMessages() {
  if (!activeChat.value || loadingEarlierMessages.value || !hasMoreMessages.value) return
  const element = messageListRef.value
  const previousHeight = element?.scrollHeight ?? 0
  loadingEarlierMessages.value = true
  notice.value = ''
  try {
    const nextPage = messagePage.value + 1
    const page = await listMessages(activeChat.value.chatId, nextPage, messagePageSize)
    const existingIds = new Set(messages.value.map((message) => message.messageId))
    messages.value = [...messages.value, ...page.list.filter((message) => !existingIds.has(message.messageId))]
    messagePage.value = nextPage
    hasMoreMessages.value = page.hasMore
    await nextTick()
    if (element) {
      element.scrollTop = element.scrollHeight - previousHeight
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load earlier messages'
  } finally {
    loadingEarlierMessages.value = false
  }
}

async function markActiveChatAsRead() {
  if (!activeChat.value) return
  try {
    await markChatAsRead(activeChat.value.chatId)
    activeChat.value = { ...activeChat.value, unreadCount: 0 }
    chats.value = chats.value.map((chat) =>
      chat.chatId === activeChat.value?.chatId ? { ...chat, unreadCount: 0 } : chat,
    )
  } catch {
    // Read receipts are best effort and should not interrupt chatting.
  }
}

async function handleMessageSearch() {
  if (!activeChat.value) return
  const keyword = messageKeyword.value.trim()
  if (!keyword) {
    await loadMessages()
    return
  }
  searchingMessages.value = true
  notice.value = ''
  try {
    const page = await searchMessages(activeChat.value.chatId, keyword)
    messages.value = page.list
    hasMoreMessages.value = false
    await scrollMessagesToBottom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Message search failed'
  } finally {
    searchingMessages.value = false
  }
}

async function clearMessageSearch() {
  messageKeyword.value = ''
  await loadMessages()
}

async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }
  searching.value = true
  notice.value = ''
  try {
    searchResults.value = (await searchUsers(searchKeyword.value)).filter((user) => user.userId !== currentUserId.value)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Search failed'
  } finally {
    searching.value = false
  }
}

async function startChat(targetUserId: number) {
  notice.value = ''
  try {
    const chat = await createPrivateChat(targetUserId)
    searchResults.value = []
    searchKeyword.value = ''
    await loadChats()
    await selectChat(chat)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to create chat'
  }
}

function pickImage() {
  imageInput.value?.click()
}

function pickFile() {
  fileInput.value?.click()
}

async function handleImageSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  notice.value = ''
  try {
    sending.value = true
    const result = await uploadImage(file, 'chat-image')
    await sendChatMessage('', 'image', result.fileId)
    if (imageInput.value) {
      imageInput.value.value = ''
    }
    await loadMessages()
    await loadChats()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Image upload failed'
  } finally {
    sending.value = false
  }
}

async function handleFileSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  notice.value = ''
  try {
    sending.value = true
    const result = await uploadFile(file, 'chat-file')
    await sendChatMessage(result.fileUrl, 'file', result.fileId)
    if (fileInput.value) {
      fileInput.value.value = ''
    }
    await loadMessages()
    await loadChats()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'File upload failed'
  } finally {
    sending.value = false
  }
}

async function handleSend() {
  if (!activeChat.value) return
  if (!draft.value.trim()) return
  sending.value = true
  notice.value = ''
  try {
    await sendChatMessage(draft.value.trim(), 'text')
    draft.value = ''
    await loadMessages()
    await loadChats()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to send message'
  } finally {
    sending.value = false
  }
}

async function sendChatMessage(content: string, messageType: ChatMessage['messageType'], fileId?: number) {
  if (!activeChat.value) return
  await sendMessage(activeChat.value.chatId, content, messageType, fileId)
  sendTypingStatus(false)
  shouldStickToBottom.value = true
}

function handleDraftInput() {
  if (!activeChat.value) return
  sendTypingStatus(true)
  window.clearTimeout(typingStopTimer)
  typingStopTimer = window.setTimeout(() => sendTypingStatus(false), 1500)
}

function sendTypingStatus(typing: boolean) {
  if (!activeChat.value) return
  updateTypingStatus(activeChat.value.chatId, typing).catch(() => {
    // Typing status is transient and can be dropped without blocking input.
  })
}

function clearTypingTimers() {
  window.clearTimeout(typingStopTimer)
  window.clearTimeout(typingIndicatorTimer)
}

async function handleRecall(messageId: number) {
  try {
    if (!window.confirm('Recall this message?')) return
    await recallMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Recall failed'
  }
}

async function handleDelete(messageId: number) {
  try {
    if (!window.confirm('Delete this message from your view?')) return
    await deleteMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Delete failed'
  }
}

async function handlePin(messageId: number) {
  try {
    await pinMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Pin failed'
  }
}

async function handleUnpin(messageId: number) {
  try {
    await unpinMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Unpin failed'
  }
}

function initials(value: string) {
  return value.slice(0, 2).toUpperCase()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function formatShortTime(value: string) {
  const date = new Date(value)
  const now = new Date()
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString([], { month: '2-digit', day: '2-digit' })
}

function fileNameFromUrl(url: string) {
  try {
    const pathname = new URL(url).pathname
    const name = pathname.split('/').pop()
    return name ? decodeURIComponent(name) : 'Attachment'
  } catch {
    const name = url.split('/').pop()
    return name || 'Attachment'
  }
}

function fileMeta(message: ChatMessage) {
  const parts = []
  if (message.fileMimeType) {
    parts.push(message.fileMimeType)
  }
  if (message.fileSize != null) {
    parts.push(formatFileSize(message.fileSize))
  }
  return parts.length ? parts.join(' · ') : 'Open attachment'
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function handleMessageScroll() {
  const element = messageListRef.value
  if (!element) return
  shouldStickToBottom.value = element.scrollHeight - element.scrollTop - element.clientHeight < 96
}

async function scrollMessagesToBottom() {
  await nextTick()
  if (!shouldStickToBottom.value) return
  const element = messageListRef.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}
</script>

<style scoped>
.chat-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  min-height: calc(100vh - 64px);
  max-width: 1180px;
  background: #fff;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
}

.chat-sidebar {
  border-right: 1px solid #d8e0ea;
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-sidebar-header,
.chat-header {
  min-height: 76px;
  padding: 16px;
  border-bottom: 1px solid #d8e0ea;
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-sidebar-header {
  justify-content: space-between;
}

.chat-sidebar-header h2,
.chat-header h2 {
  font-size: 18px;
  margin: 0;
}

.chat-sidebar-header p,
.chat-header p {
  margin: 4px 0 0;
  color: #53627d;
  font-size: 13px;
}

.search-box {
  padding: 12px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #d8e0ea;
}

.search-box input,
.composer input {
  flex: 1;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.search-results,
.conversation-list {
  display: grid;
  gap: 4px;
  padding: 8px;
}

.conversation-list {
  overflow: auto;
}

.user-result,
.conversation-item {
  width: 100%;
  border: 0;
  background: transparent;
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border-radius: 6px;
  text-align: left;
  cursor: pointer;
}

.conversation-item {
  position: relative;
}

.user-result:hover,
.conversation-item:hover,
.conversation-item.active {
  background: #e9eef6;
}

.conversation-main,
.user-result span:last-child {
  display: grid;
  min-width: 0;
}

.conversation-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.conversation-main strong,
.user-result strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.conversation-title time {
  color: #748198;
  font-size: 12px;
  flex: 0 0 auto;
}

.conversation-main small,
.user-result small {
  color: #53627d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #4f8cff;
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  flex: 0 0 auto;
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.unread-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.chat-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.chat-header-main {
  min-width: 0;
}

.empty-chat {
  margin: auto;
  text-align: center;
  color: #53627d;
}

.empty-list {
  color: #748198;
  padding: 20px 12px;
  text-align: center;
  font-size: 13px;
}

.message-area {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message-search {
  padding: 12px 18px;
  display: flex;
  gap: 8px;
  border-bottom: 1px solid #d8e0ea;
}

.message-search input {
  flex: 1;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message-state {
  margin: auto;
  color: #748198;
  text-align: center;
}

.load-more-messages {
  align-self: center;
  border: 0;
  border-radius: 999px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  font-weight: 700;
  padding: 8px 14px;
}

.load-more-messages:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.message-bubble {
  align-self: flex-start;
  max-width: min(560px, 86%);
  padding: 10px 12px;
  border-radius: 8px;
  background: #eef2f7;
}

.message-bubble.mine {
  align-self: flex-end;
  background: #dff7ef;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-image {
  max-width: 100%;
  max-height: 360px;
  border-radius: 6px;
  display: block;
  object-fit: contain;
}

.image-preview-button {
  display: block;
  max-width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: zoom-in;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: min(300px, 100%);
  color: #10233f;
}

.file-icon {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  background: #dbe7ff;
  color: #2457c5;
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 800;
  flex: 0 0 auto;
}

.file-info {
  display: grid;
  min-width: 0;
}

.file-info strong,
.file-info small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-info strong {
  font-size: 14px;
}

.file-info small {
  color: #53627d;
  font-size: 12px;
}

.message-bubble footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  align-items: center;
  margin-top: 8px;
  font-size: 12px;
  color: #53627d;
}

.read-label {
  color: #0b6b57;
  font-weight: 700;
}

.message-bubble footer button,
.icon-button {
  border: 0;
  background: transparent;
  color: #2457c5;
  cursor: pointer;
}

.icon-button {
  background: #e9eef6;
  border-radius: 6px;
  padding: 8px 10px;
}

.pin-label {
  color: #0b6b57;
  font-weight: 700;
}

.recalled {
  color: #53627d;
  font-style: italic;
}

.composer {
  border-top: 1px solid #d8e0ea;
  padding: 12px;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.composer textarea {
  flex: 1;
  min-width: 0;
  min-height: 42px;
  max-height: 132px;
  resize: vertical;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.compact {
  padding: 10px 14px;
  border-radius: 6px;
}

.hidden-file-input {
  display: none;
}

.image-lightbox {
  position: fixed;
  inset: 0;
  z-index: 20;
  border: 0;
  background: rgba(8, 18, 34, 0.78);
  display: grid;
  place-items: center;
  padding: 28px;
  cursor: zoom-out;
}

.image-lightbox img {
  max-width: min(960px, 96vw);
  max-height: 90vh;
  border-radius: 8px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

@media (max-width: 860px) {
  .chat-workspace {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    max-height: 360px;
    border-right: 0;
    border-bottom: 1px solid #d8e0ea;
  }

  .composer {
    flex-wrap: wrap;
  }

  .composer textarea {
    flex-basis: 100%;
  }
}
</style>
