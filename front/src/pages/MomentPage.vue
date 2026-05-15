<template>
  <section class="moments-workspace liquid-glass">
    <main class="timeline-panel">
      <header class="timeline-header">
        <div class="timeline-title-block">
          <p class="eyebrow">朋友圈</p>
          <h2>{{ timelineTitle }}</h2>
          <p>已加载 {{ moments.length }} 条动态</p>
        </div>
        <div class="timeline-tools">
          <button class="owner-avatar-trigger" type="button" title="查看我的动态" @click="openMomentDrawer('profile')">
            <AvatarFrame :name="profileSummary?.nickname || userNickname || userEmail || 'ME'" size="lg" interactive />
          </button>
          <button class="icon-button" :disabled="loading" title="刷新" @click="refreshCurrentView">
            <SvgIcon name="refresh" />
          </button>
          <button class="icon-button notification-trigger" title="通知" @click="openMomentDrawer('notifications')">
            <SvgIcon name="chat" />
            <span v-if="unreadNotifications">{{ unreadNotifications }}</span>
          </button>
          <button v-if="isModerator" class="icon-button" title="举报审核" @click="openMomentDrawer('reports')">
            <SvgIcon name="block" />
          </button>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>
      <div v-if="loading" class="empty-state">动态加载中...</div>
      <div v-else-if="!moments.length" class="empty-state">暂无动态</div>

      <article
        v-for="moment in moments"
        :key="moment.momentId"
        class="moment-item"
        :class="{ highlighted: focusedMomentId === moment.momentId }"
        :data-moment-id="moment.momentId"
      >
        <header class="moment-head">
          <button class="avatar-button" type="button" @click="openUserMoments(moment.authorId)">
            <AvatarFrame :src="moment.authorAvatarUrl" :name="moment.authorNickname || 'U'" size="sm" interactive />
          </button>
          <div>
            <h3>
              <button class="author-link" type="button" @click="openUserMoments(moment.authorId)">
                {{ moment.authorNickname || `User ${moment.authorId}` }}
              </button>
            </h3>
            <p>{{ formatTime(moment.createdAt) }} | {{ moment.visibility }}{{ moment.location ? ` | ${moment.location}` : '' }}</p>
          </div>
          <button v-if="moment.authorId === currentUserId" class="danger-link" @click="handleDeleteMoment(moment)">删除</button>
        </header>

        <textarea v-if="editingMomentId === moment.momentId" v-model="editDraft" rows="4" maxlength="5000"></textarea>
        <p v-else class="moment-content">{{ normalizeDisplayText(moment.content) }}</p>
        <div v-if="moment.tags.length || moment.mood || moment.activity" class="moment-meta-tags">
          <span v-for="tag in moment.tags" :key="tag">#{{ tag }}</span>
          <span v-if="moment.mood">{{ moment.mood }}</span>
          <span v-if="moment.activity">{{ moment.activity }}</span>
        </div>
        <div v-if="editingMomentId === moment.momentId" class="moment-actions">
          <button class="primary-btn compact" @click="saveEdit(moment)">保存</button>
          <button class="secondary-btn compact" @click="cancelEdit">取消</button>
        </div>

        <div v-if="moment.mediaList.length" class="moment-media-grid">
          <button v-for="media in moment.mediaList" :key="media.fileId" type="button" @click="previewUrl = resolveAssetUrl(media.fileUrl)">
            <img v-if="media.mimeType.startsWith('image/')" :src="resolveAssetUrl(media.fileUrl)" alt="" />
            <span v-else>视频 / 文件</span>
          </button>
        </div>

        <footer class="moment-actions">
          <button @click="toggleLike(moment)"><SvgIcon name="star" />{{ moment.liked ? '取消点赞' : '点赞' }} {{ moment.likeCount }}</button>
          <button @click="toggleCollect(moment)"><SvgIcon name="profile" />{{ moment.collected ? '取消收藏' : '收藏' }} {{ moment.collectCount }}</button>
          <button @click="toggleComments(moment)"><SvgIcon name="chat" />评论 {{ moment.commentCount }}</button>
          <button @click="showLikes(moment)"><SvgIcon name="search" />点赞用户</button>
          <button v-if="moment.authorId === currentUserId" @click="startEdit(moment)"><SvgIcon name="edit" />编辑</button>
          <button v-else @click="handleReport(moment)"><SvgIcon name="block" />举报</button>
        </footer>

        <section v-if="openCommentMomentId === moment.momentId" class="comments-panel">
          <div v-if="mentionUserIdsDraft.length" class="mention-draft">
            已提及 {{ mentionUserIdsDraft.map(labelFriend).join('、') }}
            <button type="button" @click="clearCommentMentions">清空</button>
          </div>
          <form class="comment-form" @submit.prevent="submitComment(moment)">
            <div class="comment-composer">
              <div v-if="replyToCommentId" class="reply-banner">
                <span>回复 {{ replyTargetLabel(replyToCommentId) }}</span>
                <button type="button" @click="cancelReply">取消</button>
              </div>
              <textarea v-model="commentDraft" rows="3" maxlength="1000" :placeholder="commentPlaceholder"></textarea>
              <div class="comment-toolbar">
                <button class="secondary-btn compact" type="button" @click="chooseCommentMentions">
                  <SvgIcon name="at" />
                  @好友
                </button>
                <button class="primary-btn compact icon-text-btn" :disabled="!commentDraft.trim()">
                  <SvgIcon name="send" />
                  发表评论
                </button>
              </div>
            </div>
          </form>
          <div
            v-for="comment in visibleComments"
            :key="comment.commentId"
            class="comment-row"
            :class="{ nested: Boolean(comment.replyToCommentId) }"
          >
            <AvatarFrame :name="comment.userNickname || `User ${comment.userId}`" size="sm" />
            <div class="comment-stack">
              <div class="comment-bubble">
                <div class="comment-head">
                  <button class="author-link comment-author" type="button" @click="openUserMoments(comment.userId)">
                    {{ comment.userNickname || `User ${comment.userId}` }}
                  </button>
                  <small v-if="comment.replyToCommentId">回复 {{ replyTargetLabel(comment.replyToCommentId) }}</small>
                </div>
                <div v-if="comment.replyToCommentId" class="comment-reply-target">
                  @{{ replyTargetLabel(comment.replyToCommentId) }}
                </div>
                <p>{{ normalizeDisplayText(comment.content) }}</p>
              </div>
              <div class="comment-meta">
                <button @click="startReply(comment)">回复</button>
                <button v-if="commentReplyCount(comment.commentId)" @click="toggleReplies(comment.commentId)">
                  {{ isRepliesExpanded(comment.commentId) ? '收起回复' : `展开回复 ${commentReplyCount(comment.commentId)}` }}
                </button>
                <button v-if="canDeleteComment(moment, comment)" @click="removeComment(moment, comment)">删除</button>
              </div>
            </div>
          </div>
        </section>
      </article>

      <button v-if="hasMore && !loading" class="load-more" :disabled="loadingMore" @click="loadMore">
        {{ loadingMore ? '加载中' : '加载更多' }}
      </button>
    </main>

    <button class="floating-compose-btn" type="button" title="发布动态" @click="openMomentDrawer('compose')">
      <SvgIcon name="plus" />
    </button>

    <div v-if="showMomentDrawer" class="drawer-backdrop" @click.self="closeMomentDrawer">
      <aside class="moment-drawer">
        <header class="drawer-head">
          <div>
            <p class="eyebrow">功能面板</p>
            <h3>{{ activeDrawerTitle }}</h3>
          </div>
          <button class="icon-button" type="button" @click="closeMomentDrawer">
            <SvgIcon name="close" />
          </button>
        </header>

        <form v-if="drawerMode === 'compose'" class="composer-card" @submit.prevent="handleCreateMoment">
          <section class="compose-block">
            <header class="compose-block-head">
              <strong>内容</strong>
              <small>先写下这一刻想分享的内容</small>
            </header>
            <textarea v-model="draft" rows="6" maxlength="5000" placeholder="分享这一刻的新鲜事"></textarea>
          </section>

          <section class="compose-block">
            <header class="compose-block-head">
              <strong>氛围</strong>
              <small>地点、标签、心情和活动信息</small>
            </header>
            <input v-model="locationDraft" placeholder="位置" />
            <input v-model="tagDraft" placeholder="标签，使用逗号分隔" />
            <div class="composer-inline">
              <input v-model="moodDraft" maxlength="32" placeholder="心情" />
              <input v-model="activityDraft" maxlength="32" placeholder="活动" />
            </div>
          </section>

          <section class="compose-block">
            <header class="compose-block-head">
              <strong>可见范围</strong>
              <small>控制谁可以看到这条动态</small>
            </header>
            <select v-model="visibilityDraft">
              <option value="public">公开</option>
              <option value="friends">好友可见</option>
              <option value="private">仅自己可见</option>
              <option value="specified">指定可见</option>
            </select>
            <div v-if="visibilityDraft === 'specified'" class="friend-picker">
              <label v-for="friend in friends" :key="friend.userId">
                <input v-model="visibleUserIdsDraft" type="checkbox" :value="friend.userId" />
                <span>{{ friend.remarkName || friend.nickname || friend.email }}</span>
              </label>
              <small v-if="!friends.length">暂无可选好友</small>
            </div>
          </section>

          <section class="compose-block">
            <header class="compose-block-head">
              <strong>媒体</strong>
              <small>支持图片和视频，按需补充内容氛围</small>
            </header>
            <input ref="mediaInput" class="hidden-file-input" type="file" accept="image/*,video/*" multiple @change="handleMediaSelect" />
            <button type="button" class="media-picker-button" :disabled="creating" @click="mediaInput?.click()">
              <span><SvgIcon name="image" /></span>
              <strong>添加图片 / 视频</strong>
              <small>支持多选，最多 9 张图片或 3 个视频</small>
            </button>
            <div v-if="mediaDraft.length" class="media-draft">
              <span v-for="media in mediaDraft" :key="media.fileId">
                {{ media.fileName }}
                <button type="button" @click="removeMedia(media.fileId)">x</button>
              </span>
            </div>
          </section>

          <div class="composer-actions">
            <button class="primary-btn compact icon-text-btn" :disabled="creating || !canPost">
              <SvgIcon name="send" />
              {{ creating ? '发布中' : '发布' }}
            </button>
          </div>
        </form>

        <section v-else-if="drawerMode === 'profile'" class="drawer-section">

          <div v-if="profileSummary" class="profile-stats">
            <span>动态 {{ profileSummary.momentCount }}</span>
            <span>点赞 {{ profileSummary.totalLikeCount }}</span>
            <span>评论 {{ profileSummary.totalCommentCount }}</span>
            <span>收藏 {{ profileSummary.totalCollectCount }}</span>
            <span>粉丝 {{ profileSummary.followerCount }}</span>
            <span>关注 {{ profileSummary.followingCount }}</span>
          </div>
          <button class="drawer-action" @click="loadMyMoments"><SvgIcon name="moment" />我的动态</button>
          <button class="drawer-action" @click="loadCollectedMoments"><SvgIcon name="profile" />我的收藏</button>
          <button class="drawer-action" @click="loadTimeline"><SvgIcon name="refresh" />回到时间线</button>
        </section>

        <section v-else-if="drawerMode === 'notifications'" class="drawer-section">
          <div class="notification-head">
            <h3>通知 <span v-if="unreadNotifications">({{ unreadNotifications }})</span></h3>
            <button class="secondary-btn compact" :disabled="loadingNotifications" @click="loadNotifications">
              {{ loadingNotifications ? '加载中' : '刷新' }}
            </button>
          </div>
          <button v-if="unreadNotifications" class="drawer-action" @click="markAllNotificationsRead">
            <SvgIcon name="refresh" />全部已读
          </button>
          <div v-if="!notifications.length" class="notification-empty">暂无通知</div>
          <div v-else class="notification-list">
            <button
              v-for="notification in notifications"
              :key="notification.notificationId"
              :class="{ unread: notification.read === 0 }"
              type="button"
              @click="openNotification(notification)"
            >
              <div class="notification-card-head">
                <span class="notification-pill">
                  <SvgIcon :name="notificationIcon(notification.notificationType)" />
                  {{ notificationTypeLabel(notification.notificationType) }}
                </span>
                <small>{{ formatTime(notification.createdAt) }}</small>
              </div>
              <strong>{{ normalizeDisplayText(notification.title) }}</strong>
              <span>{{ normalizeDisplayText(notification.content) }}</span>
            </button>
          </div>
        </section>

        <section v-else-if="drawerMode === 'reports'" class="drawer-section">
          <div class="notification-head">
            <h3>举报审核</h3>
            <button class="secondary-btn compact" :disabled="loadingReports" @click="loadReports">
              {{ loadingReports ? '加载中' : '刷新' }}
            </button>
          </div>
          <div v-if="!reports.length" class="notification-empty">暂无待处理举报</div>
          <div v-else class="report-list">
            <article v-for="report in reports" :key="report.reportId">
              <strong>#{{ report.reportId }} {{ report.authorNickname || `Moment ${report.momentId}` }}</strong>
              <p>{{ report.reason }}</p>
              <small>{{ report.momentContent || '动态内容不可用' }}</small>
              <div class="report-actions">
                <button class="secondary-btn compact" @click="reviewReport(report, 3, false)">驳回</button>
                <button class="danger-action compact" @click="reviewReport(report, 2, true)">移除</button>
              </div>
            </article>
          </div>
        </section>
      </aside>
    </div>

    <button v-if="previewUrl" class="image-lightbox" @click="previewUrl = ''">
      <img :src="previewUrl" alt="preview" />
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { uploadFile, type UploadResponse } from '../api/file'
import { listFriends, type Friend } from '../api/friend'
import AvatarFrame from '../components/AvatarFrame.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { resolveAssetUrl } from '../utils/assets'
import { normalizeDisplayText } from '../utils/text'
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
const expandedReplyIds = ref<number[]>([])
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
const drawerMode = ref<'compose' | 'profile' | 'notifications' | 'reports' | ''>('')
const focusedMomentId = ref<number | null>(null)

