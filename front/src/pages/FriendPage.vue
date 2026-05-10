<template>
  <section class="friend-workspace">
    <header class="friend-topbar">
      <div>
        <h2>好友</h2>
        <p>{{ friends.length }} 位联系人，{{ receivedRequests.length }} 条待处理请求</p>
      </div>
      <div class="friend-top-actions">
        <label class="presence-inline">
          <span>在线状态</span>
          <select v-model="presenceStatus" @change="() => savePresence()">
            <option value="online">在线</option>
            <option value="offline">离线</option>
            <option value="busy">忙碌</option>
            <option value="invisible">隐身</option>
          </select>
        </label>
        <button class="secondary-btn compact" :disabled="loading" @click="loadAll">刷新</button>
        <button class="primary-btn compact request-entry" type="button" @click="showFriendTools = true">
          添加/请求
          <span v-if="receivedRequests.length" class="request-dot">{{ receivedRequests.length }}</span>
        </button>
      </div>
    </header>

    <div v-if="notice" class="notice error">{{ notice }}</div>

    <main class="friend-main-grid">
      <section class="friend-panel contacts-panel">
        <header class="section-header">
          <div>
            <h3>我的好友</h3>
            <p>{{ presence ? `最后活跃 ${formatTime(presence.lastActiveAt)}` : '好友列表和常用操作' }}</p>
          </div>
        </header>

        <div v-if="loading" class="empty-state large">好友加载中...</div>
        <div v-else-if="!friends.length" class="empty-state large">暂无好友</div>
        <div v-else class="friend-grid">
          <article v-for="friend in sortedFriends" :key="friend.userId" class="friend-item">
            <span class="avatar large">
              <img v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="" />
              <span v-else>{{ initials(displayName(friend)) }}</span>
            </span>
            <div class="friend-info">
              <h3>{{ displayName(friend) }}</h3>
              <p>{{ friend.email }}</p>
              <small>{{ friend.friendGroup || '默认分组' }} {{ friend.star ? '| 星标' : '' }}</small>
            </div>
            <div class="friend-actions">
              <button @click="editFriend(friend)">编辑</button>
              <button @click="toggleStar(friend)">{{ friend.star ? '取消星标' : '星标' }}</button>
              <button @click="blockFriend(friend)">拉黑</button>
              <button class="danger" @click="removeFriend(friend)">删除</button>
            </div>
          </article>
        </div>
      </section>

      <section class="friend-panel blocked-panel">
        <header class="section-header">
          <div>
            <h3>黑名单</h3>
            <p>{{ blockedUsers.length }} 位用户</p>
          </div>
        </header>
        <div v-if="!blockedUsers.length" class="empty-state">暂无黑名单用户</div>
        <div v-else class="blocked-list">
          <article v-for="user in blockedUsers" :key="user.userId">
            <span>{{ user.nickname || user.email }}</span>
            <button @click="unblock(user.userId)">解除拉黑</button>
          </article>
        </div>
      </section>
    </main>

    <div v-if="showFriendTools" class="tool-overlay" @click.self="showFriendTools = false">
      <aside class="friend-tool-drawer">
        <header class="drawer-header">
          <div>
            <h2>添加好友与请求</h2>
            <p>搜索陌生人、加入群组、处理好友请求</p>
          </div>
          <button class="secondary-btn compact" type="button" @click="showFriendTools = false">关闭</button>
        </header>

        <section class="friend-card">
          <h3>搜索好友/群组</h3>
          <div class="search-row">
            <input v-model="keyword" type="search" placeholder="搜索用户或群组" @keyup.enter="handleSearch" />
            <button class="secondary-btn compact" :disabled="searching" @click="handleSearch">
              {{ searching ? '...' : '搜索' }}
            </button>
          </div>
          <div v-if="searchHistory.length" class="history-list">
            <button v-for="item in searchHistory" :key="item.historyId" type="button" @click="searchFromHistory(item.keyword)">
              {{ item.keyword }}
            </button>
          </div>
          <div v-if="searchResults.length" class="user-list">
            <button v-for="user in searchResults" :key="user.userId" type="button" @click="requestFriend(user)">
              <span class="avatar">
                <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
                <span v-else>{{ initials(user.nickname || user.email) }}</span>
              </span>
              <span>
                <strong>{{ user.nickname || user.email }}</strong>
                <small>{{ user.email }}</small>
              </span>
            </button>
          </div>
          <div v-if="groupResults.length" class="request-list">
            <article v-for="group in groupResults" :key="group.groupId">
              <strong>{{ group.groupName }}</strong>
              <p>{{ group.description || `${group.memberCount} 名成员` }}</p>
              <button @click="requestGroup(group.groupId)">申请加入</button>
            </article>
          </div>
        </section>

        <section class="friend-card">
          <h3>推荐</h3>
          <div v-if="recommendedFriends.length" class="user-list">
            <button v-for="user in recommendedFriends" :key="user.userId" type="button" @click="requestFriend(user)">
              <span class="avatar">
                <img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" />
                <span v-else>{{ initials(user.nickname || user.email) }}</span>
              </span>
              <span>
                <strong>{{ user.nickname || user.email }}</strong>
                <small>{{ user.email }}</small>
              </span>
            </button>
          </div>
          <div v-if="recommendedGroups.length" class="topic-list">
            <button v-for="group in recommendedGroups" :key="group.groupId" type="button" @click="requestGroup(group.groupId)">
              {{ group.groupName }}
            </button>
          </div>
          <div v-if="recommendedTopics.length" class="topic-list">
            <button v-for="topic in recommendedTopics" :key="topic.tag" type="button" @click="searchTopic(topic.tag)">
              #{{ topic.tag }} {{ topic.momentCount }}
            </button>
          </div>
        </section>

        <section class="friend-card">
          <h3>收到的请求</h3>
          <div v-if="!receivedRequests.length" class="empty-state">暂无待处理请求</div>
          <div v-else class="request-list">
            <article v-for="request in receivedRequests" :key="request.requestId">
              <strong>{{ request.requesterNickname || request.requesterEmail }}</strong>
              <p>{{ request.remark || '无附言' }}</p>
              <div class="row-actions">
                <button @click="approveRequest(request)">同意</button>
                <button class="danger" @click="rejectRequest(request)">拒绝</button>
              </div>
            </article>
          </div>
        </section>

        <section class="friend-card">
          <h3>已发送请求</h3>
          <div v-if="!sentRequests.length" class="empty-state">暂无已发送请求</div>
          <div v-else class="request-list">
            <article v-for="request in sentRequests" :key="request.requestId">
              <strong>{{ request.receiverNickname || request.receiverEmail }}</strong>
              <p>{{ requestStatus(request.status) }}</p>
            </article>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { UserSearchItem } from '../api/chat'
