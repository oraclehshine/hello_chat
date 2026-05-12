<template>
  <section class="chat-workspace">
    <aside class="chat-sidebar">
      <header class="chat-sidebar-header">
        <div>
          <h2>单聊</h2>
          <p>私聊会话</p>
        </div>
        <button class="icon-button" :disabled="loadingChats" title="刷新" @click="loadChats">
          <SvgIcon name="refresh" />
        </button>
      </header>

      <div class="search-box">
        <input v-model="searchKeyword" type="search" placeholder="搜索邮箱或昵称" @keyup.enter="handleSearch" />
        <button class="secondary-btn compact icon-only-btn" :disabled="searching" title="搜索" @click="handleSearch">
          <SvgIcon name="search" />
        </button>
      </div>

      <div v-if="searchResults.length" class="search-results">
        <button
          v-for="user in searchResults"
          :key="user.userId"
          class="user-result"
          @click="startChat(user.userId)"
        >
          <AvatarFrame :name="user.nickname || user.email" size="sm" />
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
          <AvatarFrame :src="chat.targetAvatarUrl" :name="chat.targetNickname || chat.targetEmail" size="md" />
          <span class="conversation-main">
            <span class="conversation-title">
              <strong>{{ chat.targetNickname || chat.targetEmail }}</strong>
              <time>{{ chat.lastMessageAt ? formatShortTime(chat.lastMessageAt) : '' }}</time>
            </span>
            <small>{{ chat.lastMessagePreview || '暂无消息' }}</small>
          </span>
          <span v-if="chat.unreadCount > 0" class="unread-badge">{{ chat.unreadCount > 99 ? '99+' : chat.unreadCount }}</span>
        </button>
        <div v-if="!loadingChats && !chats.length" class="empty-list">暂无会话</div>
      </div>
    </aside>

    <main class="chat-panel">
      <header v-if="activeChat" class="chat-header">
        <AvatarFrame :src="activeChat.targetAvatarUrl" :name="activeChat.targetNickname || activeChat.targetEmail" size="md" />
        <div class="chat-header-main">
          <h2>{{ activeChat.targetNickname || activeChat.targetEmail }}</h2>
          <p>{{ typingChatId === activeChat.chatId ? '对方正在输入...' : activeChat.targetEmail }}</p>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>

      <div v-if="!activeChat" class="empty-chat">
        <h2>选择一个会话</h2>
        <p>通过邮箱或昵称搜索用户，开始一段私聊。</p>
      </div>

      <div v-else class="message-area">
        <div class="message-search">
          <input v-model="messageKeyword" type="search" placeholder="搜索当前会话消息" @keyup.enter="handleMessageSearch" />
          <button class="secondary-btn compact icon-only-btn" :disabled="searchingMessages" title="搜索消息" @click="handleMessageSearch">
            <SvgIcon name="search" />
          </button>
          <button v-if="messageKeyword" class="secondary-btn compact icon-only-btn" title="清空" @click="clearMessageSearch">
            <SvgIcon name="close" />
          </button>
        </div>
        <div ref="messageListRef" class="message-list" @scroll="handleMessageScroll">
          <div v-if="loadingMessages" class="message-state">消息加载中...</div>
          <div v-else-if="!orderedMessages.length" class="message-state">
            {{ messageKeyword ? '没有匹配的消息' : '发一句你好，开始这段对话' }}
          </div>
          <button
            v-if="!loadingMessages && !messageKeyword && hasMoreMessages"
            class="load-more-messages"
            :disabled="loadingEarlierMessages"
            @click="loadEarlierMessages"
          >
            {{ loadingEarlierMessages ? '加载中...' : '加载更早消息' }}
          </button>
          <article
            v-for="message in orderedMessages"
            :key="message.messageId"
            class="message-row"
            :class="{ mine: message.senderId === currentUserId }"
            @contextmenu.prevent="openMessageMenu($event, message)"
          >
            <AvatarFrame
              :src="message.senderId === currentUserId ? currentUserAvatarUrl : activeChat?.targetAvatarUrl"
              :name="message.senderId === currentUserId ? currentUserDisplayName : activeChat?.targetNickname || activeChat?.targetEmail || 'U'"
              size="sm"
            />
            <div class="message-stack">
              <div v-if="message.senderId !== currentUserId" class="message-label">
                {{ activeChat?.targetNickname || activeChat?.targetEmail || '对方' }}
              </div>
              <div class="message-bubble">
                <p v-if="message.recallStatus === 1" class="recalled">消息已撤回</p>
                <template v-else>
                  <p v-if="message.messageType === 'image'">
                    <button class="image-preview-button" @click="previewImageUrl = resolveAssetUrl(message.content)">
                      <img class="message-image" :src="resolveAssetUrl(message.content)" alt="chat image" />
                    </button>
                  </p>
                  <p v-else-if="message.messageType === 'file'">
                    <a class="file-card" :href="resolveAssetUrl(message.content)" target="_blank" rel="noreferrer">
                      <span class="file-icon">文件</span>
                      <span class="file-info">
                        <strong>{{ message.fileName || fileNameFromUrl(message.content) }}</strong>
                        <small>{{ fileMeta(message) }}</small>
                      </span>
                    </a>
                  </p>
                  <p v-else>{{ message.content }}</p>
                </template>
              </div>
              <footer class="message-meta">
                <span v-if="message.pinnedAt" class="pin-label">已置顶</span>
                <span>{{ formatTime(message.sentAt) }}</span>
                <span v-if="message.senderId === currentUserId" class="read-label">
                  {{ message.messageStatus === 2 ? '已读' : '已发送' }}
                </span>
              </footer>
            </div>
          </article>
        </div>

        <form class="composer" @submit.prevent="handleSend">
          <textarea
            v-model="draft"
            rows="1"
            maxlength="1000"
            placeholder="输入消息"
            @input="handleDraftInput"
            @keydown.enter.exact.prevent="handleSend"
          />
          <input ref="imageInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageSelect" />
          <input ref="fileInput" class="hidden-file-input" type="file" @change="handleFileSelect" />
          <div class="composer-toolbar">
            <div class="composer-tools">
              <button type="button" class="secondary-btn compact icon-only-btn composer-icon" :disabled="sending" title="发送图片" @click="pickImage">
                <SvgIcon name="image" />
              </button>
              <button type="button" class="secondary-btn compact icon-only-btn composer-icon" :disabled="sending" title="发送文件" @click="pickFile">
                <SvgIcon name="file" />
              </button>
            </div>
            <button class="primary-btn compact send-btn" :disabled="sending || !draft.trim()" title="发送">
              <SvgIcon name="send" />
              <span>{{ sending ? '发送中' : '发送' }}</span>
            </button>
          </div>
        </form>
      </div>
    </main>

    <button v-if="previewImageUrl" class="image-lightbox" @click="previewImageUrl = ''">
      <img :src="previewImageUrl" alt="preview" />
    </button>

    <div
      v-if="messageMenu.visible && messageMenu.message"
      class="message-context-menu"
      :style="{ left: `${messageMenu.x}px`, top: `${messageMenu.y}px` }"
    >
      <button v-if="!messageMenu.message.pinnedAt" type="button" @click="runMessageAction(() => handlePin(messageMenu.message!.messageId))">
        置顶
      </button>
      <button v-else type="button" @click="runMessageAction(() => handleUnpin(messageMenu.message!.messageId))">
        取消置顶
      </button>
      <button
        v-if="messageMenu.message.senderId === currentUserId && messageMenu.message.recallStatus === 0"
        type="button"
        @click="runMessageAction(() => handleRecall(messageMenu.message!.messageId))"
      >
        撤回
      </button>
      <button type="button" class="danger-item" @click="runMessageAction(() => handleDelete(messageMenu.message!.messageId))">
        删除
      </button>
    </div>
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
import AvatarFrame from '../components/AvatarFrame.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { resolveAssetUrl } from '../utils/assets'

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
const currentUserDisplayName = ref('我')
const currentUserAvatarUrl = ref('')
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
const messageMenuWidth = 160
const messageMenuHeight = 180
const messageMenu = ref<{ visible: boolean; x: number; y: number; message: ChatMessage | null }>({
  visible: false,
  x: 0,
  y: 0,
  message: null,
})
let typingStopTimer: number | undefined
let typingIndicatorTimer: number | undefined

