import { get, post } from './request'
import type { ApiResponse } from './request'

// 发布记录 - 与后端 PublishRecordVO 对应
export interface PublishRecord {
  id: number
  batchId: string
  deviceId: number
  deviceName: string | null
  name: string | null
  status: number
  statusDesc: string
  itemCount: number
  publishedAt: string | null
  completedAt: string | null
  createdAt: string
}

export interface PublishRecordListResult {
  list: PublishRecord[]
  total: number
  page: number
  size: number
  pages: number
}

// 发布任务明细 - 与后端 PublishTaskItemVO / DeviceTaskVO.TaskItemVO 对应
export interface TaskItem {
  materialId: number
  materialName: string
  materialType: string
  materialUrl: string
  thumbnailUrl?: string
  sortOrder: number
  duration?: number
  fitMode: string
  transition: string
}

// 发布任务 - 与后端 PublishTaskVO 对应
export interface PublishTask {
  id: number
  batchId: string
  deviceId: number
  userId: number
  name: string
  playMode: 'SEQUENCE' | 'RANDOM'
  playInterval: number
  loopPlay: boolean
  autoPlay: boolean
  scheduledAt?: string
  publishedAt?: string
  startedAt?: string
  completedAt?: string
  status: number
  statusDesc: string
  items: TaskItem[]
  createdAt: string
}

export interface PublishTaskListResult {
  list: PublishTask[]
  total: number
  page: number
  size: number
  pages: number
}

// 设备任务 - 与后端 DeviceTaskVO 对应（TV端使用）
// 注意：DeviceTaskVO 返回的是任务对象，包含 items 列表
export interface DeviceTask {
  taskId: number
  batchId: string
  playMode: 'SEQUENCE' | 'RANDOM'
  playInterval: number
  autoPlay: boolean
  loopPlay: boolean
  status: number
  statusDesc: string
  items: TaskItem[]
  createdAt: string
}

// 创建发布任务 - 素材项
export interface CreatePublishMaterial {
  materialId: number
  sortOrder?: number
  duration?: number
  fitMode?: string
  transition?: string
}

// 创建发布任务请求参数
export interface CreatePublishParams {
  deviceIds: number[]
  materials?: CreatePublishMaterial[]
  name?: string
  playMode: 'SEQUENCE' | 'RANDOM'
  playInterval: number
  loopPlay?: boolean
  autoPlay?: boolean
  scheduledAt?: string
}

export const publishApi = {
  // 发布记录列表
  list(params?: { page?: number; size?: number; status?: string; deviceId?: number }): Promise<ApiResponse<PublishRecordListResult>> {
    return get('/publish/records', { params })
  },

  // 发布任务列表
  listTasks(params?: { page?: number; size?: number; status?: string; deviceId?: number }): Promise<ApiResponse<PublishTaskListResult>> {
    return get('/publish/tasks', { params })
  },

  // 任务详情
  getDetail(id: number): Promise<ApiResponse<PublishTask>> {
    return get(`/publish/task/${id}`)
  },

  // 创建发布任务
  create(params: CreatePublishParams): Promise<ApiResponse<PublishTask[]>> {
    return post('/publish/task', params)
  },

  // 取消任务
  cancel(id: number): Promise<ApiResponse<void>> {
    return post(`/publish/${id}/cancel`)
  },

  // 获取设备当前任务（管理端调用，通过设备ID）
  getDeviceTasks(deviceId: number): Promise<ApiResponse<DeviceTask[]>> {
    return get(`/publish/device/${deviceId}`)
  }
}
