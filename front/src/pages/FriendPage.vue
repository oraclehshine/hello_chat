<template>
  <section class="friend-workspace">
    <aside class="friend-sidebar">
      <header class="friend-header">
        <div>
          <h2>Friends</h2>
          <p>Requests, contacts, and blocks</p>
        </div>
        <button class="icon-button" :disabled="loading" @click="loadAll">Refresh</button>
      </header>

      <section class="friend-card">
        <h3>Presence</h3>
        <select v-model="presenceStatus" @change="() => savePresence()">
          <option value="online">Online</option>
          <option value="offline">Offline</option>
          <option value="busy">Busy</option>
          <option value="invisible">Invisible</option>
        </select>
        <small v-if="presence">Last active {{ formatTime(presence.lastActiveAt) }}</small>
      </section>

      <section class="friend-card">
        <h3>Social search</h3>
        <div class="search-row">
          <input v-model="keyword" type="search" placeholder="Search users or groups" @keyup.enter="handleSearch" />
          <button class="secondary-btn compact" :disabled="searching" @click="handleSearch">
            {{ searching ? '...' : 'Search' }}
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
            <p>{{ group.description || `${group.memberCount} members` }}</p>
            <button @click="requestGroup(group.groupId)">Request join</button>
          </article>
        </div>
      </section>

      <section class="friend-card">
        <h3>Recommendations</h3>
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
        <h3>Received requests</h3>
        <div v-if="!receivedRequests.length" class="empty-state">No pending requests</div>
        <div v-else class="request-list">
          <article v-for="request in receivedRequests" :key="request.requestId">
            <strong>{{ request.requesterNickname || request.requesterEmail }}</strong>
            <p>{{ request.remark || 'No message' }}</p>
            <div class="row-actions">
              <button @click="approveRequest(request)">Approve</button>
              <button class="danger" @click="rejectRequest(request)">Reject</button>
            </div>
          </article>
        </div>
      </section>

      <section class="friend-card">
        <h3>Sent requests</h3>
        <div v-if="!sentRequests.length" class="empty-state">No sent requests</div>
        <div v-else class="request-list">
          <article v-for="request in sentRequests" :key="request.requestId">
            <strong>{{ request.receiverNickname || request.receiverEmail }}</strong>
            <p>{{ requestStatus(request.status) }}</p>
          </article>
        </div>
      </section>
    </aside>

    <main class="friend-main">
      <header class="section-header">
        <div>
          <h2>My friends</h2>
          <p>{{ friends.length }} contacts</p>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>
      <div v-if="loading" class="empty-state large">Loading friends...</div>
      <div v-else-if="!friends.length" class="empty-state large">No friends yet</div>

      <div v-else class="friend-grid">
        <article v-for="friend in sortedFriends" :key="friend.userId" class="friend-item">
          <span class="avatar large">
            <img v-if="friend.avatarUrl" :src="friend.avatarUrl" alt="" />
            <span v-else>{{ initials(displayName(friend)) }}</span>
          </span>
          <div class="friend-info">
            <h3>{{ displayName(friend) }}</h3>
            <p>{{ friend.email }}</p>
            <small>{{ friend.friendGroup || 'Default' }} {{ friend.star ? '| Starred' : '' }}</small>
          </div>
          <div class="friend-actions">
            <button @click="editFriend(friend)">Edit</button>
            <button @click="toggleStar(friend)">{{ friend.star ? 'Unstar' : 'Star' }}</button>
            <button @click="blockFriend(friend)">Block</button>
            <button class="danger" @click="removeFriend(friend)">Delete</button>
          </div>
        </article>
      </div>

      <section class="blocked-panel">
        <h2>Blocked users</h2>
        <div v-if="!blockedUsers.length" class="empty-state">No blocked users</div>
        <div v-else class="blocked-list">
          <article v-for="user in blockedUsers" :key="user.userId">
            <span>{{ user.nickname || user.email }}</span>
            <button @click="unblock(user.userId)">Unblock</button>
          </article>
        </div>
      </section>
    </main>
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
    notice.value = error instanceof Error ? error.message : 'Load friends failed'
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
    notice.value = error instanceof Error ? error.message : 'Search failed'
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
  const remark = window.prompt(`Send friend request to ${user.nickname || user.email}`, '')
  if (remark === null) return
  try {
    await sendFriendRequest(user.userId, remark)
    searchResults.value = searchResults.value.filter((item) => item.userId !== user.userId)
    sentRequests.value = await listSentFriendRequests()
    notice.value = 'Friend request sent'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Send request failed'
  }
}

