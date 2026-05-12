<template>
  <section class="group-workspace">
    <aside class="group-sidebar">
      <header class="group-header">
        <div>
          <h2>群聊</h2>
          <p>我已加入的群组会话</p>
        </div>
        <div class="group-header-actions">
          <button class="icon-button" :disabled="loadingGroups" title="刷新" @click="loadGroups">
            <SvgIcon name="refresh" />
          </button>
          <button class="icon-button" title="群组工具" @click="openGroupTools(groupToolMode)">
            <SvgIcon name="menu" />
          </button>
        </div>
      </header>

      <div class="group-list">
        <button
          v-for="group in groups"
          :key="group.groupId"
          class="group-item"
          :class="{ active: activeGroup?.groupId === group.groupId }"
          @click="selectGroup(group)"
        >
          <AvatarFrame :src="group.avatarUrl" :name="group.groupName" size="sm" />
          <span>
            <strong>{{ group.groupName }}</strong>
            <small>{{ group.memberCount }} 名成员</small>
          </span>
          <span v-if="group.mentionUnreadCount > 0" class="mention-badge">@{{ group.mentionUnreadCount }}</span>
          <span v-else-if="group.unreadCount > 0" class="unread-badge">{{ group.unreadCount > 99 ? '99+' : group.unreadCount }}</span>
        </button>
        <div v-if="!loadingGroups && !groups.length" class="empty-list">暂无群聊</div>
      </div>

      <div v-if="showGroupTools" class="group-tool-overlay" @click.self="showGroupTools = false">
        <aside class="group-tool-drawer">
          <header class="group-tool-head">
            <div>
              <h3>群组工具</h3>
              <p>加入群聊、查看申请和审核信息</p>
            </div>
            <button class="secondary-btn compact icon-only-btn" type="button" title="关闭" @click="showGroupTools = false">
              <SvgIcon name="close" />
            </button>
          </header>

          <div class="group-tool-switch" aria-label="群组工具">
            <button type="button" :class="{ active: groupToolMode === 'join' }" aria-label="加入群聊" @click="openGroupTools('join')">
              <SvgIcon name="group" />
              <span class="tool-tip">加入群聊</span>
            </button>
            <button type="button" :class="{ active: groupToolMode === 'mine' }" aria-label="我的申请" @click="openGroupTools('mine')">
              <SvgIcon name="inbox" />
              <span class="tool-tip">我的申请</span>
            </button>
            <button type="button" :class="{ active: groupToolMode === 'audit' }" aria-label="审核信息" @click="openGroupTools('audit')">
              <SvgIcon name="check" />
              <span class="tool-tip">审核信息</span>
            </button>
          </div>

          <section v-if="groupToolMode === 'join'" class="tool-card">
            <h4>加入或创建群聊</h4>
            <div class="member-picker">
              <input v-model="groupKeyword" placeholder="搜索群组" @keyup.enter.prevent="handleGroupSearch" />
              <button type="button" class="secondary-btn compact" :disabled="searchingGroups" @click="handleGroupSearch">
                {{ searchingGroups ? '...' : '搜索' }}
              </button>
            </div>
            <div class="member-picker">
              <input v-model="inviteCodeDraft" placeholder="邀请码" @keyup.enter.prevent="joinByInvite" />
              <button type="button" class="secondary-btn compact" @click="joinByInvite">加入</button>
            </div>
            <div v-if="groupResults.length" class="user-results compact-results">
              <button v-for="group in groupResults" :key="group.groupId" type="button" @click="requestJoin(group)">
                <AvatarFrame :src="group.avatarUrl" :name="group.groupName" size="sm" />
                <span>
                  <strong>{{ group.groupName }}</strong>
                  <small>{{ group.memberCount }} 名成员 | 申请加入</small>
                </span>
              </button>
            </div>

            <form class="create-group tool-stack" @submit.prevent="handleCreateGroup">
              <h4>创建群聊</h4>
              <input v-model="newGroupName" maxlength="30" placeholder="群名称" />
              <textarea v-model="newGroupDescription" rows="2" placeholder="群描述"></textarea>
              <div class="member-picker">
                <input v-model="userKeyword" placeholder="搜索要添加的用户" @keyup.enter.prevent="handleUserSearch" />
                <button type="button" class="secondary-btn compact" :disabled="searchingUsers" @click="handleUserSearch">
                  {{ searchingUsers ? '...' : '搜索' }}
                </button>
              </div>
              <div v-if="userResults.length" class="user-results compact-results">
                <button v-for="user in userResults" :key="user.userId" type="button" @click="toggleMember(user)">
                  <AvatarFrame :name="user.nickname || user.email" size="sm" />
                  <span>
                    <strong>{{ user.nickname || user.email }}</strong>
                    <small>{{ selectedMemberIds.includes(user.userId) ? '已选择' : user.email }}</small>
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
                {{ creatingGroup ? '创建中' : '创建群聊' }}
              </button>
            </form>
          </section>

          <section v-else-if="groupToolMode === 'mine'" class="tool-card">
            <h4>我的申请</h4>
            <button type="button" class="secondary-btn compact" :disabled="loadingMyJoinRequests" @click="loadMyJoinRequests">
              {{ loadingMyJoinRequests ? '加载中' : '刷新申请' }}
            </button>
            <div v-if="myJoinRequests.length" class="request-status-list">
              <p v-for="request in myJoinRequests" :key="request.requestId">
                <strong>群聊 #{{ request.groupId }}</strong>
                <small>{{ joinRequestStatus(request.status) }}</small>
              </p>
            </div>
            <div v-else class="empty-inline">暂无申请记录</div>
          </section>

          <section v-else class="tool-card">
            <h4>审核信息</h4>
            <template v-if="activeGroup && canManageGroup">
              <button class="secondary-btn compact" :disabled="loadingJoinRequests" @click="loadJoinRequests">
                {{ loadingJoinRequests ? '加载中' : '加载审核' }}
              </button>
              <div v-if="joinRequests.length" class="member-list">
                <div v-for="request in joinRequests" :key="request.requestId" class="member-row">
                  <AvatarFrame :src="request.requesterAvatarUrl" :name="request.requesterNickname || request.requesterEmail" size="sm" />
                  <span>
                    <strong>{{ request.requesterNickname || request.requesterEmail }}</strong>
                    <small>{{ request.message || '无附言' }}</small>
                  </span>
                  <span class="member-actions">
                    <button type="button" @click="reviewJoin(request, true)">同意</button>
                    <button class="danger" type="button" @click="reviewJoin(request, false)">拒绝</button>
                  </span>
                </div>
              </div>
              <div v-else class="empty-inline">当前群暂无待审核申请</div>
            </template>
            <template v-else>
              <button class="secondary-btn compact" :disabled="loadingNotifications" @click="loadNotifications">
                {{ loadingNotifications ? '加载中' : '加载通知' }}
              </button>
              <div v-if="notifications.length" class="notification-list">
                <p v-for="item in notifications" :key="item.notificationId">
                  <strong>{{ item.content }}</strong>
                  <small>{{ formatTime(item.createdAt) }}</small>
                </p>
              </div>
              <div v-else class="empty-inline">
                {{ activeGroup ? '当前没有审核通知' : '先选择一个群聊，再查看审核信息' }}
              </div>
            </template>
          </section>
        </aside>
      </div>
    </aside>

    <main class="group-panel">
      <header v-if="activeGroup" class="chat-header">
        <AvatarFrame :src="activeGroup.avatarUrl" :name="activeGroup.groupName" size="md" />
        <div>
          <h2>{{ activeGroup.groupName }}</h2>
          <p>{{ activeGroup.memberCount }} 名成员</p>
        </div>
      </header>

      <div v-if="notice" class="notice error">{{ notice }}</div>

      <div v-if="!activeGroup" class="empty-group">
        <h2>选择一个群聊</h2>
        <p>创建或选择一个群，开始群聊。</p>
      </div>

      <div v-else class="group-chat">
        <aside class="member-panel">
          <section class="profile-box">
            <button class="section-toggle" type="button" @click="togglePanel('profile')">
              <span>群资料</span>
              <SvgIcon name="chevron" :class="{ open: openPanels.profile }" />
            </button>
            <div v-if="openPanels.profile" class="section-body">
              <div class="profile-avatar-row">
                <AvatarFrame :src="profileAvatarPreview || activeGroup.avatarUrl" :name="activeGroup.groupName" size="lg" />
                <button
                  v-if="editingProfile"
                  type="button"
                  class="secondary-btn compact"
                  :disabled="savingProfile"
                  @click="groupAvatarInput?.click()"
                >
                  更换头像
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
                <input v-model="profileNameDraft" maxlength="30" placeholder="群名称" />
                <textarea v-model="profileDescriptionDraft" rows="3" maxlength="255" placeholder="群描述"></textarea>
                <label class="inline-setting">
                  <input v-model="profileChatEnabledDraft" type="checkbox" />
                  允许发言
                </label>
                <input v-model.number="profileRecallLimitDraft" type="number" min="0" max="1440" placeholder="撤回分钟数" />
                <div class="panel-actions split-actions">
                  <button class="secondary-btn compact" :disabled="savingProfile" @click="cancelEditGroupProfile">取消</button>
                  <button class="primary-btn compact" :disabled="savingProfile || !profileNameDraft.trim()" @click="saveGroupProfile">
                    {{ savingProfile ? '保存中' : '保存' }}
                  </button>
                </div>
              </template>
              <template v-else>
                <strong>{{ activeGroup.groupName }}</strong>
                <p>{{ activeGroup.description || '暂无描述' }}</p>
                <p>邀请码： {{ activeGroup.inviteCode || '-' }}</p>
                <p>发言： {{ activeGroup.chatEnabled === 1 ? '开启' : '关闭' }} | 撤回： {{ activeGroup.recallLimitMinutes }} 分钟</p>
                <div class="panel-actions split-actions">
                  <button v-if="canManageGroup" class="secondary-btn compact" @click="startEditGroupProfile">编辑</button>
                  <button v-if="!isOwner" class="secondary-btn compact danger-action" @click="leaveActiveGroup">退出群聊</button>
                  <button v-else class="secondary-btn compact danger-action" @click="dissolveActiveGroup">解散群聊</button>
                </div>
              </template>
            </div>
          </section>

          <section class="announcement-box">
            <button class="section-toggle" type="button" @click="togglePanel('notice')">
              <span>群公告</span>
              <SvgIcon name="chevron" :class="{ open: openPanels.notice }" />
            </button>
            <div v-if="openPanels.notice" class="section-body">
              <div v-if="activeGroup.noticeUnread" class="notice-unread">未读公告</div>
              <p v-if="!editingNotice">{{ activeGroup.notice || '暂无公告' }}</p>
              <textarea v-else v-model="noticeDraft" rows="4" maxlength="1000"></textarea>
              <small v-if="noticeReadStats">{{ noticeReadStats.readCount }}/{{ noticeReadStats.memberCount }} 已读</small>
              <div class="panel-actions">
                <button class="secondary-btn compact" :disabled="savingNotice" @click="markNoticeRead">
                  标记已读
                </button>
                <button v-if="!editingNotice && canManageGroup" class="secondary-btn compact" @click="startEditNotice">编辑</button>
                <button v-else class="primary-btn compact" :disabled="savingNotice" @click="saveNotice">
                  {{ savingNotice ? '保存中' : '保存' }}
                </button>
              </div>
            </div>
          </section>

          <section class="invite-box">
            <button class="section-toggle" type="button" @click="togglePanel('files')">
              <span>文件</span>
              <SvgIcon name="chevron" :class="{ open: openPanels.files }" />
            </button>
            <div v-if="openPanels.files" class="section-body">
              <button class="secondary-btn compact" :disabled="loadingFiles" @click="loadFiles">
                {{ loadingFiles ? '加载中' : '加载文件' }}
              </button>
              <div v-if="groupFiles.length" class="file-list">
                <a v-for="file in groupFiles" :key="file.messageId" :href="resolveAssetUrl(file.content)" target="_blank" rel="noreferrer">
                  <strong>{{ file.fileName || fileNameFromUrl(file.content) }}</strong>
                  <small>{{ fileMeta(file) }}</small>
                </a>
              </div>
            </div>
          </section>

          <section class="invite-box" v-if="canManageGroup">
            <button class="section-toggle" type="button" @click="togglePanel('invite')">
              <span>邀请成员</span>
              <SvgIcon name="chevron" :class="{ open: openPanels.invite }" />
            </button>
            <div v-if="openPanels.invite" class="section-body">
              <div class="member-picker">
                <input v-model="inviteKeyword" placeholder="搜索用户" @keyup.enter.prevent="handleInviteSearch" />
                <button type="button" class="secondary-btn compact" :disabled="searchingInvites" @click="handleInviteSearch">
                  {{ searchingInvites ? '...' : '搜索' }}
                </button>
              </div>
              <div v-if="inviteResults.length" class="user-results compact-results">
                <button v-for="user in inviteResults" :key="user.userId" type="button" @click="inviteUser(user.userId)">
                  <AvatarFrame :name="user.nickname || user.email" size="sm" />
                  <span>
                    <strong>{{ user.nickname || user.email }}</strong>
                    <small>{{ user.email }}</small>
                  </span>
                </button>
              </div>
            </div>
          </section>

          <section class="invite-box">
            <button class="section-toggle" type="button" @click="togglePanel('members')">
              <span>成员</span>
              <SvgIcon name="chevron" :class="{ open: openPanels.members }" />
            </button>
            <div v-if="openPanels.members" class="section-body">
              <div class="member-list">
                <div v-for="member in members" :key="member.userId" class="member-row">
                  <AvatarFrame :src="member.avatarUrl" :name="member.nickname || member.email" size="sm" />
                  <span>
                    <template v-if="editingNickname && member.userId === currentUserId">
                      <input
                        v-model="nicknameDraft"
                        class="nickname-input"
                        maxlength="64"
                        placeholder="群昵称"
                        @keyup.enter="saveMyNickname"
                      />
                    </template>
                    <strong v-else>{{ member.groupNickname || member.nickname || member.email }}</strong>
                    <small>{{ memberStatus(member) }}</small>
                  </span>
                  <span v-if="member.userId === currentUserId" class="member-actions">
                    <button v-if="!editingNickname" type="button" @click="startEditNickname(member)">群昵称</button>
                    <template v-else>
                      <button type="button" :disabled="savingNickname" @click="saveMyNickname">
                        {{ savingNickname ? '保存中' : '保存' }}
                      </button>
                      <button type="button" :disabled="savingNickname" @click="cancelEditNickname">取消</button>
                    </template>
                  </span>
                  <span v-if="canManageMember(member)" class="member-actions">
                    <button v-if="canToggleAdmin(member)" type="button" @click="toggleAdmin(member)">
                      {{ member.role === 2 ? '取消管理员' : '设为管理员' }}
                    </button>
                    <button type="button" @click="toggleMute(member)">
                      {{ isMuted(member) ? '解除禁言' : '禁言' }}
                    </button>
                    <button v-if="canTransferOwner(member)" type="button" @click="transferOwnerTo(member)">转让</button>
                    <button class="danger" type="button" @click="removeMemberFromGroup(member)">移除</button>
                  </span>
                </div>
              </div>
            </div>
          </section>
        </aside>

        <section class="message-column">
          <div class="message-search">
            <input
              v-model="messageKeyword"
              type="search"
              placeholder="搜索群消息"
              @keyup.enter="handleMessageSearch"
            />
            <button class="secondary-btn compact" :disabled="searchingMessages" @click="handleMessageSearch">
              {{ searchingMessages ? '...' : '搜索' }}
            </button>
            <button v-if="messageKeyword" class="secondary-btn compact" @click="clearMessageSearch">清空</button>
          </div>
          <div ref="messageListRef" class="message-list">
            <div v-if="loadingMessages" class="message-state">消息加载中...</div>
            <div v-else-if="!orderedMessages.length" class="message-state">
              {{ messageKeyword ? '没有匹配的消息' : '发送第一条群消息' }}
            </div>
            <button
              v-if="!loadingMessages && hasMoreMessages"
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
              @contextmenu.prevent="openGroupMessageMenu($event, message)"
            >
              <AvatarFrame
                :src="message.senderId === currentUserId ? currentUserAvatarUrl : memberAvatar(message.senderId)"
                :name="message.senderId === currentUserId ? currentUserDisplayName : message.senderNickname || memberName(message.senderId)"
                size="sm"
              />
              <div class="message-stack">
                <div v-if="message.senderId !== currentUserId" class="message-label">
                  {{ message.senderNickname || memberName(message.senderId) }}
                </div>
                <div class="message-bubble">
                  <p v-if="message.recallStatus === 1" class="recalled">消息已撤回</p>
                  <template v-else>
                    <div v-if="message.replyPreview" class="reply-preview">{{ message.replyPreview }}</div>
                    <template v-if="message.messageType === 'image'">
                      <button class="image-preview-button" @click="previewImageUrl = resolveAssetUrl(message.content)">
                        <img class="message-image" :src="resolveAssetUrl(message.content)" alt="group image" />
                      </button>
                    </template>
                    <template v-else-if="message.messageType === 'file'">
                      <a class="file-card" :href="resolveAssetUrl(message.content)" target="_blank" rel="noreferrer">
                        <span class="file-icon">文件</span>
                        <span class="file-info">
                          <strong>{{ message.fileName || fileNameFromUrl(message.content) }}</strong>
                          <small>{{ fileMeta(message) }}</small>
                        </span>
                      </a>
                    </template>
                    <p v-else>{{ message.content }}</p>
                  </template>
                </div>
                <footer class="message-meta">
                  <span v-if="message.mentionAll" class="mention-tag">@All</span>
                  <span v-if="message.mentionUserIds.length" class="mention-tag">@{{ message.mentionUserIds.length }}</span>
                  {{ formatTime(message.sentAt) }}
                </footer>
              </div>
            </article>
          </div>

          <form class="composer" @submit.prevent="handleSend">
            <div v-if="replyTarget" class="reply-compose">
              正在回复： {{ replyTarget.content || replyTarget.fileName || replyTarget.messageType }}
              <button type="button" @click="replyTarget = null">x</button>
            </div>
            <textarea
              v-model="draft"
              maxlength="1000"
              rows="1"
              placeholder="输入群消息"
              @keydown.enter.exact.prevent="handleSend"
            ></textarea>
            <input ref="imageInput" class="hidden-file-input" type="file" accept="image/*" @change="handleImageSelect" />
            <input ref="fileInput" class="hidden-file-input" type="file" @change="handleFileSelect" />
            <div class="composer-toolbar">
              <div class="composer-tools">
                <div class="mention-menu-shell">
                  <button
                    type="button"
                    class="secondary-btn compact icon-only-btn composer-icon"
                    :class="{ selected: mentionAllDraft || mentionUserIdsDraft.length }"
                    :disabled="sending"
                    title="提及成员"
                    @click="showMentionMenu = !showMentionMenu"
                  >
                    <SvgIcon name="at" />
                  </button>
                  <div v-if="showMentionMenu" class="mention-menu-popover">
                    <button
                      type="button"
                      class="mention-menu-item"
                      :class="{ active: mentionAllDraft }"
                      :disabled="!canMentionAll"
                      @click="mentionAllDraft = !mentionAllDraft"
                    >
                      <SvgIcon name="group" />
                      <span>@All</span>
                    </button>
                    <button type="button" class="mention-menu-item" @click="chooseMentions(); showMentionMenu = false">
                      <SvgIcon name="profile" />
                      <span>@成员</span>
                    </button>
                  </div>
                </div>
                <button type="button" class="secondary-btn compact icon-only-btn composer-icon" :disabled="sending" title="发送图片" @click="imageInput?.click()">
                  <SvgIcon name="image" />
                </button>
                <button type="button" class="secondary-btn compact icon-only-btn composer-icon" :disabled="sending" title="发送文件" @click="fileInput?.click()">
                  <SvgIcon name="file" />
                </button>
              </div>
              <span v-if="mentionAllDraft || mentionUserIdsDraft.length" class="composer-status">
                {{ mentionAllDraft ? '@All 已启用' : `已提及 ${mentionUserIdsDraft.length} 人` }}
              </span>
              <button class="primary-btn compact send-btn" :disabled="sending || !draft.trim()">
                <SvgIcon name="send" />
                <span>{{ sending ? '发送中' : '发送' }}</span>
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>

    <button v-if="previewImageUrl" class="image-lightbox" @click="previewImageUrl = ''">
      <img :src="previewImageUrl" alt="preview" />
    </button>

    <div
      v-if="groupMessageMenu.visible && groupMessageMenu.message"
      class="message-context-menu"
      :style="{ left: `${groupMessageMenu.x}px`, top: `${groupMessageMenu.y}px` }"
    >
      <button type="button" @click="runGroupMessageAction(() => replyTo(groupMessageMenu.message!))">
        回复
      </button>
      <button
        v-if="groupMessageMenu.message.senderId === currentUserId && groupMessageMenu.message.recallStatus === 0"
        type="button"
        @click="runGroupMessageAction(() => handleRecall(groupMessageMenu.message!.messageId))"
      >
        撤回
      </button>
      <button type="button" class="danger-item" @click="runGroupMessageAction(() => handleDelete(groupMessageMenu.message!.messageId))">
        删除
      </button>
    </div>
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
import AvatarFrame from '../components/AvatarFrame.vue'
import SvgIcon from '../components/SvgIcon.vue'
import { resolveAssetUrl } from '../utils/assets'

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
const currentUserDisplayName = ref('我')
const currentUserAvatarUrl = ref('')
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
const showGroupTools = ref(false)
const groupToolMode = ref<'join' | 'mine' | 'audit'>('join')
const showMentionMenu = ref(false)
const openPanels = ref({
  profile: true,
  notice: true,
  files: false,
  invite: false,
  members: true,
})
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
const groupMessageMenuWidth = 172
const groupMessageMenuHeight = 196
const groupMessageMenu = ref<{ visible: boolean; x: number; y: number; message: GroupMessage | null }>({
  visible: false,
  x: 0,
  y: 0,
  message: null,
})

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
    const data = JSON.parse(authData)
    currentUserId.value = data.userId
    currentUserDisplayName.value = data.nickname || data.email || '我'
    currentUserAvatarUrl.value = data.avatarUrl || ''
  }
  window.addEventListener('pointerdown', handleGlobalPointerDown)
  window.addEventListener('keydown', handleEscapeClose)
  window.addEventListener('scroll', closeGroupMessageMenu, true)
  connectSocket()
  await Promise.all([loadGroups(), loadFriends()])
})