const canPost = computed(() => draft.value.trim().length > 0 || mediaDraft.value.length > 0)
const isModerator = computed(() => currentUserId.value === 1)
const showMomentDrawer = computed(() => drawerMode.value !== '')
const commentPlaceholder = computed(() =>
  replyToCommentId.value ? `回复 ${replyTargetLabel(replyToCommentId.value)}` : '写一条评论',
)
const visibleComments = computed(() => {
  const result: MomentComment[] = []
  const byParent = new Map<number, MomentComment[]>()

  for (const comment of comments.value) {
    if (!comment.replyToCommentId) continue
    const list = byParent.get(comment.replyToCommentId) || []
    list.push(comment)
    byParent.set(comment.replyToCommentId, list)
  }

  const appendReplies = (parentId: number) => {
    if (!expandedReplyIds.value.includes(parentId)) return
    const replies = byParent.get(parentId) || []
    for (const reply of replies) {
      result.push(reply)
      appendReplies(reply.commentId)
    }
  }

  for (const comment of comments.value) {
    if (comment.replyToCommentId) continue
    result.push(comment)
    appendReplies(comment.commentId)
  }

  return result
})
const activeDrawerTitle = computed(() => {
  if (drawerMode.value === 'compose') return '发布动态'
  if (drawerMode.value === 'profile') return '我的主页'
  if (drawerMode.value === 'notifications') return '通知中心'
  if (drawerMode.value === 'reports') return '举报审核'
  return ''
})
const timelineTitle = computed(() => {
  if (viewingCollections.value) return '我的收藏'
  if (!viewingMine.value) return '时间线'
  if (viewingProfileUserId.value === currentUserId.value) return '我的动态'
  const name = profileSummary.value?.nickname || `User ${viewingProfileUserId.value}`
  return `${name} 的动态`
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
    notice.value = error instanceof Error ? error.message : '加载动态失败'
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
    notice.value = error instanceof Error ? error.message : '加载通知失败'
  } finally {
    loadingNotifications.value = false
  }
}