import { requestJoinGroup, type GroupSummary } from '../api/group'
import {
  approveFriendRequest,
  blockUser,
  deleteFriend,
  listBlockedUsers,
  listFriends,
  listReceivedFriendRequests,
  listSentFriendRequests,
  rejectFriendRequest,
  sendFriendRequest,
  unblockUser,
  updateFriend,
  type BlockedUser,
  type Friend,
  type FriendRequest,
} from '../api/friend'
import {
  listRecommendations,
  listSearchHistory,
  socialSearch,
  updatePresence,
  type Presence,
  type SearchHistory,
  type TopicRecommendation,
} from '../api/social'

const friends = ref<Friend[]>([])
const receivedRequests = ref<FriendRequest[]>([])
const sentRequests = ref<FriendRequest[]>([])
const blockedUsers = ref<BlockedUser[]>([])
const searchResults = ref<UserSearchItem[]>([])
const groupResults = ref<GroupSummary[]>([])
const searchHistory = ref<SearchHistory[]>([])
const recommendedFriends = ref<UserSearchItem[]>([])
const recommendedGroups = ref<GroupSummary[]>([])
const recommendedTopics = ref<TopicRecommendation[]>([])
const presence = ref<Presence | null>(null)
const presenceStatus = ref<Presence['status']>('online')
const keyword = ref('')
const notice = ref('')
const loading = ref(false)
const searching = ref(false)
const currentUserId = ref(0)
const showFriendTools = ref(false)

