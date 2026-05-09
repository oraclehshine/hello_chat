<template>
  <section class="group-workspace">
    <aside class="group-sidebar">
      <header class="group-header">
        <div>
          <h2>Groups</h2>
          <p>Phase 3 group chat</p>
        </div>
        <button class="icon-button" :disabled="loadingGroups" @click="loadGroups">Refresh</button>
      </header>

      <form class="create-group" @submit.prevent="handleCreateGroup">
        <input v-model="newGroupName" maxlength="30" placeholder="Group name" />
        <textarea v-model="newGroupDescription" rows="2" placeholder="Description"></textarea>
        <div class="member-picker">
          <input v-model="userKeyword" placeholder="Search users to add" @keyup.enter.prevent="handleUserSearch" />
          <button type="button" class="secondary-btn compact" :disabled="searchingUsers" @click="handleUserSearch">
            {{ searchingUsers ? '...' : 'Search' }}
          </button>
        </div>
        <div v-if="userResults.length" class="user-results">
          <button v-for="user in userResults" :key="user.userId" type="button" @click="toggleMember(user)">
            <span class="avatar">{{ initials(user.nickname || user.email) }}</span>
            <span>
              <strong>{{ user.nickname || user.email }}</strong>
              <small>{{ selectedMemberIds.includes(user.userId) ? 'Selected' : user.email }}</small>
            </span>
          </button>
        </div>
        <div v-if="selectedMembers.length" class="selected-members">
          <span v-for="member in selectedMembers" :key="member.userId">
            {{ member.nickname || member.email }}
            <button type="button" @click="removeMember(member.userId)">x</button>
          </span>
        </div>
        <button class="primary-btn compact" :disabled="creatingGroup || !canCreateGroup">
          {{ creatingGroup ? 'Creating' : 'Create group' }}
        </button>
      </form>

      <section class="discover-group">
        <div class="member-picker">
          <input v-model="groupKeyword" placeholder="Search groups" @keyup.enter.prevent="handleGroupSearch" />
          <button type="button" class="secondary-btn compact" :disabled="searchingGroups" @click="handleGroupSearch">
            {{ searchingGroups ? '...' : 'Search' }}
          </button>
        </div>
        <div class="member-picker">
          <input v-model="inviteCodeDraft" placeholder="Invite code" @keyup.enter.prevent="joinByInvite" />
          <button type="button" class="secondary-btn compact" @click="joinByInvite">Join</button>
        </div>
        <div v-if="groupResults.length" class="user-results compact-results">
          <button v-for="group in groupResults" :key="group.groupId" type="button" @click="requestJoin(group)">
            <span class="avatar">
              <img v-if="group.avatarUrl" :src="group.avatarUrl" alt="" />
              <span v-else>{{ initials(group.groupName) }}</span>
            </span>
            <span>
              <strong>{{ group.groupName }}</strong>
              <small>{{ group.memberCount }} members | Request join</small>
            </span>
          </button>
        </div>
        <div class="panel-actions">
          <button type="button" class="secondary-btn compact" :disabled="loadingMyJoinRequests" @click="loadMyJoinRequests">
            {{ loadingMyJoinRequests ? 'Loading' : 'My requests' }}
          </button>
        </div>
        <div v-if="myJoinRequests.length" class="request-status-list">
          <p v-for="request in myJoinRequests" :key="request.requestId">
            <strong>Group #{{ request.groupId }}</strong>
            <small>{{ joinRequestStatus(request.status) }}</small>
          </p>
        </div>
      </section>

      <div class="group-list">
        <button
          v-for="group in groups"
          :key="group.groupId"
          class="group-item"
          :class="{ active: activeGroup?.groupId === group.groupId }"
          @click="selectGroup(group)"
        >
          <span class="avatar">
            <img v-if="group.avatarUrl" :src="group.avatarUrl" alt="" />
            <span v-else>{{ initials(group.groupName) }}</span>
          </span>
          <span>
            <strong>{{ group.groupName }}</strong>
            <small>{{ group.memberCount }} members</small>
          </span>
          <span v-if="group.mentionUnreadCount > 0" class="mention-badge">@{{ group.mentionUnreadCount }}</span>
          <span v-else-if="group.unreadCount > 0" class="unread-badge">{{ group.unreadCount > 99 ? '99+' : group.unreadCount }}</span>
        </button>
        <div v-if="!loadingGroups && !groups.length" class="empty-list">No groups yet</div>
      </div>
    </aside>

    <main class="group-panel">
      <header v-if="activeGroup" class="chat-header">
        <span class="avatar">
          <img v-if="activeGroup.avatarUrl" :src="activeGroup.avatarUrl" alt="" />
          <span v-else>{{ initials(activeGroup.groupName) }}</span>
        </span>
        <div>
          <h2>{{ activeGroup.groupName }}</h2>
          <p>{{ activeGroup.memberCount }} members</p>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>

      <div v-if="!activeGroup" class="empty-group">
        <h2>Select a group</h2>
        <p>Create or choose a group to start chatting.</p>
      </div>

      <div v-else class="group-chat">
        <aside class="member-panel">
          <section class="profile-box">
            <h3>Group profile</h3>
            <div class="profile-avatar-row">
              <span class="avatar large-avatar">
                <img v-if="profileAvatarPreview || activeGroup.avatarUrl" :src="profileAvatarPreview || activeGroup.avatarUrl || ''" alt="" />
                <span v-else>{{ initials(activeGroup.groupName) }}</span>
              </span>
              <button
                v-if="editingProfile"
                type="button"
                class="secondary-btn compact"
                :disabled="savingProfile"
                @click="groupAvatarInput?.click()"
              >
                Avatar
              </button>
              <input
                ref="groupAvatarInput"
                class="hidden-file-input"
                type="file"
                accept="image/*"
                @change="handleGroupAvatarSelect"
              />
            </div>
            <template v-if="editingProfile">
              <input v-model="profileNameDraft" maxlength="30" placeholder="Group name" />
              <textarea v-model="profileDescriptionDraft" rows="3" maxlength="255" placeholder="Description"></textarea>
              <label class="inline-setting">
                <input v-model="profileChatEnabledDraft" type="checkbox" />
                Chat enabled
              </label>
              <input v-model.number="profileRecallLimitDraft" type="number" min="0" max="1440" placeholder="Recall minutes" />
              <div class="panel-actions split-actions">
                <button class="secondary-btn compact" :disabled="savingProfile" @click="cancelEditGroupProfile">Cancel</button>
                <button class="primary-btn compact" :disabled="savingProfile || !profileNameDraft.trim()" @click="saveGroupProfile">
                  {{ savingProfile ? 'Saving' : 'Save' }}
                </button>
              </div>
            </template>
            <template v-else>
              <strong>{{ activeGroup.groupName }}</strong>
              <p>{{ activeGroup.description || 'No description yet' }}</p>
              <p>Invite code: {{ activeGroup.inviteCode || '-' }}</p>
              <p>Chat: {{ activeGroup.chatEnabled === 1 ? 'Enabled' : 'Closed' }} | Recall: {{ activeGroup.recallLimitMinutes }} min</p>
              <div class="panel-actions split-actions">
                <button v-if="canManageGroup" class="secondary-btn compact" @click="startEditGroupProfile">Edit</button>
                <button v-if="!isOwner" class="secondary-btn compact danger-action" @click="leaveActiveGroup">Leave</button>
                <button v-else class="secondary-btn compact danger-action" @click="dissolveActiveGroup">Dissolve</button>
              </div>
            </template>
          </section>

          <section class="announcement-box">
            <h3>Announcement</h3>
            <div v-if="activeGroup.noticeUnread" class="notice-unread">Unread announcement</div>
            <p v-if="!editingNotice">{{ activeGroup.notice || 'No announcement yet' }}</p>
            <textarea v-else v-model="noticeDraft" rows="4" maxlength="1000"></textarea>
            <small v-if="noticeReadStats">{{ noticeReadStats.readCount }}/{{ noticeReadStats.memberCount }} read</small>
            <div class="panel-actions">
              <button class="secondary-btn compact" :disabled="savingNotice" @click="markNoticeRead">
                Mark read
              </button>
              <button v-if="!editingNotice && canManageGroup" class="secondary-btn compact" @click="startEditNotice">Edit</button>
              <button v-else class="primary-btn compact" :disabled="savingNotice" @click="saveNotice">
                {{ savingNotice ? 'Saving' : 'Save' }}
              </button>
            </div>
          </section>

          <section class="invite-box">
            <h3>Files</h3>
            <button class="secondary-btn compact" :disabled="loadingFiles" @click="loadFiles">
              {{ loadingFiles ? 'Loading' : 'Load files' }}
            </button>
            <div v-if="groupFiles.length" class="file-list">
              <a v-for="file in groupFiles" :key="file.messageId" :href="file.content" target="_blank" rel="noreferrer">
                <strong>{{ file.fileName || fileNameFromUrl(file.content) }}</strong>
                <small>{{ fileMeta(file) }}</small>
              </a>
            </div>
          </section>

          <section class="invite-box" v-if="canManageGroup">
            <h3>Invite</h3>
            <div class="member-picker">
              <input v-model="inviteKeyword" placeholder="Search users" @keyup.enter.prevent="handleInviteSearch" />
              <button type="button" class="secondary-btn compact" :disabled="searchingInvites" @click="handleInviteSearch">
                {{ searchingInvites ? '...' : 'Search' }}
              </button>
            </div>
            <div v-if="inviteResults.length" class="user-results compact-results">
              <button v-for="user in inviteResults" :key="user.userId" type="button" @click="inviteUser(user.userId)">
                <span class="avatar">{{ initials(user.nickname || user.email) }}</span>
                <span>
                  <strong>{{ user.nickname || user.email }}</strong>
                  <small>{{ user.email }}</small>
                </span>
              </button>
            </div>
          </section>

          <section class="invite-box" v-if="canManageGroup">
            <h3>Join requests</h3>
            <button class="secondary-btn compact" :disabled="loadingJoinRequests" @click="loadJoinRequests">
              {{ loadingJoinRequests ? 'Loading' : 'Load requests' }}
            </button>
            <div v-if="joinRequests.length" class="member-list">
              <div v-for="request in joinRequests" :key="request.requestId" class="member-row">
                <span class="avatar">
                  <img v-if="request.requesterAvatarUrl" :src="request.requesterAvatarUrl" alt="" />
                  <span v-else>{{ initials(request.requesterNickname || request.requesterEmail) }}</span>
                </span>
                <span>
                  <strong>{{ request.requesterNickname || request.requesterEmail }}</strong>
                  <small>{{ request.message || 'No message' }}</small>
                </span>
                <span class="member-actions">
                  <button type="button" @click="reviewJoin(request, true)">Approve</button>
                  <button class="danger" type="button" @click="reviewJoin(request, false)">Reject</button>
                </span>
              </div>
            </div>
          </section>

          <section class="invite-box">
            <h3>Notifications</h3>
            <button class="secondary-btn compact" :disabled="loadingNotifications" @click="loadNotifications">
              {{ loadingNotifications ? 'Loading' : 'Load notices' }}
            </button>
            <div v-if="notifications.length" class="notification-list">
              <p v-for="item in notifications" :key="item.notificationId">
                <strong>{{ item.content }}</strong>
                <small>{{ formatTime(item.createdAt) }}</small>
              </p>
            </div>
          </section>

          <h3>Members</h3>
          <div class="member-list">
            <div v-for="member in members" :key="member.userId" class="member-row">
              <span class="avatar">
                <img v-if="member.avatarUrl" :src="member.avatarUrl" alt="" />
                <span v-else>{{ initials(member.nickname || member.email) }}</span>
              </span>
              <span>
                <template v-if="editingNickname && member.userId === currentUserId">
                  <input
                    v-model="nicknameDraft"
                    class="nickname-input"
                    maxlength="64"
                    placeholder="Group nickname"
                    @keyup.enter="saveMyNickname"
                  />
                </template>
                <strong v-else>{{ member.groupNickname || member.nickname || member.email }}</strong>
                <small>{{ memberStatus(member) }}</small>
              </span>
              <span v-if="member.userId === currentUserId" class="member-actions">
                <button v-if="!editingNickname" type="button" @click="startEditNickname(member)">Nickname</button>
                <template v-else>
                  <button type="button" :disabled="savingNickname" @click="saveMyNickname">
                    {{ savingNickname ? 'Saving' : 'Save' }}
                  </button>
                  <button type="button" :disabled="savingNickname" @click="cancelEditNickname">Cancel</button>
                </template>
              </span>
              <span v-if="canManageMember(member)" class="member-actions">
                <button v-if="canToggleAdmin(member)" type="button" @click="toggleAdmin(member)">
                  {{ member.role === 2 ? 'Unset admin' : 'Set admin' }}
                </button>
                <button type="button" @click="toggleMute(member)">
                  {{ isMuted(member) ? 'Unmute' : 'Mute' }}
                </button>
                <button v-if="canTransferOwner(member)" type="button" @click="transferOwnerTo(member)">Transfer</button>
                <button class="danger" type="button" @click="removeMemberFromGroup(member)">Remove</button>
              </span>
            </div>
          </div>
        </aside>

        <section class="message-column">
          <div class="message-search">
            <input
              v-model="messageKeyword"
              type="search"
              placeholder="Search messages in this group"
              @keyup.enter="handleMessageSearch"
            />
            <button class="secondary-btn compact" :disabled="searchingMessages" @click="handleMessageSearch">
              {{ searchingMessages ? '...' : 'Search' }}
            </button>
            <button v-if="messageKeyword" class="secondary-btn compact" @click="clearMessageSearch">Clear</button>
          </div>
          <div ref="messageListRef" class="message-list">
            <div v-if="loadingMessages" class="message-state">Loading messages...</div>
            <div v-else-if="!orderedMessages.length" class="message-state">
              {{ messageKeyword ? 'No matching messages' : 'Send the first group message' }}
            </div>
            <button
              v-if="!loadingMessages && hasMoreMessages"
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
                <strong v-if="message.senderId !== currentUserId" class="sender-name">{{ message.senderNickname }}</strong>
                <div v-if="message.replyPreview" class="reply-preview">{{ message.replyPreview }}</div>
                <template v-if="message.messageType === 'image'">
                  <button class="image-preview-button" @click="previewImageUrl = message.content">
                    <img class="message-image" :src="message.content" alt="group image" />
                  </button>
                </template>
                <template v-else-if="message.messageType === 'file'">
                  <a class="file-card" :href="message.content" target="_blank" rel="noreferrer">
                    <span class="file-icon">FILE</span>
                    <span class="file-info">
                      <strong>{{ message.fileName || fileNameFromUrl(message.content) }}</strong>
                      <small>{{ fileMeta(message) }}</small>
                    </span>
                  </a>
                </template>
                <p v-else>{{ message.content }}</p>
              </template>
              <footer>
                <span v-if="message.mentionAll" class="mention-tag">@All</span>
                <span v-if="message.mentionUserIds.length" class="mention-tag">@{{ message.mentionUserIds.length }}</span>
                {{ formatTime(message.sentAt) }}
                <button @click="replyTo(message)">Reply</button>
                <button
                  v-if="message.senderId === currentUserId && message.recallStatus === 0"
                  @click="handleRecall(message.messageId)"
                >
                  Recall
                </button>
                <button @click="handleDelete(message.messageId)">Delete</button>
              </footer>
            </article>
          </div>

          <form class="composer" @submit.prevent="handleSend">
            <div v-if="replyTarget" class="reply-compose">
              Replying: {{ replyTarget.content || replyTarget.fileName || replyTarget.messageType }}
              <button type="button" @click="replyTarget = null">x</button>
            </div>
            <textarea
              v-model="draft"
              maxlength="1000"
              rows="1"
              placeholder="Type a group message"
              @keydown.enter.exact.prevent="handleSend"
            ></textarea>
            <input ref="imageInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageSelect" />
            <input ref="fileInput" class="hidden-file-input" type="file" @change="handleFileSelect" />
            <button
              type="button"
              class="secondary-btn compact"
              :class="{ selected: mentionAllDraft }"
              :disabled="sending || !canMentionAll"
              @click="mentionAllDraft = !mentionAllDraft"
            >
              @All
            </button>
            <button type="button" class="secondary-btn compact" :disabled="sending" @click="chooseMentions">@Member</button>
            <button type="button" class="secondary-btn compact" :disabled="sending" @click="imageInput?.click()">Image</button>
            <button type="button" class="secondary-btn compact" :disabled="sending" @click="fileInput?.click()">File</button>
            <button class="primary-btn compact" :disabled="sending || !draft.trim()">
              {{ sending ? 'Sending' : 'Send' }}
            </button>
          </form>
        </section>
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
  addGroupMembers,
  approveGroupJoinRequest,
  createGroup,
  deleteGroupMessage,
  dissolveGroup,
  getGroupAnnouncementReadStats,
  joinGroupByInviteCode,
  listGroupJoinRequests,
  listGroupFiles,
  leaveGroup,
  listGroupMembers,
  listGroupMessages,
  listGroups,
  listGroupNotifications,
  listMyGroupJoinRequests,
  markGroupAnnouncementRead,
  markGroupAsRead,
  muteGroupMember,
  recallGroupMessage,
  rejectGroupJoinRequest,
  removeGroupMember,
  requestJoinGroup,
  searchGroups,
  searchGroupMessages,
  sendGroupMessage,
  sendGroupMentionAllMessage,
  setGroupAdmin,
  transferGroupOwner,
  unmuteGroupMember,
  unsetGroupAdmin,
  updateGroupAnnouncement,
  updateGroupProfile,
  updateMyGroupNickname,
  type GroupJoinRequest,
  type GroupMember,
  type GroupMessage,
  type GroupNoticeReadStats,
  type GroupNotification,
  type GroupSummary,
} from '../api/group'
import type { UserSearchItem } from '../api/chat'
import { listFriends, type Friend } from '../api/friend'
import { createChatSocket, type ChatSocketEvent } from '../api/chatSocket'
import { uploadFile, uploadImage } from '../api/file'