async function loadFriends() {
  try {
    friends.value = await listFriends()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载好友失败'
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
    notice.value = error instanceof Error ? error.message : '加载举报失败'
  } finally {
    loadingReports.value = false
  }
}

async function handleMediaSelect(event: Event) {
  const files = Array.from((event.target as HTMLInputElement).files || [])
  if (!files.length) return
  if (mediaDraft.value.length + files.length > 12) {
    notice.value = '媒体数量不能超过 12 个'
    return
  }
  const nextFiles = [...mediaDraft.value, ...files.map((file) => ({ mimeType: file.type }))]
  const imageCount = nextFiles.filter((item) => item.mimeType.startsWith('image/')).length
  const videoCount = nextFiles.filter((item) => item.mimeType.startsWith('video/')).length
  if (imageCount > 9 || videoCount > 3) {
    notice.value = '最多支持 9 张图片和 3 个视频'
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
    notice.value = error instanceof Error ? error.message : '媒体上传失败'
  } finally {
    creating.value = false
  }
}

function removeMedia(fileId: number) {
  mediaDraft.value = mediaDraft.value.filter((item) => item.fileId !== fileId)
}

async function openMomentDrawer(mode: 'compose' | 'profile' | 'notifications' | 'reports') {
  drawerMode.value = mode
  if (mode === 'profile') await loadProfileSummary(currentUserId.value)
  if (mode === 'notifications') await loadNotifications()
  if (mode === 'reports') await loadReports()
}