async function openGroupTools(mode: 'join' | 'mine' | 'audit') {
  groupToolMode.value = mode
  showGroupTools.value = true
  if (mode === 'mine') {
    await loadMyJoinRequests()
    return
  }
  if (mode === 'audit') {
    if (activeGroup.value && canManageGroup.value) {
      await loadJoinRequests()
    } else {
      await loadNotifications()
    }
  }
}

function togglePanel(panel: keyof typeof openPanels.value) {
  openPanels.value[panel] = !openPanels.value[panel]
}

function openGroupMessageMenu(event: MouseEvent, message: GroupMessage) {
  const padding = 12
  const maxX = Math.max(padding, window.innerWidth - groupMessageMenuWidth - padding)
  const maxY = Math.max(padding, window.innerHeight - groupMessageMenuHeight - padding)
  groupMessageMenu.value = {
    visible: true,
    x: Math.min(event.clientX, maxX),
    y: Math.min(event.clientY, maxY),
    message,
  }
}

function closeGroupMessageMenu() {
  groupMessageMenu.value.visible = false
  groupMessageMenu.value.message = null
}

function handleGlobalPointerDown(event: PointerEvent) {
  const target = event.target
  if (groupMessageMenu.value.visible) {
    if (target instanceof Element && target.closest('.message-context-menu')) return
    closeGroupMessageMenu()
  }
  if (showMentionMenu.value) {
    if (target instanceof Element && target.closest('.mention-menu-shell')) return
    showMentionMenu.value = false
  }
}