const sortedFriends = computed(() => [...friends.value].sort((left, right) => right.star - left.star))

onMounted(async () => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    currentUserId.value = JSON.parse(authData).userId
  }
  await loadAll()
})

async function loadAll() {
  loading.value = true
  notice.value = ''
  try {
    const [friendList, received, sent, blocks] = await Promise.all([
      listFriends(),
      listReceivedFriendRequests(),
      listSentFriendRequests(),
      listBlockedUsers(),
    ])
    friends.value = friendList
    receivedRequests.value = received
    sentRequests.value = sent
    blockedUsers.value = blocks
    await Promise.all([loadSearchHistory(), loadRecommendations(), savePresence(false)])
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载好友失败'
  } finally {
    loading.value = false
  }
}

async function handleSearch() {
  if (!keyword.value.trim()) return
  searching.value = true
  notice.value = ''
  try {
    const friendIds = new Set(friends.value.map((friend) => friend.userId))
    const result = await socialSearch(keyword.value.trim())
    searchResults.value = result.users.filter(
      (user) => user.userId !== currentUserId.value && !friendIds.has(user.userId),
    )
    groupResults.value = result.groups
    searchHistory.value = await listSearchHistory()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '搜索失败'
  } finally {
    searching.value = false
  }
}

async function searchFromHistory(value: string) {
  keyword.value = value
  await handleSearch()
}

async function searchTopic(tag: string) {
  keyword.value = `#${tag}`
  await handleSearch()
}

async function requestFriend(user: UserSearchItem) {
  const remark = window.prompt(`向 ${user.nickname || user.email} 发送好友请求`, '')
  if (remark === null) return
  try {
    await sendFriendRequest(user.userId, remark)
    searchResults.value = searchResults.value.filter((item) => item.userId !== user.userId)
    sentRequests.value = await listSentFriendRequests()
    notice.value = '好友请求已发送'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '发送请求失败'
  }
}

async function requestGroup(groupId: number) {
  const message = window.prompt('入群申请说明', '')
  if (message === null) return
  try {
    await requestJoinGroup(groupId, message)
    groupResults.value = groupResults.value.filter((group) => group.groupId !== groupId)
    recommendedGroups.value = recommendedGroups.value.filter((group) => group.groupId !== groupId)
    notice.value = '入群申请已发送'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '入群申请失败'
  }
}

async function savePresence(showNotice = true) {
  try {
    presence.value = await updatePresence(presenceStatus.value)
    if (showNotice) notice.value = '在线状态已更新'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '在线状态更新失败'
  }
}

async function loadSearchHistory() {
  searchHistory.value = await listSearchHistory()
}

async function loadRecommendations() {
  const result = await listRecommendations()
  recommendedFriends.value = result.friends
  recommendedGroups.value = result.groups
  recommendedTopics.value = result.topics
}

async function approveRequest(request: FriendRequest) {
  try {
    await approveFriendRequest(request.requestId)
    await loadAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '同意请求失败'
  }
}