function closeMomentDrawer() {
  drawerMode.value = ''
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
    closeMomentDrawer()
    await loadTimeline()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '发布动态失败'
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
    window.alert(users.length ? users.map((user) => user.nickname || user.email || `User ${user.userId}`).join('\n') : '暂无点赞')
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载点赞用户失败'
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
    expandedReplyIds.value = []
    mentionUserIdsDraft.value = []
    replyToCommentId.value = undefined
    return
  }
  openCommentMomentId.value = moment.momentId
  expandedReplyIds.value = []
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
  expandedReplyIds.value = Array.from(
    new Set(
      comments.value
        .filter((comment) => comment.replyToCommentId)
        .map((comment) => comment.replyToCommentId!)
        .filter((id) => id > 0),
    ),
  )
  replaceMoment({ ...moment, commentCount: moment.commentCount + 1 })
}

function startReply(comment: MomentComment) {
  replyToCommentId.value = comment.commentId
}

function chooseCommentMentions() {
  if (!friends.value.length) {
    notice.value = '暂无可提及的好友'
    return
  }
  const names = friends.value.map((friend, index) => `${index + 1}. ${friend.remarkName || friend.nickname || friend.email}`).join('\n')
  const raw = window.prompt(`输入序号选择要提及的好友，多个序号用逗号分隔\n${names}`)
  if (!raw) return
  const userIds = raw
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((index) => Number.isInteger(index) && index > 0 && index <= friends.value.length)
    .map((index) => friends.value[index - 1].userId)
  if (!userIds.length) {
    notice.value = '没有选择有效好友'
    return
  }
  mentionUserIdsDraft.value = Array.from(new Set([...mentionUserIdsDraft.value, ...userIds]))
  const mentionText = userIds.map((id) => `@${labelFriend(id)}`).join(' ')
  commentDraft.value = `${commentDraft.value}${commentDraft.value ? ' ' : ''}${mentionText} `
}