async function requestGroup(groupId: number) {
  const message = window.prompt('Join request message', '')
  if (message === null) return
  try {
    await requestJoinGroup(groupId, message)
    groupResults.value = groupResults.value.filter((group) => group.groupId !== groupId)
    recommendedGroups.value = recommendedGroups.value.filter((group) => group.groupId !== groupId)
    notice.value = 'Group join request sent'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Join request failed'
  }
}

async function savePresence(showNotice = true) {
  try {
    presence.value = await updatePresence(presenceStatus.value)
    if (showNotice) notice.value = 'Presence updated'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Presence update failed'
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
    notice.value = error instanceof Error ? error.message : 'Approve failed'
  }
}

async function rejectRequest(request: FriendRequest) {
  try {
    await rejectFriendRequest(request.requestId)
    receivedRequests.value = receivedRequests.value.filter((item) => item.requestId !== request.requestId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Reject failed'
  }
}

async function editFriend(friend: Friend) {
  const remarkName = window.prompt('Remark name', friend.remarkName || '')
  if (remarkName === null) return
  const friendGroup = window.prompt('Friend group', friend.friendGroup || 'Default')
  if (friendGroup === null) return
  try {
    replaceFriend(await updateFriend(friend.userId, { remarkName, friendGroup }))
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Update friend failed'
  }
}

async function toggleStar(friend: Friend) {
  try {
    replaceFriend(await updateFriend(friend.userId, { star: friend.star !== 1 }))
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Star friend failed'
  }
}

async function removeFriend(friend: Friend) {
  if (!window.confirm(`Delete ${displayName(friend)} from friends?`)) return
  try {
    await deleteFriend(friend.userId)
    friends.value = friends.value.filter((item) => item.userId !== friend.userId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Delete friend failed'
  }
}

async function blockFriend(friend: Friend) {
  if (!window.confirm(`Block ${displayName(friend)}?`)) return
  try {
    await blockUser(friend.userId)
    await loadAll()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Block user failed'
  }
}

async function unblock(userId: number) {
  try {
    await unblockUser(userId)
    blockedUsers.value = blockedUsers.value.filter((user) => user.userId !== userId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Unblock failed'
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
  if (status === 2) return 'Approved'
  if (status === 3) return 'Rejected'
  return 'Pending'
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.friend-workspace {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  min-height: calc(100vh - 64px);
  max-width: 1220px;
  background: #fff;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
}

.friend-sidebar {
  border-right: 1px solid #d8e0ea;
  background: #f7f9fc;
  padding: 16px;
  display: grid;
  align-content: start;
  gap: 14px;
}

.friend-header,
.section-header,
.search-row,
.row-actions,
.friend-actions,
.blocked-list article {
  display: flex;
  align-items: center;
  gap: 10px;
}

.friend-header,
.section-header {
  justify-content: space-between;
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

.friend-card,
.blocked-panel {
  display: grid;
  gap: 10px;
}

.friend-card select {
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  padding: 10px 12px;
}

.search-row input {
  flex: 1;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
}

.user-list,
.request-list,
.blocked-list,
.history-list,
.topic-list {
  display: grid;
  gap: 8px;
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
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.user-list button {
  display: flex;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.friend-main {
  padding: 18px;
  min-width: 0;
  overflow: auto;
  display: grid;
  align-content: start;
  gap: 18px;
}

.friend-grid {
  display: grid;
  gap: 10px;
}

.friend-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
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
  flex-wrap: wrap;
  justify-content: flex-end;
}

button {
  border: 0;
  border-radius: 6px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
}

button.danger,
.danger {
  color: #9b2626;
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

@media (max-width: 900px) {
  .friend-workspace {
    grid-template-columns: 1fr;
  }

  .friend-sidebar {
    border-right: 0;
    border-bottom: 1px solid #d8e0ea;
  }

  .friend-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .friend-actions {
    grid-column: 1 / -1;
    justify-content: flex-start;
  }
}
</style>
