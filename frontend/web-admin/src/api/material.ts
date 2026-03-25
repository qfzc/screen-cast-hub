import { get, post, del } from './request'
import type { ApiResponse } from './request'

export interface Material {
  id: number
  name: string
  originalName?: string
  type: 'IMAGE' | 'VIDEO' | 'PDF' | 'PPT'
  originalUrl: string
  convertedUrl?: string
  thumbnailUrl?: string
  fileSize: number
  duration?: number
  pageCount?: number
  convertStatus: number
  convertStatusDesc?: string
  createdAt: string
}

export interface MaterialListResult {
  list: Material[]
  total: number
}

export interface UploadResult {
  id: number
  name: string
  url: string
}

export const materialApi = {
  list(params?: { page?: number; size?: number; type?: string }): Promise<ApiResponse<MaterialListResult>> {
    return get('/material/list', { params })
  },

  getById(id: number): Promise<ApiResponse<Material>> {
    return get(`/material/${id}`)
  },

  upload(file: File, onProgress?: (percent: number) => void): Promise<ApiResponse<UploadResult>> {
    const formData = new FormData()
    formData.append('file', file)

    return post('/material/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (e.total && onProgress) {
          onProgress(Math.round((e.loaded * 100) / e.total))
        }
      }
    })
  },

  delete(id: number): Promise<ApiResponse<void>> {
    return del(`/material/${id}`)
  },

  getConversionStatus(id: number): Promise<ApiResponse<{ status: string }>> {
    return get(`/material/${id}/conversion-status`)
  }
}
