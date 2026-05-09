<template>
  <section class="moments-workspace">
    <aside class="moments-side">
      <header>
        <h2>Moments</h2>
        <p>Phase 4 timeline</p>
      </header>

      <form class="composer-card" @submit.prevent="handleCreateMoment">
        <textarea v-model="draft" rows="5" maxlength="5000" placeholder="Share something"></textarea>
        <input v-model="locationDraft" placeholder="Location" />
        <input v-model="tagDraft" placeholder="Tags, comma separated" />
        <div class="composer-inline">
          <input v-model="moodDraft" maxlength="32" placeholder="Mood" />
          <input v-model="activityDraft" maxlength="32" placeholder="Activity" />
        </div>
        <select v-model="visibilityDraft">
          <option value="public">Public</option>
          <option value="friends">Friends</option>
          <option value="private">Private</option>
          <option value="specified">Specified</option>
        </select>
        <div v-if="visibilityDraft === 'specified'" class="friend-picker">
          <label v-for="friend in friends" :key="friend.userId">
            <input v-model="visibleUserIdsDraft" type="checkbox" :value="friend.userId" />
            <span>{{ friend.remarkName || friend.nickname || friend.email }}</span>
          </label>
          <small v-if="!friends.length">No friends available</small>
        </div>
        <input ref="mediaInput" class="hidden-file-input" type="file" accept="image/*,video/*" multiple @change="handleMediaSelect" />
        <div v-if="mediaDraft.length" class="media-draft">
          <span v-for="media in mediaDraft" :key="media.fileId">
            {{ media.fileName }}
            <button type="button" @click="removeMedia(media.fileId)">x</button>
          </span>
        </div>
        <div class="composer-actions">
          <button type="button" class="secondary-btn compact" :disabled="creating" @click="mediaInput?.click()">Media</button>
          <button class="primary-btn compact" :disabled="creating || !canPost">
            {{ creating ? 'Posting' : 'Post' }}
          </button>
        </div>
      </form>

      <section class="profile-card">
        <h3>{{ profileSummary?.nickname || userNickname || userEmail || 'Me' }}</h3>
        <p>{{ profileSummary?.email || userEmail }}</p>
        <div v-if="profileSummary" class="profile-stats">
          <span>Moments {{ profileSummary.momentCount }}</span>
          <span>Likes {{ profileSummary.totalLikeCount }}</span>
          <span>Comments {{ profileSummary.totalCommentCount }}</span>
          <span>Collections {{ profileSummary.totalCollectCount }}</span>
          <span>Followers {{ profileSummary.followerCount }}</span>
          <span>Following {{ profileSummary.followingCount }}</span>
        </div>
        <button class="secondary-btn compact" @click="loadMyMoments">My moments</button>
        <button v-if="viewingMine && viewingProfileUserId !== currentUserId" class="secondary-btn compact" @click="loadTimeline">
          Back to timeline
        </button>
        <button class="secondary-btn compact" @click="loadCollectedMoments">Collections</button>
        <button class="secondary-btn compact" @click="loadTimeline">Timeline</button>
      </section>

      <section class="profile-card">
        <div class="notification-head">
          <h3>Notifications <span v-if="unreadNotifications">({{ unreadNotifications }})</span></h3>
          <button class="secondary-btn compact" :disabled="loadingNotifications" @click="loadNotifications">
            {{ loadingNotifications ? 'Loading' : 'Refresh' }}
          </button>
        </div>
        <button v-if="unreadNotifications" class="secondary-btn compact" @click="markAllNotificationsRead">
          Mark all read
        </button>
        <div v-if="!notifications.length" class="notification-empty">No updates</div>
        <div v-else class="notification-list">
          <button
            v-for="notification in notifications"
            :key="notification.notificationId"
            :class="{ unread: notification.read === 0 }"
            type="button"
            @click="openNotification(notification)"
          >
            <strong>{{ notification.title }}</strong>
            <span>{{ notification.content }}</span>
            <small>{{ formatTime(notification.createdAt) }}</small>
          </button>
        </div>
      </section>

      <section v-if="isModerator" class="profile-card">
        <div class="notification-head">
          <h3>Reports</h3>
          <button class="secondary-btn compact" :disabled="loadingReports" @click="loadReports">
            {{ loadingReports ? 'Loading' : 'Refresh' }}
          </button>
        </div>
        <div v-if="!reports.length" class="notification-empty">No pending reports</div>
        <div v-else class="report-list">
          <article v-for="report in reports" :key="report.reportId">
            <strong>#{{ report.reportId }} {{ report.authorNickname || `Moment ${report.momentId}` }}</strong>
            <p>{{ report.reason }}</p>
            <small>{{ report.momentContent || 'Moment unavailable' }}</small>
            <div class="report-actions">
              <button class="secondary-btn compact" @click="reviewReport(report, 3, false)">Reject</button>
              <button class="danger-action compact" @click="reviewReport(report, 2, true)">Remove</button>
            </div>
          </article>
        </div>
      </section>
    </aside>

    <main class="timeline-panel">
      <header class="timeline-header">
        <div>
          <h2>{{ timelineTitle }}</h2>
          <p>{{ moments.length }} loaded</p>
        </div>
        <button class="icon-button" :disabled="loading" @click="refreshCurrentView">
          Refresh
        </button>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>
      <div v-if="loading" class="empty-state">Loading moments...</div>
      <div v-else-if="!moments.length" class="empty-state">No moments yet</div>

      <article v-for="moment in moments" :key="moment.momentId" class="moment-item">
        <header class="moment-head">
          <button class="avatar" type="button" @click="openUserMoments(moment.authorId)">
            <img v-if="moment.authorAvatarUrl" :src="moment.authorAvatarUrl" alt="" />
            <span v-else>{{ initials(moment.authorNickname || 'U') }}</span>
          </button>
          <div>
            <h3>
              <button class="author-link" type="button" @click="openUserMoments(moment.authorId)">
                {{ moment.authorNickname || `User ${moment.authorId}` }}
              </button>
            </h3>
            <p>{{ formatTime(moment.createdAt) }} · {{ moment.visibility }}{{ moment.location ? ` · ${moment.location}` : '' }}</p>
          </div>
          <button v-if="moment.authorId === currentUserId" class="danger-link" @click="handleDeleteMoment(moment)">Delete</button>
        </header>

        <textarea v-if="editingMomentId === moment.momentId" v-model="editDraft" rows="4" maxlength="5000"></textarea>
        <p v-else class="moment-content">{{ moment.content }}</p>
        <div v-if="moment.tags.length || moment.mood || moment.activity" class="moment-meta-tags">
          <span v-for="tag in moment.tags" :key="tag">#{{ tag }}</span>
          <span v-if="moment.mood">{{ moment.mood }}</span>
          <span v-if="moment.activity">{{ moment.activity }}</span>
        </div>
        <div v-if="editingMomentId === moment.momentId" class="moment-actions">
          <button class="primary-btn compact" @click="saveEdit(moment)">Save</button>
          <button class="secondary-btn compact" @click="cancelEdit">Cancel</button>
        </div>

        <div v-if="moment.mediaList.length" class="moment-media-grid">
          <button v-for="media in moment.mediaList" :key="media.fileId" type="button" @click="previewUrl = media.fileUrl">
            <img v-if="media.mimeType.startsWith('image/')" :src="media.fileUrl" alt="" />
            <span v-else>Video/File</span>
          </button>
        </div>

        <footer class="moment-actions">
          <button @click="toggleLike(moment)">{{ moment.liked ? 'Unlike' : 'Like' }} · {{ moment.likeCount }}</button>
          <button @click="toggleCollect(moment)">{{ moment.collected ? 'Uncollect' : 'Collect' }} · {{ moment.collectCount }}</button>
          <button @click="toggleComments(moment)">Comments · {{ moment.commentCount }}</button>
          <button @click="showLikes(moment)">Liked by</button>
          <button v-if="moment.authorId === currentUserId" @click="startEdit(moment)">Edit</button>
          <button v-else @click="handleReport(moment)">Report</button>
        </footer>

        <section v-if="openCommentMomentId === moment.momentId" class="comments-panel">
          <div v-if="mentionUserIdsDraft.length" class="mention-draft">
            Mentioning {{ mentionUserIdsDraft.map(labelFriend).join(', ') }}
            <button type="button" @click="clearCommentMentions">Clear</button>
          </div>
          <form class="comment-form" @submit.prevent="submitComment(moment)">
            <input v-model="commentDraft" maxlength="1000" placeholder="Write a comment" />
            <button class="secondary-btn compact" type="button" @click="chooseCommentMentions">@User</button>
            <button class="primary-btn compact" :disabled="!commentDraft.trim()">Comment</button>
          </form>
          <div v-for="comment in comments" :key="comment.commentId" class="comment-row">
            <span>
              <button class="author-link comment-author" type="button" @click="openUserMoments(comment.userId)">
                {{ comment.userNickname || `User ${comment.userId}` }}
              </button>
              <small v-if="comment.replyToCommentId"> replied #{{ comment.replyToCommentId }}</small>
              {{ comment.content }}
            </span>
            <button @click="startReply(comment)">Reply</button>
            <button v-if="canDeleteComment(moment, comment)" @click="removeComment(moment, comment)">Delete</button>
          </div>
        </section>
      </article>

      <button v-if="hasMore && !loading" class="load-more" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? 'Loading' : 'Load more' }}
      </button>
    </main>

    <button v-if="previewUrl" class="image-lightbox" @click="previewUrl = ''">
      <img :src="previewUrl" alt="preview" />
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { uploadFile, type UploadResponse } from '../api/file'
import { listFriends, type Friend } from '../api/friend'
import {
  addMomentComment,
  collectMoment,
  countMomentUnreadNotifications,
  createMoment,
  deleteMoment,
  deleteMomentComment,
  getMoment,
  getMomentProfileSummary,
  likeMoment,
  listCollectedMoments as fetchCollectedMoments,
  listMomentLikes,
  listMomentNotifications,
  listMomentReports,
  listMomentComments,
  listMoments,
  listUserMoments,
  markAllMomentNotificationsRead,
  markMomentNotificationRead,
  reportMoment,
  reviewMomentReport,
  uncollectMoment,
  unlikeMoment,
  updateMoment,
  type Moment,
  type MomentComment,
  type MomentProfileSummary,
  type MomentNotification,
  type MomentReport,
} from '../api/moment'

