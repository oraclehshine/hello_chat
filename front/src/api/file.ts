import http from './http'

export interface UploadResponse {
  fileId: number
  fileUrl: string
  fileName: string
  fileType: string
  mimeType: string
  fileSize: number
  scene: string
}

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

export async function uploadFile(file: File, scene = 'attachment') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('scene', scene)
  const response = await http.post<ApiResponse<UploadResponse>>('/files/upload', formData)
  return response.data.data
}

export async function uploadImage(file: File, scene = 'image') {
  return uploadFile(file, scene)
}