function handleEscapeClose(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeGroupMessageMenu()
    showMentionMenu.value = false
  }
}

async function runGroupMessageAction(action: () => Promise<void> | void) {
  closeGroupMessageMenu()
  await action()
}

onBeforeUnmount(() => {
  socket.value?.close()
  window.removeEventListener('pointerdown', handleGlobalPointerDown)
  window.removeEventListener('keydown', handleEscapeClose)
  window.removeEventListener('scroll', closeGroupMessageMenu, true)
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
        notice.value = '群聊已解散'
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
    notice.value = error instanceof Error ? error.message : '加载群聊失败'
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
    notice.value = error instanceof Error ? error.message : '搜索消息失败'
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
    notice.value = error instanceof Error ? error.message : '加载更早消息失败'
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
    notice.value = error instanceof Error ? error.message : '搜索用户失败'
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
    notice.value = error instanceof Error ? error.message : '搜索群聊失败'
  } finally {
    searchingGroups.value = false
  }
}

async function requestJoin(group: GroupSummary) {
  const message = window.prompt(`申请加入 ${group.groupName}`, '')
  if (message === null) return
  try {
    await requestJoinGroup(group.groupId, message)
    await loadMyJoinRequests()
    notice.value = '入群申请已发送'
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '入群申请失败'
  }
}