const moments = ref<Moment[]>([])
const comments = ref<MomentComment[]>([])
const notifications = ref<MomentNotification[]>([])
const reports = ref<MomentReport[]>([])
const profileSummary = ref<MomentProfileSummary | null>(null)
const friends = ref<Friend[]>([])
const unreadNotifications = ref(0)
const draft = ref('')
const locationDraft = ref('')
const tagDraft = ref('')
const moodDraft = ref('')
const activityDraft = ref('')
const visibilityDraft = ref('public')
const visibleUserIdsDraft = ref<number[]>([])
const editDraft = ref('')
const commentDraft = ref('')
const mentionUserIdsDraft = ref<number[]>([])
const notice = ref('')
const userEmail = ref('')
const userNickname = ref('')
const currentUserId = ref(0)
const page = ref(1)
const pageSize = 10
const hasMore = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadingNotifications = ref(false)
const loadingReports = ref(false)
const creating = ref(false)
const viewingMine = ref(false)
const viewingCollections = ref(false)
const viewingProfileUserId = ref<number | null>(null)
const mediaInput = ref<HTMLInputElement | null>(null)
const mediaDraft = ref<UploadResponse[]>([])
const previewUrl = ref('')
const editingMomentId = ref<number | null>(null)
const openCommentMomentId = ref<number | null>(null)
const replyToCommentId = ref<number | undefined>()

