import { get, post } from './request'
import type { ApiResponse } from './request'

export interface Device {
  id: number
  deviceToken: string
  name: string
  bindCode: string
  status: number // 0: offline, 1: online
  onlineAt: string
  createdAt: string
  groupId?: number
  model?: string
  statusDesc?: string
  storageUsed?: number
  storageTotal?: number
  playingContent?: string
  groupName?: string
  bindAt?: string
  appVersion?: string
}

export interface DeviceListResult {
  list: Device[]
  total: number
}

export interface BindDeviceParams {
  bindCode: string
  name: string
  groupId?: number
}

export interface HeartbeatParams {
  deviceToken: string
}

export const deviceApi = {
  list(params?: { page?: number; size?: number; status?: number }): Promise<ApiResponse<DeviceListResult>> {
    return get('/device/list', { params })
  },

  getById(id: number): Promise<ApiResponse<Device>> {
    return get(`/device/${id}`)
  },

  bind(params: BindDeviceParams): Promise<ApiResponse<Device>> {
    return post('/device/bind', params)
  },

  unbind(id: number): Promise<ApiResponse<void>> {
    return post(`/device/${id}/unbind`)
  },

  heartbeat(params: HeartbeatParams): Promise<ApiResponse<void>> {
    return post('/device/heartbeat', params)
  },

  rename(id: number, name: string): Promise<ApiResponse<Device>> {
    return post(`/device/${id}/rename`, { name })
  },

  setGroup(id: number, groupId: number | null): Promise<ApiResponse<Device>> {
    return post(`/device/${id}/group`, { groupId })
  }
}
