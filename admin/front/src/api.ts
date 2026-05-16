import axios from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface MetricCard {
  key: string
  label: string
  value: number
  delta: number
  unit: string
}

export interface DashboardOverview {
  cards: MetricCard[]
  realtimeEvents: RealtimeEvent[]
  todos: Array<{ label: string; value: number; level: string }>
}

export interface TrendPoint {
  label: string
  onlineUsers: number
  messages: number
  sensitiveHits: number
}

export interface RealtimeEvent {
  type: string
  title: string
  content: string
  time: string
}

export interface UserRow {
  userId: number
  email: string
  nickname: string
  status: string
  online: boolean
  messageCount: number
  groupCount: number
  riskLevel: string
  lastActiveAt: string
}

export interface GroupRow {
  groupId: number
  groupName: string
  ownerName: string
  status: string
  memberCount: number
  todayMessages: number
  sensitiveHits: number
  reportCount: number
}

export interface SensitiveWordRow {
  wordId: number
  word: string
  scene: string
  level: string
  action: string
  enabled: boolean
  hitCount: number
}

export interface SensitiveWordPayload {
  word: string
  scene: string
  level: string
  action: string
  enabled: boolean
}

export interface AnnouncementRow {
  announcementId: number
  title: string
  scope: string
  status: string
  readCount: number
  createdAt: string
}

export interface AnnouncementPayload {
  title: string
  content: string
  scope: string
  status: string
}

export interface AuditLogRow {
  logId: number
  adminName: string
  module: string
  action: string
  target: string
  ip: string
  createdAt: string
}

const http = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_BASE_URL || '/admin/api/v1',
  timeout: 10000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>) {
  try {
    const response = await promise
    if (response.data.code !== 0) {
      throw new Error(response.data.message)
    }
    return response.data.data
  } catch (err) {
    if (axios.isAxiosError(err)) {
      const detail = err.response?.data?.detail
      throw new Error(typeof detail === 'string' ? detail : err.message)
    }
    throw err
  }
}

export function login(username: string, password: string) {
  return unwrap<{ accessToken: string; admin: { displayName: string; role: string } }>(
    http.post('/auth/login', { username, password }),
  )
}

export function getDashboardOverview() {
  return unwrap<DashboardOverview>(http.get('/dashboard/overview'))
}

export function getDashboardTrends() {
  return unwrap<TrendPoint[]>(http.get('/dashboard/trends'))
}

export function listUsers(keyword = '') {
  return unwrap<{ list: UserRow[]; total: number }>(http.get('/users', { params: { keyword } }))
}

export function freezeUser(userId: number) {
  return unwrap<{ userId: number; status: string }>(http.post(`/users/${userId}/freeze`))
}

export function unfreezeUser(userId: number) {
  return unwrap<{ userId: number; status: string }>(http.post(`/users/${userId}/unfreeze`))
}

export function listGroups(keyword = '') {
  return unwrap<{ list: GroupRow[]; total: number }>(http.get('/groups', { params: { keyword } }))
}

export function disableGroup(groupId: number) {
  return unwrap<{ groupId: number; status: string }>(http.post(`/groups/${groupId}/disable`))
}

export function enableGroup(groupId: number) {
  return unwrap<{ groupId: number; status: string }>(http.post(`/groups/${groupId}/enable`))
}

export function listSensitiveWords() {
  return unwrap<{ list: SensitiveWordRow[]; total: number }>(http.get('/sensitive-words'))
}

export function createSensitiveWord(payload: SensitiveWordPayload) {
  return unwrap<SensitiveWordRow>(http.post('/sensitive-words', payload))
}

export function enableSensitiveWord(wordId: number) {
  return unwrap<SensitiveWordRow>(http.post(`/sensitive-words/${wordId}/enable`))
}

export function disableSensitiveWord(wordId: number) {
  return unwrap<SensitiveWordRow>(http.post(`/sensitive-words/${wordId}/disable`))
}

export function deleteSensitiveWord(wordId: number) {
  return unwrap<boolean>(http.delete(`/sensitive-words/${wordId}`))
}

export function listAnnouncements() {
  return unwrap<{ list: AnnouncementRow[]; total: number }>(http.get('/announcements'))
}

export function createAnnouncement(payload: AnnouncementPayload) {
  return unwrap<AnnouncementRow>(http.post('/announcements', payload))
}

export function publishAnnouncement(announcementId: number) {
  return unwrap<AnnouncementRow>(http.post(`/announcements/${announcementId}/publish`))
}

export function withdrawAnnouncement(announcementId: number) {
  return unwrap<AnnouncementRow>(http.post(`/announcements/${announcementId}/withdraw`))
}

export function deleteAnnouncement(announcementId: number) {
  return unwrap<boolean>(http.delete(`/announcements/${announcementId}`))
}

export function listAuditLogs() {
  return unwrap<{ list: AuditLogRow[]; total: number }>(http.get('/audit-logs'))
}

export function dashboardWsUrl() {
  const configured = import.meta.env.VITE_ADMIN_API_BASE_URL
  if (configured) {
    return configured
      .replace(/^http:\/\//, 'ws://')
      .replace(/^https:\/\//, 'wss://')
      .replace(/\/admin\/api\/v1\/?$/, '/admin/api/v1/dashboard/ws')
  }
  return `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/admin/api/v1/dashboard/ws`
}