const orderedMessages = computed(() => [...messages.value].reverse())

onMounted(async () => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    const data = JSON.parse(authData)
    currentUserId.value = data.userId
    currentUserDisplayName.value = data.nickname || data.email || '我'
    currentUserAvatarUrl.value = data.avatarUrl || ''
  }
  window.addEventListener('pointerdown', handleGlobalPointerDown)
  window.addEventListener('keydown', handleEscapeClose)
  window.addEventListener('scroll', closeMessageMenu, true)
  connectSocket()
  await loadChats()
})

onBeforeUnmount(() => {
  socket.value?.close()
  clearTypingTimers()
  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleEscapeClose)
  window.removeEventListener('scroll', closeMessageMenu, true)
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
    notice.value = error instanceof Error ? error.message : '加载会话失败'
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
    notice.value = error instanceof Error ? error.message : '加载消息失败'
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
    notice.value = error instanceof Error ? error.message : '加载更早消息失败'
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
    notice.value = error instanceof Error ? error.message : '搜索消息失败'
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
    notice.value = error instanceof Error ? error.message : '搜索失败'
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
    notice.value = error instanceof Error ? error.message : '创建会话失败'
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
    notice.value = error instanceof Error ? error.message : '图片上传失败'
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
    notice.value = error instanceof Error ? error.message : '文件上传失败'
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
    notice.value = error instanceof Error ? error.message : '发送消息失败'
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
    if (!window.confirm('确认撤回这条消息？')) return
    await recallMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '撤回失败'
  }
}