async function loadMyJoinRequests() {
  loadingMyJoinRequests.value = true
  try {
    myJoinRequests.value = await listMyGroupJoinRequests()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载我的申请失败'
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
    notice.value = error instanceof Error ? error.message : '通过邀请码入群失败'
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
    notice.value = error instanceof Error ? error.message : '搜索邀请成员失败'
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
    notice.value = error instanceof Error ? error.message : '邀请成员失败'
  }
}

async function loadJoinRequests() {
  if (!activeGroup.value) return
  loadingJoinRequests.value = true
  try {
    joinRequests.value = await listGroupJoinRequests(activeGroup.value.groupId)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载入群申请失败'
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
    notice.value = error instanceof Error ? error.message : '处理入群申请失败'
  }
}

async function loadNotifications() {
  if (!activeGroup.value) return
  loadingNotifications.value = true
  try {
    const page = await listGroupNotifications(activeGroup.value.groupId)
    notifications.value = page.list
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '加载通知失败'
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
    notice.value = error instanceof Error ? error.message : '加载文件失败'
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
    notice.value = error instanceof Error ? error.message : '创建群聊失败'
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
    notice.value = error instanceof Error ? error.message : '发送消息失败'
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
    '输入要提及的成员 ID，多个用逗号分隔',
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
    notice.value = error instanceof Error ? error.message : '图片上传失败'
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
    notice.value = error instanceof Error ? error.message : '文件上传失败'
  } finally {
    sending.value = false
  }
}

