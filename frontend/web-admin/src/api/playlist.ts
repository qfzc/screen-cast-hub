import { get, put } from './request'
import type { ApiResponse } from './request'

// 播放列表项 - 与后端 PlaylistItem 对应
export interface PlaylistItem {
  id: number
  materialId: number
  materialName: string
  materialType: 'IMAGE' | 'VIDEO' | 'PDF'
  materialUrl: string
  thumbnailUrl?: string
  sortOrder: number
  fitMode: 'FILL' | 'FIT' | 'ORIGINAL' | 'STRETCH'
  duration: number // 播放时长(秒)
  transition: 'NONE' | 'FADE' | 'SLIDE'
}

// 设备播放列表 - 与后端 DevicePlaylist 对应
export interface DevicePlaylist {
  deviceId: number
  playMode: 'SEQUENCE' | 'RANDOM'
  playInterval: number
  loopPlay: boolean
  isActive?: boolean
  lastPublishedAt?: string
  lastBatchId?: string
  items: PlaylistItem[]
}

// 更新播放列表请求参数
export interface DevicePlaylistRequest {
  playMode: 'SEQUENCE' | 'RANDOM'
  playInterval: number
  loopPlay: boolean
  items: {
    materialId: number
    sortOrder: number
    fitMode: 'FILL' | 'FIT' | 'ORIGINAL' | 'STRETCH'
    duration: number
    transition: 'NONE' | 'FADE' | 'SLIDE'
  }[]
}

export const playlistApi = {
  // 获取设备播放列表
  getPlaylist(deviceId: number): Promise<ApiResponse<DevicePlaylist>> {
    return get(`/device/${deviceId}/playlist`)
  },

  // 更新设备播放列表
  updatePlaylist(deviceId: number, data: DevicePlaylistRequest): Promise<ApiResponse<DevicePlaylist>> {
    return put(`/device/${deviceId}/playlist`, data)
  }
}