async function handleDelete(messageId: number) {
  try {
    if (!window.confirm('确认从你的视图中删除这条消息？')) return
    await deleteMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '删除失败'
  }
}

async function handlePin(messageId: number) {
  try {
    await pinMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '置顶失败'
  }
}

async function handleUnpin(messageId: number) {
  try {
    await unpinMessage(messageId)
    await loadMessages()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '取消置顶失败'
  }
}

function openMessageMenu(event: MouseEvent, message: ChatMessage) {
  const padding = 12
  const maxX = Math.max(padding, window.innerWidth - messageMenuWidth - padding)
  const maxY = Math.max(padding, window.innerHeight - messageMenuHeight - padding)
  messageMenu.value = {
    visible: true,
    x: Math.min(event.clientX, maxX),
    y: Math.min(event.clientY, maxY),
    message,
  }
}

function closeMessageMenu() {
  messageMenu.value.visible = false
  messageMenu.value.message = null
}

function handleGlobalPointerDown(event: PointerEvent) {
  if (!messageMenu.value.visible) return
  const target = event.target
  if (target instanceof Element && target.closest('.message-context-menu')) return
  closeMessageMenu()
}

function handleEscapeClose(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeMessageMenu()
  }
}

async function runMessageAction(action: () => Promise<void>) {
  closeMessageMenu()
  await action()
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
    return name ? decodeURIComponent(name) : '附件'
  } catch {
    const name = url.split('/').pop()
    return name || '附件'
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
  return parts.length ? parts.join(' · ') : '打开附件'
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
  min-height: 100%;
  height: 100%;
  width: 100%;
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 28px;
  backdrop-filter: blur(24px);
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
}

.chat-sidebar {
  border-right: 1px solid rgba(255, 255, 255, 0.36);
  background: rgba(247, 249, 252, 0.44);
  backdrop-filter: blur(24px);
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-sidebar-header,
.chat-header {
  min-height: 76px;
  padding: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.34);
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
  transition: transform 0.18s ease, background 0.18s ease, box-shadow 0.18s ease;
  animation: chat-rise-in 0.34s ease both;
}