const groups = ref<GroupSummary[]>([])
const activeGroup = ref<GroupSummary | null>(null)
const members = ref<GroupMember[]>([])
const messages = ref<GroupMessage[]>([])
const groupResults = ref<GroupSummary[]>([])
const joinRequests = ref<GroupJoinRequest[]>([])
const myJoinRequests = ref<GroupJoinRequest[]>([])
const notifications = ref<GroupNotification[]>([])
const groupFiles = ref<GroupMessage[]>([])
const noticeReadStats = ref<GroupNoticeReadStats | null>(null)
const userResults = ref<UserSearchItem[]>([])
const inviteResults = ref<UserSearchItem[]>([])
const selectedMembers = ref<UserSearchItem[]>([])
const friends = ref<Friend[]>([])
const newGroupName = ref('')
const newGroupDescription = ref('')
const groupKeyword = ref('')
const inviteCodeDraft = ref('')
const userKeyword = ref('')
const inviteKeyword = ref('')
const draft = ref('')
const messageKeyword = ref('')
const noticeDraft = ref('')
const nicknameDraft = ref('')
const profileNameDraft = ref('')
const profileDescriptionDraft = ref('')
const profileAvatarDraft = ref('')
const profileAvatarPreview = ref('')
const profileChatEnabledDraft = ref(true)
const profileRecallLimitDraft = ref(2)
const mentionAllDraft = ref(false)
const mentionUserIdsDraft = ref<number[]>([])
const replyTarget = ref<GroupMessage | null>(null)
const notice = ref('')
const currentUserId = ref(0)
const loadingGroups = ref(false)
const loadingMessages = ref(false)
const loadingEarlierMessages = ref(false)
const searchingUsers = ref(false)
const searchingInvites = ref(false)
const searchingMessages = ref(false)
const searchingGroups = ref(false)
const loadingJoinRequests = ref(false)
const loadingMyJoinRequests = ref(false)
const loadingNotifications = ref(false)
const loadingFiles = ref(false)
const creatingGroup = ref(false)
const sending = ref(false)
const savingNotice = ref(false)
const savingProfile = ref(false)
const savingNickname = ref(false)
const editingNotice = ref(false)
const editingProfile = ref(false)
const editingNickname = ref(false)
const imageInput = ref<HTMLInputElement | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const groupAvatarInput = ref<HTMLInputElement | null>(null)
const messageListRef = ref<HTMLElement | null>(null)
const previewImageUrl = ref('')
const socket = ref<WebSocket | null>(null)
const messagePage = ref(1)
const messagePageSize = 30
const hasMoreMessages = ref(false)