async function handleRecall(messageId: number) {
  if (!activeGroup.value) return
  try {
    if (!window.confirm('确认撤回这条消息吗？')) return
    await recallGroupMessage(activeGroup.value.groupId, messageId)
    if (messageKeyword.value.trim()) {
      await handleMessageSearch()
    } else {
      await loadMessages()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '撤回失败'
  }
}

async function handleDelete(messageId: number) {
  if (!activeGroup.value) return
  try {
    if (!window.confirm('确认在群聊视图中删除这条消息吗？')) return
    await deleteGroupMessage(activeGroup.value.groupId, messageId)
    if (messageKeyword.value.trim()) {
      await handleMessageSearch()
    } else {
      await loadMessages()
    }
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '删除失败'
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
    notice.value = error instanceof Error ? error.message : '保存公告失败'
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
    notice.value = error instanceof Error ? error.message : '标记公告失败'
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
    notice.value = error instanceof Error ? error.message : '群头像上传失败'
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
    notice.value = error instanceof Error ? error.message : '保存群资料失败'
  } finally {
    savingProfile.value = false
  }
}

async function leaveActiveGroup() {
  if (!activeGroup.value) return
  if (!window.confirm(`确认退出群聊 ${activeGroup.value.groupName} 吗？`)) return
  try {
    await leaveGroup(activeGroup.value.groupId)
    resetActiveGroup()
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '退出群聊失败'
  }
}

async function dissolveActiveGroup() {
  if (!activeGroup.value) return
  if (!window.confirm(`确认解散群聊 ${activeGroup.value.groupName} 吗？该操作不可撤销。`)) return
  try {
    await dissolveGroup(activeGroup.value.groupId)
    resetActiveGroup()
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '解散群聊失败'
  }
}

async function transferOwnerTo(member: GroupMember) {
  if (!activeGroup.value) return
  if (!window.confirm(`确认将群主转让给 ${member.nickname || member.email} 吗？`)) return
  try {
    members.value = await transferGroupOwner(activeGroup.value.groupId, member.userId)
    await loadGroups()
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '转让群主失败'
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
    notice.value = error instanceof Error ? error.message : '保存群昵称失败'
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
    notice.value = error instanceof Error ? error.message : '管理员操作失败'
  }
}

function isMuted(member: GroupMember) {
  return Boolean(member.muteUntil && new Date(member.muteUntil).getTime() > Date.now())
}

function memberStatus(member: GroupMember) {
  const status = [roleLabel(member.role)]
  if (isMuted(member)) {
    status.push(`禁言至 ${formatTime(member.muteUntil || '')}`)
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
    const rawMinutes = window.prompt('禁言时长（分钟）', '10')
    if (!rawMinutes) return
    const minutes = Number(rawMinutes)
    if (!Number.isFinite(minutes) || minutes < 1) {
      notice.value = '禁言时长无效'
      return
    }
    members.value = await muteGroupMember(activeGroup.value.groupId, member.userId, minutes)
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '禁言操作失败'
  }
}

async function removeMemberFromGroup(member: GroupMember) {
  if (!activeGroup.value) return
  if (!window.confirm(`确认将 ${member.nickname || member.email} 移出该群聊吗？`)) return
  try {
    await removeGroupMember(activeGroup.value.groupId, member.userId)
    await Promise.all([loadMembers(), loadGroups()])
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '移除成员失败'
  }
}

function roleLabel(role: number) {
  if (role === 3) return '群主'
  if (role === 2) return '管理员'
  return '成员'
}

function memberName(userId: number) {
  const member = members.value.find((item) => item.userId === userId)
  return member?.groupNickname || member?.nickname || member?.email || String(userId)
}

function memberAvatar(userId: number) {
  return members.value.find((item) => item.userId === userId)?.avatarUrl || ''
}

function joinRequestStatus(status: number) {
  if (status === 1) return '已同意'
  if (status === 2) return '已拒绝'
  return '待处理'
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
    return name ? decodeURIComponent(name) : '附件'
  } catch {
    return url.split('/').pop() || '附件'
  }
}

