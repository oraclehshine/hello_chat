import http from './http'
import type { ApiResponse, PageResponse } from './chat'

export interface GroupSummary {
  groupId: number
  ownerId: number
  groupName: string
  description: string | null
  avatarUrl: string | null
  notice: string | null
  status: number
  inviteCode: string | null
  chatEnabled: number
  recallLimitMinutes: number
  memberCount: number
  unreadCount: number
  mentionUnreadCount: number
  noticeUnread: boolean
  noticeUpdatedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface GroupMember {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  role: number
  groupNickname: string | null
  muteUntil: string | null
  joinedAt: string
}

export interface GroupMessage {
  messageId: number
  groupId: number
  senderId: number
  senderNickname: string
  senderAvatarUrl: string | null
  messageType: 'text' | 'image' | 'file'
  content: string
  fileId: number | null
  replyToMessageId: number | null
  replyPreview: string | null
  mentionUserIds: number[]
  fileName: string | null
  fileMimeType: string | null
  fileSize: number | null
  mentionAll: number
  recallStatus: number
  sentAt: string
  updatedAt: string
}

export interface GroupJoinRequest {
  requestId: number
  groupId: number
  requesterId: number
  requesterEmail: string
  requesterNickname: string
  requesterAvatarUrl: string | null
  message: string | null
  status: number
  createdAt: string
}

export interface GroupNotification {
  notificationId: number
  groupId: number
  actorId: number | null
  targetUserId: number | null
  noticeType: string
  content: string
  createdAt: string
}

export interface GroupNoticeReadStats {
  readCount: number
  memberCount: number
}

export async function createGroup(groupName: string, description: string, memberIds: number[], avatarUrl?: string) {
  const response = await http.post<ApiResponse<GroupSummary>>('/groups', {
    groupName,
    description,
    memberIds,
    avatarUrl,
  })
  return response.data.data
}

export async function listGroups() {
  const response = await http.get<ApiResponse<GroupSummary[]>>('/groups')
  return response.data.data
}

export async function getGroup(groupId: number) {
  const response = await http.get<ApiResponse<GroupSummary>>(`/groups/${groupId}`)
  return response.data.data
}

export async function searchGroups(keyword: string, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<GroupSummary>>>('/groups/search', {
    params: { keyword, page, pageSize },
  })
  return response.data.data
}

export async function updateGroupProfile(
  groupId: number,
  payload: {
    groupName?: string
    description?: string
    avatarUrl?: string
    chatEnabled?: boolean
    recallLimitMinutes?: number
  },
) {
  const response = await http.put<ApiResponse<GroupSummary>>(`/groups/${groupId}`, payload)
  return response.data.data
}

export async function joinGroupByInviteCode(inviteCode: string) {
  const response = await http.post<ApiResponse<GroupSummary>>(`/groups/invite/${inviteCode}/join`)
  return response.data.data
}

export async function requestJoinGroup(groupId: number, message = '') {
  const response = await http.post<ApiResponse<GroupJoinRequest>>(`/groups/${groupId}/join-requests`, { message })
  return response.data.data
}

export async function listGroupJoinRequests(groupId: number) {
  const response = await http.get<ApiResponse<GroupJoinRequest[]>>(`/groups/${groupId}/join-requests`)
  return response.data.data
}

export async function listMyGroupJoinRequests() {
  const response = await http.get<ApiResponse<GroupJoinRequest[]>>('/groups/join-requests/me')
  return response.data.data
}

export async function approveGroupJoinRequest(groupId: number, requestId: number) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/join-requests/${requestId}/approve`)
  return response.data.data
}

export async function rejectGroupJoinRequest(groupId: number, requestId: number) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/join-requests/${requestId}/reject`)
  return response.data.data
}

export async function listGroupNotifications(groupId: number, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<GroupNotification>>>(`/groups/${groupId}/notifications`, {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function leaveGroup(groupId: number) {
  await http.post(`/groups/${groupId}/leave`)
}

export async function dissolveGroup(groupId: number) {
  await http.delete(`/groups/${groupId}`)
}

export async function transferGroupOwner(groupId: number, targetUserId: number) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/owner`, { targetUserId })
  return response.data.data
}

export async function listGroupMembers(groupId: number) {
  const response = await http.get<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members`)
  return response.data.data
}

export async function listGroupMessages(groupId: number, page = 1, pageSize = 30) {
  const response = await http.get<ApiResponse<PageResponse<GroupMessage>>>(`/groups/${groupId}/messages`, {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function listGroupFiles(groupId: number, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<GroupMessage>>>(`/groups/${groupId}/files`, {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function markGroupAsRead(groupId: number) {
  await http.post(`/groups/${groupId}/read`)
}

export async function searchGroupMessages(groupId: number, keyword: string, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<GroupMessage>>>(`/groups/${groupId}/messages/search`, {
    params: { keyword, page, pageSize },
  })
  return response.data.data
}

export async function sendGroupMessage(
  groupId: number,
  content: string,
  messageType: GroupMessage['messageType'] = 'text',
  fileId?: number,
  mentionAll = false,
  replyToMessageId?: number,
  mentionUserIds: number[] = [],
) {
  const response = await http.post<ApiResponse<GroupMessage>>(`/groups/${groupId}/messages`, {
    messageType,
    content,
    fileId,
    mentionAll,
    replyToMessageId,
    mentionUserIds,
  })
  return response.data.data
}

export async function sendGroupMentionAllMessage(groupId: number, content: string, replyToMessageId?: number) {
  const response = await http.post<ApiResponse<GroupMessage>>(`/groups/${groupId}/messages/mention-all`, {
    messageType: 'text',
    content,
    replyToMessageId,
  })
  return response.data.data
}

export async function recallGroupMessage(groupId: number, messageId: number) {
  await http.post(`/groups/${groupId}/messages/${messageId}/recall`)
}

export async function deleteGroupMessage(groupId: number, messageId: number) {
  await http.delete(`/groups/${groupId}/messages/${messageId}`)
}

export async function addGroupMembers(groupId: number, memberIds: number[]) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members`, { memberIds })
  return response.data.data
}

export async function updateMyGroupNickname(groupId: number, nickname: string) {
  const response = await http.put<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members/me/nickname`, { nickname })
  return response.data.data
}

export async function removeGroupMember(groupId: number, userId: number) {
  await http.delete(`/groups/${groupId}/members/${userId}`)
}

export async function updateGroupAnnouncement(groupId: number, notice: string) {
  const response = await http.post<ApiResponse<GroupSummary>>(`/groups/${groupId}/announcement`, { notice })
  return response.data.data
}

export async function markGroupAnnouncementRead(groupId: number) {
  const response = await http.post<ApiResponse<GroupNoticeReadStats>>(`/groups/${groupId}/announcement/read`)
  return response.data.data
}

export async function getGroupAnnouncementReadStats(groupId: number) {
  const response = await http.get<ApiResponse<GroupNoticeReadStats>>(`/groups/${groupId}/announcement/read-stats`)
  return response.data.data
}

export async function setGroupAdmin(groupId: number, userId: number) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members/${userId}/admin`)
  return response.data.data
}

export async function unsetGroupAdmin(groupId: number, userId: number) {
  const response = await http.delete<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members/${userId}/admin`)
  return response.data.data
}

export async function muteGroupMember(groupId: number, userId: number, minutes: number) {
  const response = await http.post<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members/${userId}/mute`, { minutes })
  return response.data.data
}

export async function unmuteGroupMember(groupId: number, userId: number) {
  const response = await http.delete<ApiResponse<GroupMember[]>>(`/groups/${groupId}/members/${userId}/mute`)
  return response.data.data
}