const canPost = computed(() => draft.value.trim().length > 0 || mediaDraft.value.length > 0)
const isModerator = computed(() => currentUserId.value === 1)
const timelineTitle = computed(() => {
  if (viewingCollections.value) return 'Collections'
  if (!viewingMine.value) return 'Timeline'
  if (viewingProfileUserId.value === currentUserId.value) return 'My moments'
  const name = profileSummary.value?.nickname || `User ${viewingProfileUserId.value}`
  return `${name} moments`
})

onMounted(async () => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    const data = JSON.parse(authData)
    currentUserId.value = data.userId
    userEmail.value = data.email || ''
    userNickname.value = data.nickname || ''
  }
  await loadTimeline()
  await loadFriends()
  await loadProfileSummary(currentUserId.value)
  await loadNotifications()
  if (isModerator.value) await loadReports()
})

async function loadTimeline() {
  viewingMine.value = false
  viewingCollections.value = false
  viewingProfileUserId.value = null
  page.value = 1
  await loadProfileSummary(currentUserId.value)
  await loadMomentPage(false)
}

async function loadMyMoments() {
  viewingMine.value = true
  viewingCollections.value = false
  viewingProfileUserId.value = currentUserId.value
  page.value = 1
  await loadProfileSummary(currentUserId.value)
  await loadMomentPage(false)
}