function fileMeta(message: GroupMessage) {
  const parts = []
  if (message.fileMimeType) parts.push(message.fileMimeType)
  if (message.fileSize != null) parts.push(formatFileSize(message.fileSize))
  return parts.length ? parts.join(' | ') : '打开附件'
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
  min-height: 100%;
  height: 100%;
  width: 100%;
  background:
    radial-gradient(circle at 12% 0%, rgba(79, 140, 255, 0.14), transparent 28%),
    linear-gradient(135deg, #f8fbff 0%, #eef5ff 42%, #ffffff 100%);
  border: 1px solid rgba(85, 131, 255, 0.18);
  border-radius: 28px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(37, 87, 197, 0.12);
}

.group-sidebar {
  border-right: 1px solid rgba(85, 131, 255, 0.14);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  min-height: 0;
  display: flex;
  flex-direction: column;
  animation: group-rise-in 0.44s ease both;
}

.group-header,
.chat-header {
  min-height: 76px;
  padding: 16px;
  border-bottom: 1px solid rgba(85, 131, 255, 0.14);
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(16px);
}

.group-header {
  justify-content: space-between;
}

.group-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
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
  border-bottom: 1px solid rgba(85, 131, 255, 0.14);
  animation: group-rise-in 0.48s ease both;
}