function clearCommentMentions() {
  mentionUserIdsDraft.value = []
}

function cancelReply() {
  replyToCommentId.value = undefined
}

function commentReplyCount(commentId: number) {
  return comments.value.filter((item) => item.replyToCommentId === commentId).length
}

function isRepliesExpanded(commentId: number) {
  return expandedReplyIds.value.includes(commentId)
}

function toggleReplies(commentId: number) {
  if (isRepliesExpanded(commentId)) {
    expandedReplyIds.value = expandedReplyIds.value.filter((id) => id !== commentId)
    return
  }
  expandedReplyIds.value = [...expandedReplyIds.value, commentId]
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
  if (!window.confirm('确认删除这条评论吗？')) return
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
  if (!window.confirm('确认删除这条动态吗？')) return
  await deleteMoment(moment.momentId)
  moments.value = moments.value.filter((item) => item.momentId !== moment.momentId)
}

async function handleReport(moment: Moment) {
  const reason = window.prompt('举报原因')
  if (!reason?.trim()) return
  try {
    await reportMoment(moment.momentId, reason.trim())
    notice.value = '举报已提交'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '举报失败'
  }
}

async function openNotification(notification: MomentNotification) {
  if (notification.read === 0) {
    try {
      await markMomentNotificationRead(notification.notificationId)
      notification.read = 1
      unreadNotifications.value = Math.max(0, unreadNotifications.value - 1)
    } catch (error) {
      notice.value = error instanceof Error ? error.message : '标记通知失败'
    }
  }
  closeMomentDrawer()
  focusRelatedMoment(notification.relatedId)
}