const selectedMemberIds = computed(() => selectedMembers.value.map((member) => member.userId))
const canCreateGroup = computed(() => newGroupName.value.trim().length >= 3 && selectedMembers.value.length >= 1)
const orderedMessages = computed(() => [...messages.value].reverse())
const myMember = computed(() => members.value.find((member) => member.userId === currentUserId.value) ?? null)
const canManageGroup = computed(() => (myMember.value?.role ?? 0) >= 2)
const isOwner = computed(() => myMember.value?.role === 3)
const canMentionAll = computed(() => canManageGroup.value)

onMounted(async () => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    currentUserId.value = JSON.parse(authData).userId
  }
  connectSocket()
  await Promise.all([loadGroups(), loadFriends()])
})

onBeforeUnmount(() => {
  socket.value?.close()
})

function connectSocket() {
  socket.value?.close()
  socket.value = createChatSocket(handleSocketEvent)
}

async function handleSocketEvent(event: ChatSocketEvent) {
  if (event.eventType === 'group:message:new') {
    const message = event.payload as GroupMessage
    if (!activeGroup.value || message.groupId !== activeGroup.value.groupId) return
    if (messageKeyword.value.trim()) return
    if (!messages.value.some((item) => item.messageId === message.messageId)) {
      messages.value = [message, ...messages.value]
      await scrollMessagesToBottom()
      await markActiveGroupAsRead()
    }
    return
  }
  if (event.eventType === 'group:message:update') {
    const payload = event.payload as { groupId?: number; messageId?: number }
    if (!activeGroup.value || payload.groupId !== activeGroup.value.groupId) return
    if (messageKeyword.value.trim()) {
      await handleMessageSearch()
    } else {
      await loadMessages()
    }
    return
  }
  if (event.eventType === 'group:update') {
    const payload = event.payload as { eventType?: string; group?: GroupSummary }
    if (!payload.group) return
    if (payload.eventType === 'group:dissolved') {
      groups.value = groups.value.filter((group) => group.groupId !== payload.group?.groupId)
      if (activeGroup.value?.groupId === payload.group.groupId) {
        resetActiveGroup()
        notice.value = 'Group has been dissolved'
      }
      return
    }
    groups.value = groups.value.map((group) => (group.groupId === payload.group?.groupId ? payload.group : group))
    if (activeGroup.value?.groupId === payload.group.groupId) {
      activeGroup.value = payload.group
      await loadMembers()
    } else {
      await loadGroups()
    }
  }
}