.tool-stack {
  padding: 0;
  border: 0;
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
  border-radius: 14px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.34);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  animation: group-rise-in 0.34s ease both;
}

.user-results button,
.group-item {
  cursor: pointer;
  position: relative;
}

.user-results button:hover,
.group-item:hover,
.group-item.active {
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 16px 34px rgba(37, 87, 197, 0.1);
  transform: translateY(-2px);
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
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(85, 131, 255, 0.14);
  padding: 8px 10px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.request-status-list p:hover {
  transform: translateY(-1px);
  border-color: rgba(85, 131, 255, 0.24);
  box-shadow: 0 12px 26px rgba(37, 87, 197, 0.08);
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
  border: 1px solid rgba(85, 131, 255, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
  color: #172033;
  padding: 10px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.file-list a:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.24);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 16px 34px rgba(37, 87, 197, 0.1);
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
  flex: 1;
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
  animation: group-rise-in 0.5s ease both;
  animation-delay: 0.06s;
}

.group-tool-overlay {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  justify-content: flex-start;
  background: rgba(15, 23, 42, 0.18);
}

.group-tool-drawer {
  display: grid;
  width: min(520px, 94vw);
  align-content: start;
  gap: 16px;
  overflow: auto;
  padding: 18px;
  background: linear-gradient(180deg, rgba(251, 253, 255, 0.96), rgba(241, 247, 255, 0.94));
  backdrop-filter: blur(22px);
  box-shadow: 18px 0 48px rgba(15, 23, 42, 0.14);
}

.group-tool-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.group-tool-head h3,
.group-tool-head p,
.tool-card h4,
.empty-inline {
  margin: 0;
}

.group-tool-head p,
.empty-inline {
  color: #53627d;
}

.group-tool-switch {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.group-tool-switch button {
  position: relative;
  display: grid;
  min-height: 82px;
  border: 1px solid rgba(85, 131, 255, 0.18);
  border-radius: 24px;
  background: #fff;
  color: #2f7eff;
  place-items: center;
  box-shadow: 0 14px 34px rgba(37, 87, 197, 0.08);
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.group-tool-switch button:hover,
.group-tool-switch button.active {
  background: linear-gradient(135deg, #eff6ff, #ffffff);
  box-shadow: 0 18px 42px rgba(37, 99, 255, 0.14);
  transform: translateY(-2px);
}

.group-tool-switch :deep(.ui-icon) {
  width: 30px;
  height: 30px;
}

.tool-tip {
  position: absolute;
  left: 50%;
  bottom: 10px;
  border-radius: 999px;
  background: #172033;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  opacity: 0;
  padding: 5px 9px;
  pointer-events: none;
  transform: translate(-50%, 6px);
  transition: opacity 0.16s ease, transform 0.16s ease;
  white-space: nowrap;
}

.group-tool-switch button:hover .tool-tip,
.group-tool-switch button:focus-visible .tool-tip {
  opacity: 1;
  transform: translate(-50%, 0);
}

.tool-card {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(85, 131, 255, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(37, 87, 197, 0.06);
}

.empty-group,
.empty-list,
.message-state {
  margin: auto;
  color: #748198;
  text-align: center;
}

.empty-inline {
  display: grid;
  min-height: 96px;
  border: 1px dashed rgba(85, 131, 255, 0.18);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.52);
  place-items: center;
  text-align: center;
  padding: 18px;
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
  border-left: 1px solid rgba(85, 131, 255, 0.14);
  background: rgba(255, 255, 255, 0.62);
  backdrop-filter: blur(18px);
  padding: 14px;
  overflow: auto;
}

.profile-box,
.announcement-box,
.invite-box {
  display: grid;
  gap: 8px;
  padding: 14px 14px 16px;
  margin-bottom: 14px;
  border: 1px solid rgba(85, 131, 255, 0.14);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14px 32px rgba(37, 87, 197, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease;
}

.profile-box:hover,
.announcement-box:hover,
.invite-box:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.24);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 20px 42px rgba(37, 87, 197, 0.1);
}

.member-panel h3 {
  margin: 0 0 10px;
  font-size: 14px;
}

.section-toggle {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  color: #172033;
  cursor: pointer;
  padding: 0;
  font-size: 14px;
  font-weight: 800;
}

.section-toggle :deep(.ui-icon) {
  width: 18px;
  height: 18px;
  transition: transform 0.18s ease;
}

.section-toggle :deep(.ui-icon.open) {
  transform: rotate(180deg);
}

.section-body {
  display: grid;
  gap: 8px;
  animation: section-fade-in 0.18s ease both;
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
  border-radius: 10px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 6px 8px;
  font-size: 12px;
  transition: transform 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.member-actions button:hover,
.icon-button:hover,
.load-more-messages:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(37, 87, 197, 0.12);
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
  padding: 10px 12px;
  border: 1px solid rgba(85, 131, 255, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.notification-list p:hover {
  transform: translateY(-1px);
  border-color: rgba(85, 131, 255, 0.22);
  box-shadow: 0 12px 26px rgba(37, 87, 197, 0.08);
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

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  max-width: min(700px, 94%);
  animation: group-rise-in 0.32s ease both;
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
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(85, 131, 255, 0.12);
  backdrop-filter: blur(16px);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.message-row.mine .message-bubble {
  background: rgba(223, 247, 239, 0.84);
}

.message-bubble:hover {
  transform: translateY(-1px);
  border-color: rgba(85, 131, 255, 0.22);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
}

.message-bubble p {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.message-meta {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
  align-items: center;
  color: #53627d;
  font-size: 12px;
}

.message-row.mine .message-meta {
  justify-content: flex-end;
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
  transition: transform 0.18s ease, filter 0.18s ease;
}

.image-preview-button:hover {
  transform: scale(1.02);
  filter: saturate(1.04);
}

.group-item:nth-child(1),
.member-row:nth-child(1),
.message-row:nth-child(1) {
  animation-delay: 0.02s;
}

.group-item:nth-child(2),
.member-row:nth-child(2),
.message-row:nth-child(2) {
  animation-delay: 0.05s;
}

.group-item:nth-child(3),
.member-row:nth-child(3),
.message-row:nth-child(3) {
  animation-delay: 0.08s;
}

.group-item:nth-child(4),
.member-row:nth-child(4),
.message-row:nth-child(4) {
  animation-delay: 0.11s;
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
  display: grid;
  gap: 12px;
  align-items: stretch;
  background: rgba(248, 251, 255, 0.72);
  backdrop-filter: blur(20px);
}

.composer textarea {
  width: 100%;
  min-height: 88px;
  max-height: 164px;
  resize: vertical;
  border-radius: 18px;
}

.composer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.composer-tools {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.composer-icon {
  width: 40px;
  min-width: 40px;
  border-radius: 12px;
}

.mention-menu-shell {
  position: relative;
}

.mention-menu-popover {
  position: absolute;
  left: 0;
  bottom: calc(100% + 10px);
  z-index: 12;
  display: grid;
  min-width: 140px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.34);
  border-radius: 16px;
  background: rgba(18, 28, 45, 0.9);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.24);
  animation: menu-fade-in 0.16s ease both;
}

.mention-menu-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: #f8fbff;
  cursor: pointer;
  padding: 10px 12px;
}

.mention-menu-item:hover,
.mention-menu-item.active {
  background: rgba(255, 255, 255, 0.12);
}

.composer-status {
  margin-left: auto;
  color: #53627d;
  font-size: 12px;
  font-weight: 700;
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
  border-radius: 12px;
  background: #e9eef6;
  color: #2457c5;
  cursor: pointer;
  padding: 8px 10px;
  transition: transform 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
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
  border-radius: 16px;
}

.message-context-menu {
  position: fixed;
  z-index: 60;
  display: grid;
  min-width: 156px;
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

@keyframes group-rise-in {
  from {
    opacity: 0;
    transform: translateY(18px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes section-fade-in {
  from {
    opacity: 0;
    transform: translateY(-4px);
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
    gap: 10px;
  }

  .composer-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .composer-tools,
  .composer-status,
  .send-btn {
    width: 100%;
  }

  .composer-tools {
    justify-content: space-between;
  }

  .composer-status {
    text-align: left;
  }
}
</style>





