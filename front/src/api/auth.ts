import http from './http'

export interface CaptchaResponse {
  code: number
  message: string
  data: string
  timestamp: number
}

export interface AuthResponse {
  code: number
  message: string
  data: {
    accessToken: string
    refreshToken: string
    userId: number
    email: string
    nickname: string
    avatarUrl: string | null
  }
  timestamp: number
}

export interface UserProfile {
  nickname: string
  avatarUrl: string | null
  signature: string
  phone: string
  gender?: number
  age?: number
}

export interface UserProfileResponse {
  code: number
  message: string
  data: {
    userId: number
    email: string
    nickname: string
    avatarUrl: string | null
    signature: string | null
    gender: number | null
    age: number | null
    phone: string | null
    status: string
  }
  timestamp: number
}

export async function sendCaptcha(email: string, scene: string) {
  const response = await http.post<CaptchaResponse>('/auth/email-captcha', { email, scene })
  return response.data.data
}

export async function register(email: string, password: string, captcha: string) {
  const response = await http.post<AuthResponse>('/auth/register', { email, password, captcha })
  return response.data.data
}

export async function login(email: string, password: string) {
  const response = await http.post<AuthResponse>('/auth/login', { email, password })
  return response.data.data
}

export async function resetPassword(email: string, captcha: string, newPassword: string) {
  await http.post('/auth/password-reset', { email, captcha, newPassword })
}

export async function refresh(refreshToken: string) {
  const response = await http.post<AuthResponse>('/auth/refresh', { refreshToken })
  return response.data.data
}

export async function logout(refreshToken: string) {
  await http.post('/auth/logout', { refreshToken })
}

export async function changePassword(oldPassword: string, newPassword: string) {
  await http.put('/auth/password', { oldPassword, newPassword })
}

export async function getMe() {
  const response = await http.get<UserProfileResponse>('/users/me')
  return response.data.data
}

export async function updateProfile(profile: Partial<UserProfile>) {
  const response = await http.put<UserProfileResponse>('/users/me', profile)
  return response.data.data
}

export async function updateEmail(email: string, captcha: string) {
  const response = await http.put<UserProfileResponse>('/users/me/email', { email, captcha })
  return response.data.data
}
