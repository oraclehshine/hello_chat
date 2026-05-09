import http from './http'
import type { ApiResponse, UserSearchItem } from './chat'
import type { GroupSummary } from './group'

export interface Presence {
  userId: number
  status: 'online' | 'offline' | 'busy' | 'invisible'
  lastActiveAt: string
  updatedAt: string
}

export interface SearchHistory {
  historyId: number
  keyword: string
  searchType: string
  createdAt: string
}

export interface TopicRecommendation {
  tag: string
  momentCount: number
}

export interface SocialSearchResult {
  users: UserSearchItem[]
  groups: GroupSummary[]
}

export interface RecommendationResult {
  friends: UserSearchItem[]
  groups: GroupSummary[]
  topics: TopicRecommendation[]
}

export async function updatePresence(status: Presence['status']) {
  const response = await http.put<ApiResponse<Presence>>('/social/presence', { status })
  return response.data.data
}

export async function getPresence(userId: number) {
  const response = await http.get<ApiResponse<Presence>>(`/social/presence/${userId}`)
  return response.data.data
}

export async function socialSearch(keyword: string) {
  const response = await http.get<ApiResponse<SocialSearchResult>>('/social/search', {
    params: { keyword },
  })
  return response.data.data
}

export async function listSearchHistory() {
  const response = await http.get<ApiResponse<SearchHistory[]>>('/social/search/history')
  return response.data.data
}

export async function listRecommendations() {
  const response = await http.get<ApiResponse<RecommendationResult>>('/social/recommendations')
  return response.data.data
}