async function rejectRequest(request: FriendRequest) {
  try {
    await rejectFriendRequest(request.requestId)
    receivedRequests.value = receivedRequests.value.filter((item) => item.requestId !== request.requestId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '拒绝请求失败'
  }
}

async function editFriend(friend: Friend) {
  const remarkName = window.prompt('好友备注', friend.remarkName || '')
  if (remarkName === null) return
  const friendGroup = window.prompt('好友分组', friend.friendGroup || '默认分组')
  if (friendGroup === null) return
  try {
    replaceFriend(await updateFriend(friend.userId, { remarkName, friendGroup }))
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '更新好友失败'
  }
}

async function toggleStar(friend: Friend) {
  try {
    replaceFriend(await updateFriend(friend.userId, { star: friend.star !== 1 }))
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '星标好友失败'
  }
}

async function removeFriend(friend: Friend) {
  if (!window.confirm(`确认删除好友 ${displayName(friend)}？`)) return
  try {
    await deleteFriend(friend.userId)
    friends.value = friends.value.filter((item) => item.userId !== friend.userId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '删除好友失败'
  }
}

async function blockFriend(friend: Friend) {
  if (!window.confirm(`确认拉黑 ${displayName(friend)}？`)) return
  try {
    await blockUser(friend.userId)
    await loadAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '拉黑用户失败'
  }
}

async function unblock(userId: number) {
  try {
    await unblockUser(userId)
    blockedUsers.value = blockedUsers.value.filter((user) => user.userId !== userId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '解除拉黑失败'
  }
}

function replaceFriend(friend: Friend) {
  friends.value = friends.value.map((item) => (item.userId === friend.userId ? friend : item))
}

function displayName(friend: Friend) {
  return friend.remarkName || friend.nickname || friend.email
}

function initials(value: string) {
  return value.slice(0, 2).toUpperCase()
}

function requestStatus(status: number) {
  if (status === 2) return '已同意'
  if (status === 3) return '已拒绝'
  return '待处理'
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.friend-workspace {
  display: flex;
  overflow: hidden;
  border: 1px solid #d8e0ea;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
  flex-direction: column;
}

.friend-topbar,
.section-header,
.search-row,
.row-actions,
.friend-actions,
.blocked-list article,
.friend-top-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.friend-topbar,
.section-header {
  justify-content: space-between;
}

.friend-topbar {
  min-height: 74px;
  padding: 16px 20px;
  border-bottom: 1px solid #d8e0ea;
  background: #fff;
}

h2,
h3,
p {
  margin: 0;
}

p,
small,
.empty-state {
  color: #53627d;
}

.presence-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #53627d;
  font-size: 13px;
}

.presence-inline select,
.search-row input {
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 10px;
  background: #fff;
  padding: 9px 12px;
}

.friend-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 0;
  flex: 1;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.friend-panel {
  min-height: 0;
  border: 1px solid #d8e0ea;
  border-radius: 14px;
  background: #fff;
  padding: 16px;
  overflow: auto;
}

.contacts-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.friend-card {
  display: grid;
  gap: 10px;
}

.user-list,
.request-list,
.blocked-list,
.history-list,
.topic-list,
.friend-grid {
  display: grid;
  gap: 10px;
}

.friend-grid {
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
}

.history-list,
.topic-list {
  grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
}

.user-list button,
.request-list article,
.blocked-list article,
.friend-item {
  border: 1px solid #d8e0ea;
  border-radius: 12px;
  background: #fff;
  padding: 12px;
}

.user-list button {
  display: flex;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.friend-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.friend-info {
  min-width: 0;
}

.friend-info h3,
.friend-info p {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.friend-actions {
  grid-column: 1 / -1;
  flex-wrap: wrap;
  justify-content: flex-start;
}

button {
  border: 0;
  border-radius: 10px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
}

button.danger,
.danger {
  color: #9b2626;
}

.request-entry {
  position: relative;
}

.request-dot {
  display: inline-grid;
  min-width: 18px;
  height: 18px;
  margin-left: 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  place-items: center;
  font-size: 11px;
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

.avatar.large {
  width: 46px;
  height: 46px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.large.empty-state {
  padding: 46px;
  text-align: center;
}

.tool-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.18);
}

.friend-tool-drawer {
  display: grid;
  width: min(520px, 94vw);
  align-content: start;
  gap: 16px;
  overflow: auto;
  padding: 18px;
  background: #f7f9fc;
  box-shadow: -18px 0 48px rgba(15, 23, 42, 0.14);
}

.drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 980px) {
  .friend-main-grid {
    grid-template-columns: 1fr;
  }

  .friend-topbar,
  .friend-top-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
