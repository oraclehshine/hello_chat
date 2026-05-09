import http from './http'
import type { ApiResponse } from './chat'

export interface Friend {
  friendshipId: number
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  signature: string | null
  remarkName: string | null
  friendGroup: string | null
  star: number
  createdAt: string
}

export interface FriendRequest {
  requestId: number
  requesterId: number
  requesterEmail: string
  requesterNickname: string
  requesterAvatarUrl: string | null
  receiverId: number
  receiverEmail: string
  receiverNickname: string
  receiverAvatarUrl: string | null
  remark: string | null
  status: number
  handledAt: string | null
  createdAt: string
}

export interface BlockedUser {
  userId: number
  email: string
  nickname: string
  avatarUrl: string | null
  signature: string | null
}

export async function listFriends() {
  const response = await http.get<ApiResponse<Friend[]>>('/users/friends')
  return response.data.data
}

export async function updateFriend(
  friendUserId: number,
  payload: {
    remarkName?: string
    friendGroup?: string
    star?: boolean
  },
) {
  const response = await http.put<ApiResponse<Friend>>(`/users/friends/${friendUserId}`, payload)
  return response.data.data
}

export async function deleteFriend(friendUserId: number) {
  await http.delete(`/users/friends/${friendUserId}`)
}

export async function sendFriendRequest(receiverId: number, remark = '') {
  const response = await http.post<ApiResponse<FriendRequest>>('/users/friend-requests', { receiverId, remark })
  return response.data.data
}

export async function listReceivedFriendRequests() {
  const response = await http.get<ApiResponse<FriendRequest[]>>('/users/friend-requests/received')
  return response.data.data
}

export async function listSentFriendRequests() {
  const response = await http.get<ApiResponse<FriendRequest[]>>('/users/friend-requests/sent')
  return response.data.data
}

export async function approveFriendRequest(requestId: number) {
  const response = await http.post<ApiResponse<Friend>>(`/users/friend-requests/${requestId}/approve`)
  return response.data.data
}

export async function rejectFriendRequest(requestId: number) {
  await http.post(`/users/friend-requests/${requestId}/reject`)
}

export async function blockUser(blockedUserId: number) {
  await http.post(`/users/blocks/${blockedUserId}`)
}

export async function unblockUser(blockedUserId: number) {
  await http.delete(`/users/blocks/${blockedUserId}`)
}

export async function listBlockedUsers() {
  const response = await http.get<ApiResponse<BlockedUser[]>>('/users/blocks')
  return response.data.data
}