async function markAllNotificationsRead() {
  try {
    await markAllMomentNotificationsRead()
    notifications.value = notifications.value.map((notification) => ({ ...notification, read: 1 }))
    unreadNotifications.value = 0
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '标记全部通知失败'
  }
}

async function reviewReport(report: MomentReport, status: number, deleteMoment: boolean) {
  const note = window.prompt(deleteMoment ? '移除处理说明' : '驳回处理说明', '')
  if (note === null) return
  try {
    await reviewMomentReport(report.reportId, { status, handleNote: note.trim(), deleteMoment })
    reports.value = reports.value.filter((item) => item.reportId !== report.reportId)
    if (deleteMoment) {
      moments.value = moments.value.filter((item) => item.momentId !== report.momentId)
    }
    notice.value = '举报已处理'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '处理举报失败'
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
      notice.value = error instanceof Error ? error.message : '相关动态不可用'
      return
    }
  }
  focusedMomentId.value = relatedId
  void toggleComments(target)
  setTimeout(() => {
    const element = document.querySelector<HTMLElement>(`[data-moment-id="${relatedId}"]`)
    element?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 60)
  window.setTimeout(() => {
    if (focusedMomentId.value === relatedId) {
      focusedMomentId.value = null
    }
  }, 2400)
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

function commentAuthorName(comment: MomentComment | undefined) {
  if (!comment) return '该用户'
  return comment.userNickname || `User ${comment.userId}`
}

function replyTargetLabel(replyId: number) {
  return commentAuthorName(comments.value.find((item) => item.commentId === replyId))
}

function initials(value: string) {
  return value.slice(0, 2).toUpperCase()
}

function formatTime(value: string) {
  return new Date(value).toLocaleString()
}

function notificationTypeLabel(type: string) {
  if (type.includes('LIKE')) return '点赞'
  if (type.includes('COMMENT')) return '评论'
  if (type.includes('COLLECT')) return '收藏'
  if (type.includes('REPORT')) return '举报'
  return '通知'
}

function notificationIcon(type: string) {
  if (type.includes('LIKE')) return 'star'
  if (type.includes('COMMENT')) return 'chat'
  if (type.includes('COLLECT')) return 'profile'
  if (type.includes('REPORT')) return 'block'
  return 'inbox'
}
</script>

<style scoped>
.moments-workspace {
  position: relative;
  display: block;
  min-height: 100%;
  height: 100%;
  width: 100%;
  background:
    radial-gradient(circle at 12% 0%, rgba(79, 140, 255, 0.18), transparent 30%),
    radial-gradient(circle at 88% 14%, rgba(99, 179, 255, 0.16), transparent 26%),
    linear-gradient(135deg, #f8fbff 0%, #eef5ff 46%, #ffffff 100%);
  border: 1px solid rgba(85, 131, 255, 0.18);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(37, 87, 197, 0.12);
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

.compose-block {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid rgba(85, 131, 255, 0.14);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.compose-block-head {
  display: grid;
  gap: 4px;
}

.compose-block-head strong,
.compose-block-head small {
  margin: 0;
}

.compose-block-head small {
  color: #748198;
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
.comment-form textarea {
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
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
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
  gap: 8px;
  width: 100%;
  border: 1px solid rgba(85, 131, 255, 0.12);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(244, 249, 255, 0.92));
  color: #172033;
  padding: 14px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.notification-list button.unread {
  border-color: rgba(37, 99, 255, 0.26);
  background: linear-gradient(180deg, rgba(243, 248, 255, 0.98), rgba(233, 242, 255, 0.95));
}

.notification-list span,
.notification-list small {
  color: #53627d;
  font-size: 12px;
}

.notification-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.notification-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 255, 0.08);
  color: #2457c5;
  font-size: 12px;
  font-weight: 800;
}

.notification-list strong {
  font-size: 14px;
  line-height: 1.45;
}

.notification-list button > span {
  line-height: 1.55;
}

.timeline-panel {
  min-width: 0;
  height: 100%;
  padding: 22px;
  overflow: auto;
}

.timeline-header {
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid rgba(85, 131, 255, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  box-shadow: 0 14px 34px rgba(37, 87, 197, 0.08);
  animation: timeline-header-in 0.42s ease both;
}

.timeline-title-block {
  flex: 1;
  min-width: 0;
}

.timeline-tools {
  display: flex;
  align-items: center;
  gap: 10px;
}

.owner-avatar-trigger {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.notification-trigger {
  position: relative;
}

.notification-trigger span {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #ff5f6d;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.moment-item {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
  padding: 18px;
  border: 1px solid rgba(85, 131, 255, 0.14);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 42px rgba(37, 87, 197, 0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
  animation: moment-rise-in 0.34s ease both;
}

.moment-item:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.24);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 48px rgba(37, 87, 197, 0.12);
}

.moment-item.highlighted {
  border-color: rgba(37, 99, 255, 0.34);
  box-shadow:
    0 0 0 1px rgba(37, 99, 255, 0.14),
    0 24px 52px rgba(37, 87, 197, 0.16);
  animation: moment-highlight-pulse 1.5s ease;
}

.moment-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-button {
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
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
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.moment-media-grid button:hover {
  transform: translateY(-2px) scale(1.01);
  border-color: rgba(85, 131, 255, 0.32);
  box-shadow: 0 14px 28px rgba(37, 87, 197, 0.14);
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
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 0;
  border-radius: 999px;
  background: #edf4ff;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
  font-weight: 800;
  transition: transform 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.danger-action {
  background: #ffe8e8;
  color: #9b2626;
}

.comments-panel {
  display: grid;
  gap: 10px;
  background: #f7f9fc;
  border-radius: 16px;
  padding: 12px;
  animation: comment-panel-in 0.26s ease both;
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
  align-items: flex-start;
}

.comment-form {
  align-items: stretch;
}

.comment-row {
  color: #53627d;
  animation: comment-row-in 0.28s ease both;
}

.comment-row.nested {
  margin-left: 28px;
}

.comment-stack {
  display: grid;
  gap: 6px;
  flex: 1;
}

.comment-composer {
  display: grid;
  gap: 10px;
  flex: 1;
  padding: 12px;
  border: 1px solid rgba(85, 131, 255, 0.12);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 28px rgba(37, 87, 197, 0.06);
}

.comment-form textarea {
  min-height: 88px;
  resize: vertical;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f7fbff 100%);
}

.comment-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.reply-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 255, 0.08);
  color: #2457c5;
  font-size: 12px;
  font-weight: 700;
}

.reply-banner button {
  border: 0;
  background: transparent;
  color: #9b2626;
  cursor: pointer;
}

.comment-bubble {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid rgba(85, 131, 255, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 24px rgba(37, 87, 197, 0.06);
}

.comment-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
}

.comment-bubble p {
  margin: 0;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.comment-reply-target {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  max-width: 100%;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 255, 0.08);
  color: #2457c5;
  font-size: 12px;
  font-weight: 700;
}

.comment-meta {
  display: flex;
  gap: 10px;
  padding-inline: 4px;
}

.comment-author {
  color: #172033;
  font-weight: 700;
}

.comment-head small {
  color: #748198;
}

.comment-meta button {
  border: 0;
  background: transparent;
  color: #9b2626;
  cursor: pointer;
}

.comment-row.nested .comment-bubble {
  border-left: 3px solid rgba(37, 99, 255, 0.22);
}

.comment-row:nth-child(1) {
  animation-delay: 0.02s;
}

.comment-row:nth-child(2) {
  animation-delay: 0.05s;
}

.comment-row:nth-child(3) {
  animation-delay: 0.08s;
}

.comment-row:nth-child(4) {
  animation-delay: 0.11s;
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
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.hidden-file-input {
  display: none;
}

.floating-compose-btn {
  position: absolute;
  right: 28px;
  bottom: 28px;
  z-index: 6;
  display: grid;
  width: 58px;
  height: 58px;
  border: 0;
  border-radius: 22px;
  background: linear-gradient(135deg, #2563ff, #63b3ff);
  color: #fff;
  cursor: pointer;
  place-items: center;
  box-shadow: 0 20px 40px rgba(37, 99, 255, 0.32);
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.floating-compose-btn:hover {
  transform: translateY(-3px) scale(1.03);
  box-shadow: 0 26px 52px rgba(37, 99, 255, 0.38);
  filter: saturate(1.04);
}

.drawer-backdrop {
  position: absolute;
  inset: 0;
  z-index: 10;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 30, 56, 0.24);
  backdrop-filter: blur(8px);
}

.moment-drawer {
  display: grid;
  width: min(460px, 94vw);
  height: 100%;
  align-content: start;
  gap: 16px;
  overflow: auto;
  border-left: 1px solid rgba(85, 131, 255, 0.2);
  background: linear-gradient(180deg, #ffffff 0%, #f4f8ff 100%);
  padding: 18px;
  box-shadow: -24px 0 60px rgba(22, 54, 118, 0.18);
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.drawer-head h3 {
  margin: 0;
  font-size: 22px;
}

.drawer-section {
  display: grid;
  gap: 12px;
}

.media-picker-button {
  display: grid;
  gap: 6px;
  justify-items: center;
  border: 1px dashed rgba(37, 99, 255, 0.35);
  border-radius: 22px;
  background: linear-gradient(135deg, #f7fbff, #edf5ff);
  color: #1f4fc4;
  cursor: pointer;
  padding: 20px;
  text-align: center;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.media-picker-button > span {
  display: grid;
  width: 48px;
  height: 48px;
  border-radius: 18px;
  background: #fff;
  place-items: center;
  box-shadow: 0 12px 26px rgba(37, 99, 255, 0.14);
}

.media-picker-button small {
  color: #748198;
}

.profile-hero-card,
.drawer-action {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(85, 131, 255, 0.16);
  border-radius: 20px;
  background: #fff;
  color: #172033;
  cursor: pointer;
  padding: 14px;
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.profile-hero-card {
  display: grid;
  justify-items: center;
  padding: 22px;
  text-align: center;
}

.moment-actions button:hover,
.icon-button:hover,
.danger-action:hover,
.drawer-action:hover,
.profile-hero-card:hover,
.notification-list button:hover,
.report-list article:hover,
.media-picker-button:hover,
.load-more:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(37, 87, 197, 0.12);
}

.moment-item:nth-child(1) {
  animation-delay: 0.02s;
}

.moment-item:nth-child(2) {
  animation-delay: 0.06s;
}

.moment-item:nth-child(3) {
  animation-delay: 0.1s;
}

.moment-item:nth-child(4) {
  animation-delay: 0.14s;
}

@keyframes moment-rise-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes timeline-header-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes comment-panel-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes comment-row-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes moment-highlight-pulse {
  0% {
    box-shadow:
      0 0 0 0 rgba(37, 99, 255, 0.22),
      0 18px 44px rgba(37, 87, 197, 0.12);
  }

  50% {
    box-shadow:
      0 0 0 10px rgba(37, 99, 255, 0.05),
      0 28px 56px rgba(37, 87, 197, 0.18);
  }

  100% {
    box-shadow:
      0 0 0 1px rgba(37, 99, 255, 0.14),
      0 24px 52px rgba(37, 87, 197, 0.16);
  }
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