async function loadGroups() {
  loadingGroups.value = true
  notice.value = ''
  try {
    groups.value = await listGroups()
    if (activeGroup.value) {
      activeGroup.value = groups.value.find((group) => group.groupId === activeGroup.value?.groupId) ?? activeGroup.value
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load groups'
  } finally {
    loadingGroups.value = false
  }
}

async function loadFriends() {
  try {
    friends.value = await listFriends()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load friends'
  }
}

async function selectGroup(group: GroupSummary) {
  activeGroup.value = group
  editingNotice.value = false
  editingProfile.value = false
  editingNickname.value = false
  noticeDraft.value = group.notice || ''
  profileAvatarPreview.value = ''
  mentionAllDraft.value = false
  mentionUserIdsDraft.value = []
  replyTarget.value = null
  joinRequests.value = []
  notifications.value = []
  groupFiles.value = []
  noticeReadStats.value = null
  await Promise.all([loadMembers(), loadMessages(), loadNotifications(), loadNoticeReadStats()])
  await markActiveGroupAsRead()
}

async function loadMembers() {
  if (!activeGroup.value) return
  try {
    members.value = await listGroupMembers(activeGroup.value.groupId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load members'
  }
}

async function loadMessages() {
  if (!activeGroup.value) return
  loadingMessages.value = true
  try {
    messageKeyword.value = ''
    messagePage.value = 1
    const page = await listGroupMessages(activeGroup.value.groupId, messagePage.value, messagePageSize)
    messages.value = page.list
    hasMoreMessages.value = page.hasMore
    await scrollMessagesToBottom()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to load messages'
  } finally {
    loadingMessages.value = false
  }
}

async function markActiveGroupAsRead() {
  if (!activeGroup.value) return
  try {
    await markGroupAsRead(activeGroup.value.groupId)
    activeGroup.value = { ...activeGroup.value, unreadCount: 0, mentionUnreadCount: 0 }
    groups.value = groups.value.map((group) =>
      group.groupId === activeGroup.value?.groupId ? { ...group, unreadCount: 0, mentionUnreadCount: 0 } : group,
    )
  } catch {
    // Read state is best effort and should not interrupt group chat.
  }
}

async function handleMessageSearch() {
  if (!activeGroup.value) return
  const keyword = messageKeyword.value.trim()
  if (!keyword) {
    await loadMessages()
    return
  }
  searchingMessages.value = true
  notice.value = ''
  try {
    const page = await searchGroupMessages(activeGroup.value.groupId, keyword)
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

async function loadEarlierMessages() {
  if (!activeGroup.value || loadingEarlierMessages.value || !hasMoreMessages.value || messageKeyword.value.trim()) return
  const element = messageListRef.value
  const previousHeight = element?.scrollHeight ?? 0
  loadingEarlierMessages.value = true
  try {
    const nextPage = messagePage.value + 1
    const page = await listGroupMessages(activeGroup.value.groupId, nextPage, messagePageSize)
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

async function handleUserSearch() {
  if (!userKeyword.value.trim()) return
  searchingUsers.value = true
  try {
    const keyword = userKeyword.value.trim().toLowerCase()
    userResults.value = friends.value
      .filter((friend) =>
        `${friend.remarkName || ''} ${friend.nickname || ''} ${friend.email || ''}`.toLowerCase().includes(keyword),
      )
      .map(friendToUserSearchItem)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'User search failed'
  } finally {
    searchingUsers.value = false
  }
}

async function handleGroupSearch() {
  if (!groupKeyword.value.trim()) return
  searchingGroups.value = true
  try {
    const page = await searchGroups(groupKeyword.value.trim())
    const myGroupIds = new Set(groups.value.map((group) => group.groupId))
    groupResults.value = page.list.filter((group) => !myGroupIds.has(group.groupId))
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Group search failed'
  } finally {
    searchingGroups.value = false
  }
}

async function requestJoin(group: GroupSummary) {
  const message = window.prompt(`Request to join ${group.groupName}`, '')
  if (message === null) return
  try {
    await requestJoinGroup(group.groupId, message)
    await loadMyJoinRequests()
    notice.value = 'Join request sent'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Join request failed'
  }
}

async function loadMyJoinRequests() {
  loadingMyJoinRequests.value = true
  try {
    myJoinRequests.value = await listMyGroupJoinRequests()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load my requests failed'
  } finally {
    loadingMyJoinRequests.value = false
  }
}

async function joinByInvite() {
  if (!inviteCodeDraft.value.trim()) return
  try {
    const group = await joinGroupByInviteCode(inviteCodeDraft.value.trim())
    inviteCodeDraft.value = ''
    await loadGroups()
    await selectGroup(group)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Join by invite failed'
  }
}

async function handleInviteSearch() {
  if (!inviteKeyword.value.trim()) return
  searchingInvites.value = true
  try {
    const memberIds = new Set(members.value.map((member) => member.userId))
    const keyword = inviteKeyword.value.trim().toLowerCase()
    inviteResults.value = friends.value
      .filter((friend) => !memberIds.has(friend.userId))
      .filter((friend) =>
        `${friend.remarkName || ''} ${friend.nickname || ''} ${friend.email || ''}`.toLowerCase().includes(keyword),
      )
      .map(friendToUserSearchItem)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Invite search failed'
  } finally {
    searchingInvites.value = false
  }
}

async function inviteUser(userId: number) {
  if (!activeGroup.value) return
  try {
    members.value = await addGroupMembers(activeGroup.value.groupId, [userId])
    inviteResults.value = inviteResults.value.filter((user) => user.userId !== userId)
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Invite failed'
  }
}

async function loadJoinRequests() {
  if (!activeGroup.value) return
  loadingJoinRequests.value = true
  try {
    joinRequests.value = await listGroupJoinRequests(activeGroup.value.groupId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load join requests failed'
  } finally {
    loadingJoinRequests.value = false
  }
}

async function reviewJoin(request: GroupJoinRequest, approve: boolean) {
  if (!activeGroup.value) return
  try {
    members.value = approve
      ? await approveGroupJoinRequest(activeGroup.value.groupId, request.requestId)
      : await rejectGroupJoinRequest(activeGroup.value.groupId, request.requestId)
    joinRequests.value = joinRequests.value.filter((item) => item.requestId !== request.requestId)
    await Promise.all([loadGroups(), loadNotifications()])
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Review join request failed'
  }
}

async function loadNotifications() {
  if (!activeGroup.value) return
  loadingNotifications.value = true
  try {
    const page = await listGroupNotifications(activeGroup.value.groupId)
    notifications.value = page.list
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load notifications failed'
  } finally {
    loadingNotifications.value = false
  }
}

async function loadFiles() {
  if (!activeGroup.value) return
  loadingFiles.value = true
  try {
    const page = await listGroupFiles(activeGroup.value.groupId)
    groupFiles.value = page.list
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load files failed'
  } finally {
    loadingFiles.value = false
  }
}

async function loadNoticeReadStats() {
  if (!activeGroup.value) return
  try {
    noticeReadStats.value = await getGroupAnnouncementReadStats(activeGroup.value.groupId)
  } catch {
    noticeReadStats.value = null
  }
}

function toggleMember(user: UserSearchItem) {
  if (selectedMemberIds.value.includes(user.userId)) {
    removeMember(user.userId)
    return
  }
  selectedMembers.value = [...selectedMembers.value, user]
}

function friendToUserSearchItem(friend: Friend): UserSearchItem {
  return {
    userId: friend.userId,
    email: friend.email,
    nickname: friend.remarkName || friend.nickname,
    avatarUrl: friend.avatarUrl,
  }
}

function removeMember(userId: number) {
  selectedMembers.value = selectedMembers.value.filter((member) => member.userId !== userId)
}

async function handleCreateGroup() {
  if (!canCreateGroup.value) return
  creatingGroup.value = true
  notice.value = ''
  try {
    const group = await createGroup(
      newGroupName.value.trim(),
      newGroupDescription.value.trim(),
      selectedMemberIds.value,
    )
    newGroupName.value = ''
    newGroupDescription.value = ''
    selectedMembers.value = []
    userResults.value = []
    userKeyword.value = ''
    await loadGroups()
    await selectGroup(group)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to create group'
  } finally {
    creatingGroup.value = false
  }
}

async function handleSend() {
  if (!activeGroup.value || !draft.value.trim()) return
  sending.value = true
  try {
    if (mentionAllDraft.value && canMentionAll.value) {
      await sendGroupMentionAllMessage(activeGroup.value.groupId, draft.value.trim(), replyTarget.value?.messageId)
    } else {
      await sendGroupMessage(
        activeGroup.value.groupId,
        draft.value.trim(),
        'text',
        undefined,
        false,
        replyTarget.value?.messageId,
        mentionUserIdsDraft.value,
      )
    }
    draft.value = ''
    mentionAllDraft.value = false
    mentionUserIdsDraft.value = []
    replyTarget.value = null
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Failed to send message'
  } finally {
    sending.value = false
  }
}

function replyTo(message: GroupMessage) {
  replyTarget.value = message
}

function chooseMentions() {
  const candidates = members.value.filter((member) => member.userId !== currentUserId.value)
  const rawValue = window.prompt(
    'Mention member IDs, separated by comma',
    candidates.map((member) => `${member.userId}:${member.groupNickname || member.nickname || member.email}`).join(', '),
  )
  if (!rawValue) return
  mentionUserIdsDraft.value = rawValue
    .split(',')
    .map((item) => Number(item.split(':')[0].trim()))
    .filter((value) => Number.isFinite(value) && value > 0)
  if (mentionUserIdsDraft.value.length) {
    draft.value = `${draft.value} ${mentionUserIdsDraft.value.map((id) => `@${memberName(id)}`).join(' ')}`.trim()
  }
}

async function handleImageSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!activeGroup.value || !file) return
  sending.value = true
  try {
    const result = await uploadImage(file, 'chat-image')
    await sendGroupMessage(activeGroup.value.groupId, '', 'image', result.fileId)
    if (imageInput.value) imageInput.value.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Image upload failed'
  } finally {
    sending.value = false
  }
}

async function handleFileSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!activeGroup.value || !file) return
  sending.value = true
  try {
    const result = await uploadFile(file, 'chat-file')
    await sendGroupMessage(activeGroup.value.groupId, result.fileUrl, 'file', result.fileId)
    if (fileInput.value) fileInput.value.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'File upload failed'
  } finally {
    sending.value = false
  }
}

async function handleRecall(messageId: number) {
  if (!activeGroup.value) return
  try {
    if (!window.confirm('Recall this message?')) return
    await recallGroupMessage(activeGroup.value.groupId, messageId)
    if (messageKeyword.value.trim()) {
      await handleMessageSearch()
    } else {
      await loadMessages()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Recall failed'
  }
}

async function handleDelete(messageId: number) {
  if (!activeGroup.value) return
  try {
    if (!window.confirm('Delete this message from the group view?')) return
    await deleteGroupMessage(activeGroup.value.groupId, messageId)
    if (messageKeyword.value.trim()) {
      await handleMessageSearch()
    } else {
      await loadMessages()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Delete failed'
  }
}

function startEditNotice() {
  noticeDraft.value = activeGroup.value?.notice || ''
  editingNotice.value = true
}

async function saveNotice() {
  if (!activeGroup.value) return
  savingNotice.value = true
  try {
    activeGroup.value = await updateGroupAnnouncement(activeGroup.value.groupId, noticeDraft.value)
    editingNotice.value = false
    await Promise.all([loadGroups(), loadNoticeReadStats()])
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Save announcement failed'
  } finally {
    savingNotice.value = false
  }
}

async function markNoticeRead() {
  if (!activeGroup.value) return
  try {
    noticeReadStats.value = await markGroupAnnouncementRead(activeGroup.value.groupId)
    activeGroup.value = { ...activeGroup.value, noticeUnread: false }
    groups.value = groups.value.map((group) =>
      group.groupId === activeGroup.value?.groupId ? { ...group, noticeUnread: false } : group,
    )
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Mark announcement failed'
  }
}

function startEditGroupProfile() {
  if (!activeGroup.value) return
  profileNameDraft.value = activeGroup.value.groupName
  profileDescriptionDraft.value = activeGroup.value.description || ''
  profileAvatarDraft.value = activeGroup.value.avatarUrl || ''
  profileAvatarPreview.value = activeGroup.value.avatarUrl || ''
  profileChatEnabledDraft.value = activeGroup.value.chatEnabled === 1
  profileRecallLimitDraft.value = activeGroup.value.recallLimitMinutes ?? 2
  editingProfile.value = true
}

function cancelEditGroupProfile() {
  editingProfile.value = false
  profileAvatarPreview.value = ''
  if (groupAvatarInput.value) groupAvatarInput.value.value = ''
}

async function handleGroupAvatarSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!activeGroup.value || !file) return
  savingProfile.value = true
  try {
    const result = await uploadImage(file, 'group-avatar')
    profileAvatarDraft.value = result.fileUrl
    profileAvatarPreview.value = result.fileUrl
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Group avatar upload failed'
  } finally {
    savingProfile.value = false
  }
}

async function saveGroupProfile() {
  if (!activeGroup.value || !profileNameDraft.value.trim()) return
  savingProfile.value = true
  try {
    activeGroup.value = await updateGroupProfile(activeGroup.value.groupId, {
      groupName: profileNameDraft.value.trim(),
      description: profileDescriptionDraft.value.trim(),
      avatarUrl: profileAvatarDraft.value,
      chatEnabled: profileChatEnabledDraft.value,
      recallLimitMinutes: profileRecallLimitDraft.value,
    })
    editingProfile.value = false
    profileAvatarPreview.value = ''
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Save group profile failed'
  } finally {
    savingProfile.value = false
  }
}

async function leaveActiveGroup() {
  if (!activeGroup.value) return
  if (!window.confirm(`Leave ${activeGroup.value.groupName}?`)) return
  try {
    await leaveGroup(activeGroup.value.groupId)
    resetActiveGroup()
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Leave group failed'
  }
}

async function dissolveActiveGroup() {
  if (!activeGroup.value) return
  if (!window.confirm(`Dissolve ${activeGroup.value.groupName}? This cannot be undone.`)) return
  try {
    await dissolveGroup(activeGroup.value.groupId)
    resetActiveGroup()
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Dissolve group failed'
  }
}

async function transferOwnerTo(member: GroupMember) {
  if (!activeGroup.value) return
  if (!window.confirm(`Transfer group owner to ${member.nickname || member.email}?`)) return
  try {
    members.value = await transferGroupOwner(activeGroup.value.groupId, member.userId)
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Transfer owner failed'
  }
}

function startEditNickname(member: GroupMember) {
  nicknameDraft.value = member.groupNickname || ''
  editingNickname.value = true
}

function cancelEditNickname() {
  nicknameDraft.value = ''
  editingNickname.value = false
}

async function saveMyNickname() {
  if (!activeGroup.value) return
  savingNickname.value = true
  try {
    members.value = await updateMyGroupNickname(activeGroup.value.groupId, nicknameDraft.value.trim())
    editingNickname.value = false
    nicknameDraft.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Save nickname failed'
  } finally {
    savingNickname.value = false
  }
}

function canManageMember(member: GroupMember) {
  if (!canManageGroup.value || member.userId === currentUserId.value || member.role === 3) return false
  if (myMember.value?.role === 2 && member.role >= 2) return false
  return true
}

function canToggleAdmin(member: GroupMember) {
  return myMember.value?.role === 3 && member.role !== 3
}

function canTransferOwner(member: GroupMember) {
  return isOwner.value && member.userId !== currentUserId.value && member.role !== 3
}

async function toggleAdmin(member: GroupMember) {
  if (!activeGroup.value) return
  try {
    members.value = member.role === 2
      ? await unsetGroupAdmin(activeGroup.value.groupId, member.userId)
      : await setGroupAdmin(activeGroup.value.groupId, member.userId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Admin operation failed'
  }
}

function isMuted(member: GroupMember) {
  return Boolean(member.muteUntil && new Date(member.muteUntil).getTime() > Date.now())
}

function memberStatus(member: GroupMember) {
  const status = [roleLabel(member.role)]
  if (isMuted(member)) {
    status.push(`Muted until ${formatTime(member.muteUntil || '')}`)
  }
  return status.join(' | ')
}

async function toggleMute(member: GroupMember) {
  if (!activeGroup.value) return
  try {
    if (isMuted(member)) {
      members.value = await unmuteGroupMember(activeGroup.value.groupId, member.userId)
      return
    }
    const rawMinutes = window.prompt('Mute minutes', '10')
    if (!rawMinutes) return
    const minutes = Number(rawMinutes)
    if (!Number.isFinite(minutes) || minutes < 1) {
      notice.value = 'Mute minutes invalid'
      return
    }
    members.value = await muteGroupMember(activeGroup.value.groupId, member.userId, minutes)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Mute operation failed'
  }
}

async function removeMemberFromGroup(member: GroupMember) {
  if (!activeGroup.value) return
  if (!window.confirm(`Remove ${member.nickname || member.email} from this group?`)) return
  try {
    await removeGroupMember(activeGroup.value.groupId, member.userId)
    await Promise.all([loadMembers(), loadGroups()])
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Remove member failed'
  }
}

function roleLabel(role: number) {
  if (role === 3) return 'Owner'
  if (role === 2) return 'Admin'
  return 'Member'
}

function memberName(userId: number) {
  const member = members.value.find((item) => item.userId === userId)
  return member?.groupNickname || member?.nickname || member?.email || String(userId)
}

function joinRequestStatus(status: number) {
  if (status === 1) return 'Approved'
  if (status === 2) return 'Rejected'
  return 'Pending'
}

function initials(value: string) {
  return value.slice(0, 2).toUpperCase()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function fileNameFromUrl(url: string) {
  try {
    const pathname = new URL(url).pathname
    const name = pathname.split('/').pop()
    return name ? decodeURIComponent(name) : 'Attachment'
  } catch {
    return url.split('/').pop() || 'Attachment'
  }
}

function fileMeta(message: GroupMessage) {
  const parts = []
  if (message.fileMimeType) parts.push(message.fileMimeType)
  if (message.fileSize != null) parts.push(formatFileSize(message.fileSize))
  return parts.length ? parts.join(' | ') : 'Open attachment'
}

function formatFileSize(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

async function scrollMessagesToBottom() {
  await nextTick()
  const element = messageListRef.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}

function resetActiveGroup() {
  activeGroup.value = null
  members.value = []
  messages.value = []
  joinRequests.value = []
  notifications.value = []
  editingNotice.value = false
  editingProfile.value = false
  editingNickname.value = false
  noticeDraft.value = ''
  messageKeyword.value = ''
  nicknameDraft.value = ''
  profileAvatarPreview.value = ''
  mentionAllDraft.value = false
  mentionUserIdsDraft.value = []
  replyTarget.value = null
}
</script>

<style scoped>
.group-workspace {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  min-height: calc(100vh - 64px);
  max-width: 1220px;
  background: #fff;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
}

.group-sidebar {
  border-right: 1px solid #d8e0ea;
  background: #f7f9fc;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.group-header,
.chat-header {
  min-height: 76px;
  padding: 16px;
  border-bottom: 1px solid #d8e0ea;
  display: flex;
  align-items: center;
  gap: 12px;
}

.group-header {
  justify-content: space-between;
}

.group-header h2,
.chat-header h2 {
  margin: 0;
  font-size: 18px;
}

.group-header p,
.chat-header p {
  margin: 4px 0 0;
  color: #53627d;
  font-size: 13px;
}

.create-group,
.discover-group {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #d8e0ea;
}

.create-group input,
.create-group textarea,
.member-picker input,
.composer textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.member-picker {
  display: flex;
  gap: 8px;
}

.user-results,
.group-list,
.member-list {
  display: grid;
  gap: 4px;
}

.user-results button,
.group-item,
.member-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  border: 0;
  border-radius: 6px;
  padding: 10px;
  background: transparent;
  text-align: left;
}

.user-results button,
.group-item {
  cursor: pointer;
  position: relative;
}

.user-results button:hover,
.group-item:hover,
.group-item.active {
  background: #e9eef6;
}

.selected-members {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.selected-members span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background: #e9eef6;
  padding: 6px 10px;
  font-size: 12px;
}

.request-status-list {
  display: grid;
  gap: 6px;
}

.request-status-list p {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-radius: 6px;
  background: #fff;
  border: 1px solid #d8e0ea;
  padding: 8px 10px;
}

.notice-unread {
  border-radius: 6px;
  background: #f2f7ff;
  border: 1px solid #4f8cff;
  color: #2457c5;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 700;
}

.file-list {
  display: grid;
  gap: 8px;
}

.file-list a {
  display: grid;
  gap: 4px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #172033;
  padding: 10px;
}

.file-list small {
  color: #53627d;
}

.selected-members button {
  border: 0;
  background: transparent;
  color: #2457c5;
  cursor: pointer;
}

.group-list {
  padding: 8px;
  overflow: auto;
}

.group-item span:last-child,
.member-row span:last-child,
.user-results span:last-child {
  display: grid;
  min-width: 0;
}

.group-item strong,
.member-row strong,
.user-results strong,
.group-item small,
.member-row small,
.user-results small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-item small,
.member-row small,
.user-results small {
  color: #53627d;
}

.unread-badge,
.mention-badge {
  margin-left: auto;
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
}

.unread-badge {
  background: #ef4444;
}

.mention-badge {
  background: #2457c5;
}

.group-panel {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.empty-group,
.empty-list,
.message-state {
  margin: auto;
  color: #748198;
  text-align: center;
}

.empty-list {
  padding: 20px 12px;
  font-size: 13px;
}

.group-chat {
  min-height: 0;
  flex: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
}

.member-panel {
  grid-column: 2;
  grid-row: 1;
  border-left: 1px solid #d8e0ea;
  background: #fbfcfe;
  padding: 14px;
  overflow: auto;
}

.profile-box,
.announcement-box,
.invite-box {
  display: grid;
  gap: 8px;
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid #d8e0ea;
}

.member-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
}

.profile-box p,
.announcement-box p {
  margin: 0;
  color: #53627d;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.profile-box input,
.profile-box textarea,
.announcement-box textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.profile-box textarea,
.announcement-box textarea {
  resize: vertical;
}

.inline-setting {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #53627d;
  font-size: 13px;
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.large-avatar {
  width: 48px;
  height: 48px;
}

.panel-actions {
  display: flex;
  justify-content: flex-end;
}

.split-actions {
  justify-content: space-between;
  gap: 8px;
}

.danger-action {
  background: #fee2e2;
  color: #9b2626;
}

.compact-results {
  max-height: 180px;
  overflow: auto;
}

.member-row {
  position: relative;
}

.member-actions {
  margin-left: auto;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.member-actions button {
  border: 0;
  border-radius: 6px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 6px 8px;
  font-size: 12px;
}

.member-actions .danger {
  margin-left: auto;
  background: #fee2e2;
  color: #9b2626;
}

.nickname-input {
  width: 100%;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 7px 8px;
  background: #fff;
}

.notification-list {
  display: grid;
  gap: 8px;
}

.notification-list p {
  margin: 0;
  display: grid;
  gap: 2px;
  color: #53627d;
}

.notification-list small {
  color: #748198;
}

.message-column {
  grid-column: 1;
  grid-row: 1;
  min-width: 0;
  min-height: 0;
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

.sender-name {
  display: block;
  margin-bottom: 6px;
  color: #2457c5;
  font-size: 12px;
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-bubble footer {
  margin-top: 8px;
  text-align: right;
  color: #53627d;
  font-size: 12px;
}

.message-bubble footer button {
  margin-left: 6px;
  border: 0;
  background: transparent;
  color: #2457c5;
  cursor: pointer;
  font-weight: 700;
}

.recalled {
  color: #53627d;
  font-style: italic;
}

.mention-tag {
  display: inline-flex;
  margin-right: 6px;
  border-radius: 6px;
  background: #dbe7ff;
  color: #2457c5;
  padding: 2px 6px;
  font-weight: 800;
}

.reply-preview,
.reply-compose {
  border-left: 3px solid #4f8cff;
  background: #f7f9fc;
  color: #53627d;
  padding: 6px 8px;
  margin-bottom: 8px;
  border-radius: 6px;
  font-size: 12px;
}

.reply-compose {
  flex-basis: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reply-compose button {
  border: 0;
  background: transparent;
  color: #2457c5;
  cursor: pointer;
  font-weight: 800;
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

.file-info small {
  color: #53627d;
  font-size: 12px;
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
  min-height: 42px;
  max-height: 132px;
  resize: vertical;
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

.icon-button {
  border: 0;
  border-radius: 6px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
}

.compact {
  padding: 10px 14px;
  border-radius: 6px;
}

.compact.selected {
  background: #dbe7ff;
  color: #2457c5;
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
}

@media (max-width: 980px) {
  .group-workspace {
    grid-template-columns: 1fr;
  }

  .group-sidebar {
    max-height: 460px;
    border-right: 0;
    border-bottom: 1px solid #d8e0ea;
  }

  .group-chat {
    grid-template-columns: 1fr;
  }

  .member-panel {
    grid-column: 1;
    grid-row: 1;
    max-height: 180px;
    border-left: 0;
    border-bottom: 1px solid #d8e0ea;
  }

  .message-column {
    grid-row: 2;
  }

  .composer {
    flex-wrap: wrap;
  }

  .composer textarea {
    flex-basis: 100%;
  }
}
</style>
