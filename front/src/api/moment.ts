import http from './http'
import type { ApiResponse, PageResponse } from './chat'

export interface MomentUser {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  signature: string | null
}

export interface MomentMedia {
  fileId: number
  fileUrl: string
  fileName: string
  fileType: string
  mimeType: string
  fileSize: number
  sortOrder: number
}

export interface Moment {
  momentId: number
  authorId: number
  authorNickname: string
  authorAvatarUrl: string | null
  content: string
  location: string | null
  tags: string[]
  mood: string | null
  activity: string | null
  auditStatus: string
  auditReason: string | null
  visibility: string
  likeCount: number
  commentCount: number
  collectCount: number
  viewCount: number
  liked: boolean
  collected: boolean
  mediaList: MomentMedia[]
  editedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface MomentComment {
  commentId: number
  momentId: number
  userId: number
  userNickname: string
  userAvatarUrl: string | null
  replyToCommentId: number | null
  content: string
  createdAt: string
}

export interface MomentNotification {
  notificationId: number
  notificationType: string
  title: string
  content: string
  relatedId: number | null
  read: number
  createdAt: string
}

export interface MomentReport {
  reportId: number
  momentId: number
  reporterId: number
  reporterNickname: string
  authorNickname: string
  momentContent: string
  reason: string
  status: number
  handledBy: number | null
  handleNote: string | null
  handledAt: string | null
  createdAt: string
}

export interface MomentProfileSummary {
  userId: number
  nickname: string
  email: string
  avatarUrl: string | null
  momentCount: number
  totalLikeCount: number
  totalCommentCount: number
  totalCollectCount: number
  friendCount: number
  followerCount: number
  followingCount: number
}

export async function createMoment(payload: {
  content: string
  fileIds: number[]
  location?: string
  tags?: string[]
  mood?: string
  activity?: string
  visibility?: string
  visibleUserIds?: number[]
}) {
  const response = await http.post<ApiResponse<Moment>>('/moments', payload)
  return response.data.data
}

export async function listMoments(page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<Moment>>>('/moments', {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function listUserMoments(userId: number, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<Moment>>>(`/moments/users/${userId}`, {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function getMomentProfileSummary(userId: number) {
  const response = await http.get<ApiResponse<MomentProfileSummary>>(`/moments/users/${userId}/summary`)
  return response.data.data
}

export async function listCollectedMoments(page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<Moment>>>('/moments/collections', {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function getMoment(momentId: number) {
  const response = await http.get<ApiResponse<Moment>>(`/moments/${momentId}`)
  return response.data.data
}

export async function updateMoment(momentId: number, payload: {
  content?: string
  fileIds?: number[]
  location?: string
  tags?: string[]
  mood?: string
  activity?: string
  visibility?: string
  visibleUserIds?: number[]
}) {
  const response = await http.put<ApiResponse<Moment>>(`/moments/${momentId}`, payload)
  return response.data.data
}

export async function deleteMoment(momentId: number) {
  await http.delete(`/moments/${momentId}`)
}

export async function likeMoment(momentId: number) {
  const response = await http.post<ApiResponse<Moment>>(`/moments/${momentId}/likes`)
  return response.data.data
}

export async function unlikeMoment(momentId: number) {
  const response = await http.delete<ApiResponse<Moment>>(`/moments/${momentId}/likes`)
  return response.data.data
}

export async function listMomentLikes(momentId: number) {
  const response = await http.get<ApiResponse<MomentUser[]>>(`/moments/${momentId}/likes`)
  return response.data.data
}

export async function collectMoment(momentId: number) {
  const response = await http.post<ApiResponse<Moment>>(`/moments/${momentId}/collect`)
  return response.data.data
}

export async function uncollectMoment(momentId: number) {
  const response = await http.delete<ApiResponse<Moment>>(`/moments/${momentId}/collect`)
  return response.data.data
}

export async function listMomentComments(momentId: number) {
  const response = await http.get<ApiResponse<MomentComment[]>>(`/moments/${momentId}/comments`)
  return response.data.data
}

export async function addMomentComment(
  momentId: number,
  content: string,
  replyToCommentId?: number,
  mentionUserIds: number[] = [],
) {
  const response = await http.post<ApiResponse<MomentComment>>(`/moments/${momentId}/comments`, {
    content,
    replyToCommentId,
    mentionUserIds,
  })
  return response.data.data
}

export async function deleteMomentComment(momentId: number, commentId: number) {
  await http.delete(`/moments/${momentId}/comments/${commentId}`)
}

export async function reportMoment(momentId: number, reason: string) {
  await http.post(`/moments/${momentId}/report`, { reason })
}

export async function listMomentReports(status?: number, page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<MomentReport>>>('/moments/reports', {
    params: { status, page, pageSize },
  })
  return response.data.data
}

export async function reviewMomentReport(
  reportId: number,
  payload: {
    status: number
    handleNote?: string
    deleteMoment?: boolean
  },
) {
  const response = await http.put<ApiResponse<MomentReport>>(`/moments/reports/${reportId}/review`, payload)
  return response.data.data
}

export async function listMomentNotifications(page = 1, pageSize = 20) {
  const response = await http.get<ApiResponse<PageResponse<MomentNotification>>>('/moments/notifications', {
    params: { page, pageSize },
  })
  return response.data.data
}

export async function countMomentUnreadNotifications() {
  const response = await http.get<ApiResponse<number>>('/moments/notifications/unread-count')
  return response.data.data
}

export async function markMomentNotificationRead(notificationId: number) {
  await http.put(`/moments/notifications/${notificationId}/read`)
}

export async function markAllMomentNotificationsRead() {
  await http.put('/moments/notifications/read-all')
}