async function loadCollectedMoments() {
  viewingMine.value = false
  viewingCollections.value = true
  viewingProfileUserId.value = null
  page.value = 1
  await loadProfileSummary(currentUserId.value)
  await loadMomentPage(false)
}

async function openUserMoments(userId: number) {
  viewingMine.value = true
  viewingCollections.value = false
  viewingProfileUserId.value = userId
  page.value = 1
  await loadProfileSummary(userId)
  await loadMomentPage(false)
}

async function refreshCurrentView() {
  if (viewingCollections.value) {
    await loadCollectedMoments()
    return
  }
  if (viewingMine.value) {
    await openUserMoments(viewingProfileUserId.value || currentUserId.value)
    return
  }
  await loadTimeline()
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  page.value += 1
  await loadMomentPage(true)
}

async function loadMomentPage(append: boolean) {
  loading.value = !append
  loadingMore.value = append
  notice.value = ''
  try {
    const result = viewingCollections.value
      ? await fetchCollectedMoments(page.value, pageSize)
      : viewingMine.value
        ? await listUserMoments(viewingProfileUserId.value || currentUserId.value, page.value, pageSize)
        : await listMoments(page.value, pageSize)
    moments.value = append ? [...moments.value, ...result.list] : result.list
    hasMore.value = result.hasMore
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load moments failed'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function loadNotifications() {
  loadingNotifications.value = true
  notice.value = ''
  try {
    const result = await listMomentNotifications(1, 10)
    notifications.value = result.list
    unreadNotifications.value = await countMomentUnreadNotifications()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load notifications failed'
  } finally {
    loadingNotifications.value = false
  }
}

async function loadFriends() {
  try {
    friends.value = await listFriends()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load friends failed'
  }
}

async function loadProfileSummary(userId: number) {
  if (!userId) return
  try {
    profileSummary.value = await getMomentProfileSummary(userId)
  } catch {
    profileSummary.value = null
  }
}

async function loadReports() {
  if (!isModerator.value) return
  loadingReports.value = true
  try {
    const result = await listMomentReports(1, 1, 10)
    reports.value = result.list
  } catch (error) {
    reports.value = []
    notice.value = error instanceof Error ? error.message : 'Load reports failed'
  } finally {
    loadingReports.value = false
  }
}

async function handleMediaSelect(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  if (!files.length) return
  if (mediaDraft.value.length + files.length > 12) {
    notice.value = 'Media must not exceed 12'
    return
  }
  const nextFiles = [...mediaDraft.value, ...files.map((file) => ({ mimeType: file.type }))]
  const imageCount = nextFiles.filter((item) => item.mimeType.startsWith('image/')).length
  const videoCount = nextFiles.filter((item) => item.mimeType.startsWith('video/')).length
  if (imageCount > 9 || videoCount > 3) {
    notice.value = 'Media supports up to 9 images and 3 videos'
    return
  }
  creating.value = true
  try {
    const uploaded = []
    for (const file of files) {
      const scene = file.type.startsWith('video/') ? 'moment-video' : 'moment-image'
      uploaded.push(await uploadFile(file, scene))
    }
    mediaDraft.value = [...mediaDraft.value, ...uploaded]
    if (mediaInput.value) mediaInput.value.value = ''
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Media upload failed'
  } finally {
    creating.value = false
  }
}

function removeMedia(fileId: number) {
  mediaDraft.value = mediaDraft.value.filter((item) => item.fileId !== fileId)
}

async function handleCreateMoment() {
  if (!canPost.value) return
  creating.value = true
  try {
    await createMoment({
      content: draft.value.trim(),
      fileIds: mediaDraft.value.map((item) => item.fileId),
      location: locationDraft.value.trim(),
      tags: parseTagDraft(),
      mood: moodDraft.value.trim(),
      activity: activityDraft.value.trim(),
      visibility: visibilityDraft.value,
      visibleUserIds: visibilityDraft.value === 'specified' ? visibleUserIdsDraft.value : [],
    })
    draft.value = ''
    locationDraft.value = ''
    tagDraft.value = ''
    moodDraft.value = ''
    activityDraft.value = ''
    visibilityDraft.value = 'public'
    visibleUserIdsDraft.value = []
    mediaDraft.value = []
    await loadTimeline()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Create moment failed'
  } finally {
    creating.value = false
  }
}

async function toggleLike(moment: Moment) {
  const updated = moment.liked ? await unlikeMoment(moment.momentId) : await likeMoment(moment.momentId)
  replaceMoment(updated)
}

async function showLikes(moment: Moment) {
  try {
    const users = await listMomentLikes(moment.momentId)
    window.alert(users.length ? users.map((user) => user.nickname || user.email || `User ${user.userId}`).join('\n') : 'No likes yet')
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Load likes failed'
  }
}

async function toggleCollect(moment: Moment) {
  const updated = moment.collected ? await uncollectMoment(moment.momentId) : await collectMoment(moment.momentId)
  replaceMoment(updated)
}

async function toggleComments(moment: Moment) {
  if (openCommentMomentId.value === moment.momentId) {
    openCommentMomentId.value = null
    comments.value = []
    mentionUserIdsDraft.value = []
    replyToCommentId.value = undefined
    return
  }
  openCommentMomentId.value = moment.momentId
  mentionUserIdsDraft.value = []
  replyToCommentId.value = undefined
  comments.value = await listMomentComments(moment.momentId)
}

async function submitComment(moment: Moment) {
  if (!commentDraft.value.trim()) return
  await addMomentComment(moment.momentId, commentDraft.value.trim(), replyToCommentId.value, mentionUserIdsDraft.value)
  commentDraft.value = ''
  mentionUserIdsDraft.value = []
  replyToCommentId.value = undefined
  comments.value = await listMomentComments(moment.momentId)
  replaceMoment({ ...moment, commentCount: moment.commentCount + 1 })
}

function startReply(comment: MomentComment) {
  replyToCommentId.value = comment.commentId
  commentDraft.value = `Reply ${comment.userNickname || `User ${comment.userId}`}: `
}

function chooseCommentMentions() {
  if (!friends.value.length) {
    notice.value = 'No friends available to mention'
    return
  }
  const names = friends.value.map((friend, index) => `${index + 1}. ${friend.remarkName || friend.nickname || friend.email}`).join('\n')
  const raw = window.prompt(`Choose friends to mention by number, separated by commas\n${names}`)
  if (!raw) return
  const userIds = raw
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((index) => Number.isInteger(index) && index > 0 && index <= friends.value.length)
    .map((index) => friends.value[index - 1].userId)
  if (!userIds.length) {
    notice.value = 'No valid friends selected'
    return
  }
  mentionUserIdsDraft.value = Array.from(new Set([...mentionUserIdsDraft.value, ...userIds]))
  const mentionText = userIds.map((id) => `@${labelFriend(id)}`).join(' ')
  commentDraft.value = `${commentDraft.value}${commentDraft.value ? ' ' : ''}${mentionText} `
}

function clearCommentMentions() {
  mentionUserIdsDraft.value = []
}

function parseTagDraft() {
  return Array.from(
    new Set(
      tagDraft.value
        .split(',')
        .map((tag) => tag.trim().replace(/^#+/, ''))
        .filter(Boolean),
    ),
  )
}

async function removeComment(moment: Moment, comment: MomentComment) {
  if (!window.confirm('Delete this comment?')) return
  await deleteMomentComment(moment.momentId, comment.commentId)
  comments.value = comments.value.filter((item) => item.commentId !== comment.commentId)
  replaceMoment({ ...moment, commentCount: Math.max(0, moment.commentCount - 1) })
}

function startEdit(moment: Moment) {
  editingMomentId.value = moment.momentId
  editDraft.value = moment.content
}

function cancelEdit() {
  editingMomentId.value = null
  editDraft.value = ''
}

async function saveEdit(moment: Moment) {
  const updated = await updateMoment(moment.momentId, { content: editDraft.value.trim() })
  replaceMoment(updated)
  cancelEdit()
}

async function handleDeleteMoment(moment: Moment) {
  if (!window.confirm('Delete this moment?')) return
  await deleteMoment(moment.momentId)
  moments.value = moments.value.filter((item) => item.momentId !== moment.momentId)
}

async function handleReport(moment: Moment) {
  const reason = window.prompt('Report reason')
  if (!reason?.trim()) return
  try {
    await reportMoment(moment.momentId, reason.trim())
    notice.value = 'Report submitted'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Report failed'
  }
}

async function openNotification(notification: MomentNotification) {
  if (notification.read === 0) {
    try {
      await markMomentNotificationRead(notification.notificationId)
      notification.read = 1
      unreadNotifications.value = Math.max(0, unreadNotifications.value - 1)
    } catch (error) {
      notice.value = error instanceof Error ? error.message : 'Mark notification failed'
    }
  }
  focusRelatedMoment(notification.relatedId)
}

async function markAllNotificationsRead() {
  try {
    await markAllMomentNotificationsRead()
    notifications.value = notifications.value.map((notification) => ({ ...notification, read: 1 }))
    unreadNotifications.value = 0
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Mark notifications failed'
  }
}

async function reviewReport(report: MomentReport, status: number, deleteMoment: boolean) {
  const note = window.prompt(deleteMoment ? 'Handle note for removal' : 'Handle note for rejection', '')
  if (note === null) return
  try {
    await reviewMomentReport(report.reportId, { status, handleNote: note.trim(), deleteMoment })
    reports.value = reports.value.filter((item) => item.reportId !== report.reportId)
    if (deleteMoment) {
      moments.value = moments.value.filter((item) => item.momentId !== report.momentId)
    }
    notice.value = 'Report reviewed'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : 'Review report failed'
  }
}

async function focusRelatedMoment(relatedId: number | null) {
  if (!relatedId) return
  let target = moments.value.find((item) => item.momentId === relatedId)
  if (!target) {
    try {
      target = await getMoment(relatedId)
      moments.value = [target, ...moments.value]
    } catch (error) {
      notice.value = error instanceof Error ? error.message : 'Related moment unavailable'
      return
    }
  }
  void toggleComments(target)
}

function canDeleteComment(moment: Moment, comment: MomentComment) {
  return moment.authorId === currentUserId.value || comment.userId === currentUserId.value
}

function replaceMoment(moment: Moment) {
  moments.value = moments.value.map((item) => (item.momentId === moment.momentId ? moment : item))
}

function labelFriend(userId: number) {
  const friend = friends.value.find((item) => item.userId === userId)
  return friend?.remarkName || friend?.nickname || friend?.email || `User ${userId}`
}

function initials(value: string) {
  return value.slice(0, 2).toUpperCase()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.moments-workspace {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  min-height: calc(100vh - 64px);
  max-width: 1220px;
  background: #fff;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 18px 48px rgba(17, 34, 68, 0.08);
}

.moments-side {
  border-right: 1px solid #d8e0ea;
  background: #f7f9fc;
  padding: 16px;
  display: grid;
  align-content: start;
  gap: 16px;
}

.moments-side h2,
.timeline-header h2 {
  margin: 0;
  font-size: 18px;
}

.moments-side p,
.timeline-header p {
  margin: 4px 0 0;
  color: #53627d;
  font-size: 13px;
}

.composer-card,
.profile-card {
  display: grid;
  gap: 10px;
}

.profile-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.profile-stats span {
  border: 1px solid #d8e0ea;
  border-radius: 999px;
  background: #fff;
  color: #53627d;
  font-size: 12px;
  padding: 4px 8px;
}

.composer-card textarea,
.composer-card input,
.composer-card select,
.moment-item textarea,
.comment-form input {
  width: 100%;
  min-width: 0;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  padding: 10px 12px;
  background: #fff;
}

.composer-inline {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.composer-actions,
.moment-actions,
.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.media-draft {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.friend-picker {
  display: grid;
  gap: 6px;
  max-height: 150px;
  overflow: auto;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  padding: 8px;
}

.friend-picker label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #172033;
  font-size: 13px;
}

.friend-picker input {
  width: auto;
}

.friend-picker small {
  color: #748198;
}

.media-draft span {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  border-radius: 999px;
  background: #e9eef6;
  padding: 6px 10px;
  font-size: 12px;
}

.media-draft button,
.danger-link {
  border: 0;
  background: transparent;
  color: #9b2626;
  cursor: pointer;
}

.notification-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notification-head h3 {
  margin: 0;
  font-size: 15px;
}

.notification-empty {
  color: #748198;
  font-size: 13px;
}

.notification-list {
  display: grid;
  gap: 8px;
  max-height: 260px;
  overflow: auto;
}

.report-list {
  display: grid;
  gap: 8px;
  max-height: 320px;
  overflow: auto;
}

.report-list article {
  display: grid;
  gap: 6px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  padding: 10px;
}

.report-list strong {
  font-size: 13px;
  color: #172033;
}

.report-list p,
.report-list small {
  margin: 0;
  color: #53627d;
  font-size: 12px;
  overflow-wrap: anywhere;
}

.report-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.notification-list button {
  display: grid;
  gap: 4px;
  width: 100%;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #172033;
  padding: 10px;
  text-align: left;
  cursor: pointer;
}

.notification-list button.unread {
  border-color: #4f8cff;
  background: #f2f7ff;
}

.notification-list span,
.notification-list small {
  color: #53627d;
  font-size: 12px;
}

.timeline-panel {
  min-width: 0;
  padding: 18px;
  overflow: auto;
}

.timeline-header {
  margin-bottom: 16px;
}

.moment-item {
  display: grid;
  gap: 12px;
  padding: 16px 0;
  border-top: 1px solid #d8e0ea;
}

.moment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.moment-head div {
  min-width: 0;
  flex: 1;
}

.moment-head h3 {
  margin: 0;
  font-size: 15px;
}

.author-link {
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  font: inherit;
  padding: 0;
}

.moment-head p {
  margin: 4px 0 0;
  color: #53627d;
  font-size: 12px;
}

.moment-content {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.6;
}

.moment-meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.moment-meta-tags span {
  border-radius: 999px;
  background: #eef4ec;
  color: #3f6840;
  font-size: 12px;
  padding: 5px 9px;
}

.moment-media-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}

.moment-media-grid button {
  aspect-ratio: 1;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  overflow: hidden;
  background: #f7f9fc;
  cursor: zoom-in;
}

.moment-media-grid img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.moment-actions {
  justify-content: flex-start;
  flex-wrap: wrap;
}

.moment-actions button,
.icon-button,
.danger-action {
  border: 0;
  border-radius: 6px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
}

.danger-action {
  background: #ffe8e8;
  color: #9b2626;
}

.comments-panel {
  display: grid;
  gap: 10px;
  background: #f7f9fc;
  border-radius: 8px;
  padding: 12px;
}

.mention-draft {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid #d8e0ea;
  border-radius: 6px;
  background: #fff;
  color: #53627d;
  padding: 8px 10px;
  font-size: 12px;
}

.mention-draft button {
  border: 0;
  background: transparent;
  color: #9b2626;
  cursor: pointer;
}

.comment-form,
.comment-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.comment-row {
  justify-content: space-between;
  color: #53627d;
}

.comment-author {
  color: #172033;
  font-weight: 700;
}

.comment-row small {
  color: #748198;
  margin-left: 4px;
}

.comment-row button {
  border: 0;
  background: transparent;
  color: #9b2626;
  cursor: pointer;
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

.empty-state {
  padding: 40px;
  text-align: center;
  color: #748198;
}

.load-more {
  margin: 20px auto 0;
  display: block;
  border: 0;
  border-radius: 999px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 10px 16px;
  font-weight: 700;
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

@media (max-width: 900px) {
  .moments-workspace {
    grid-template-columns: 1fr;
  }

  .moments-side {
    border-right: 0;
    border-bottom: 1px solid #d8e0ea;
  }
}
</style>