.conversation-item {
  position: relative;
}

.user-result:hover,
.conversation-item:hover,
.conversation-item.active {
  background: #e9eef6;
  transform: translateY(-1px);
  box-shadow: 0 12px 26px rgba(37, 87, 197, 0.1);
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
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.34);
  backdrop-filter: blur(24px);
  overflow: hidden;
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
  overflow: hidden;
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

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: min(680px, 92%);
  animation: chat-rise-in 0.3s ease both;
}

.message-row.mine {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-stack {
  display: grid;
  gap: 6px;
}

.message-label {
  color: #748198;
  font-size: 12px;
  font-weight: 700;
  padding-inline: 4px;
}

.message-bubble {
  max-width: min(560px, 86%);
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.32);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(18px);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.message-row.mine .message-bubble {
  background: rgba(222, 247, 239, 0.7);
}

.message-bubble:hover {
  transform: translateY(-1px);
  border-color: rgba(85, 131, 255, 0.2);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.1);
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
  transition: transform 0.18s ease, filter 0.18s ease;
}

.image-preview-button:hover {
  transform: scale(1.02);
  filter: saturate(1.04);
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

.message-meta {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
  align-items: center;
  font-size: 12px;
  color: #53627d;
}

.message-row.mine .message-meta {
  justify-content: flex-end;
}

.read-label {
  color: #0b6b57;
  font-weight: 700;
}

.icon-button {
  border: 0;
  background: transparent;
  color: #2457c5;
  cursor: pointer;
}

.icon-button {
  display: grid;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  place-items: center;
  background: #eef4ff;
  color: #2f7eff;
  padding: 0;
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
  border-top: 1px solid rgba(255, 255, 255, 0.34);
  padding: 14px;
  display: grid;
  flex-shrink: 0;
  gap: 12px;
  align-items: stretch;
  background: rgba(248, 251, 255, 0.46);
  backdrop-filter: blur(22px);
}

.composer textarea {
  width: 100%;
  min-width: 0;
  min-height: 88px;
  max-height: 156px;
  resize: vertical;
  border: 1px solid #d8e0ea;
  border-radius: 18px;
  padding: 10px 12px;
  background: #fff;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.composer-icon {
  width: 40px;
  min-width: 40px;
  border-radius: 12px;
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
  border-radius: 16px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.32);
}

.message-context-menu {
  position: fixed;
  z-index: 60;
  display: grid;
  min-width: 144px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 18px;
  background: rgba(18, 28, 45, 0.86);
  backdrop-filter: blur(22px);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.26);
  animation: menu-fade-in 0.16s ease both;
}

.message-context-menu button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: flex-start;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: #f8fbff;
  cursor: pointer;
  padding: 10px 12px;
}

.message-context-menu button:hover {
  background: rgba(255, 255, 255, 0.12);
}

.message-context-menu .danger-item {
  color: #ffb4b4;
}

.conversation-item:nth-child(1),
.message-row:nth-child(1) {
  animation-delay: 0.02s;
}

.conversation-item:nth-child(2),
.message-row:nth-child(2) {
  animation-delay: 0.05s;
}

.conversation-item:nth-child(3),
.message-row:nth-child(3) {
  animation-delay: 0.08s;
}

.conversation-item:nth-child(4),
.message-row:nth-child(4) {
  animation-delay: 0.11s;
}

.conversation-item:nth-child(5),
.message-row:nth-child(5) {
  animation-delay: 0.14s;
}

@keyframes chat-rise-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes menu-fade-in {
  from {
    opacity: 0;
    transform: translateY(6px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
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
    gap: 10px;
  }

  .composer-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .composer-tools,
  .send-btn {
    width: 100%;
  }

  .composer-tools {
    justify-content: flex-start;
  }
}
</style>





