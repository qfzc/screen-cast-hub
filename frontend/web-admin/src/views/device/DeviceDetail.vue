<template>
  <div class="device-detail">
    <a-card :loading="loading">
      <template #title>
        <div class="card-header">
          <div class="header-left">
            <a-button type="link" @click="$router.back()">
              <ArrowLeftOutlined />
              {{ t('common.back') }}
            </a-button>
            <span class="title">{{ device?.name || t('device.detail') }}</span>
          </div>
          <div class="header-right">
            <a-button type="primary" @click="goToPublish">
              {{ t('device.publishContent') }}
            </a-button>
          </div>
        </div>
      </template>

      <!-- 设备信息概览 -->
      <div class="device-overview" v-if="device">
        <!-- 左侧：状态与详情 -->
        <div class="overview-left">
          <div class="status-row">
            <span :class="['status-dot', device.status === 1 ? 'online' : 'offline']"></span>
            <a-tag :color="device.status === 1 ? 'success' : 'default'">
              {{ device.status === 1 ? t('device.online') : t('device.offline') }}
            </a-tag>
            <span class="heartbeat">
              <ClockCircleOutlined />
              {{ formatTime(device.onlineAt) }}
            </span>
          </div>
          <div class="info-row">
            <div class="info-item">
              <span class="label">{{ t('device.id') }}</span>
              <span class="value">{{ device.id }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('device.bindCode') }}</span>
              <span class="value bind-code">{{ device.bindCode }}</span>
            </div>
            <div class="info-item">
              <span class="label">{{ t('device.token') }}</span>
              <a-tooltip :title="device.deviceToken" placement="top">
                <span class="value token">{{ device.deviceToken }}</span>
              </a-tooltip>
            </div>
            <div class="info-item">
              <span class="label">{{ t('device.created') }}</span>
              <span class="value">{{ formatTime(device.createdAt) }}</span>
            </div>
          </div>
        </div>
        <!-- 右侧：快捷操作 -->
        <div class="overview-actions">
          <a-button @click="goToContent">
            <UnorderedListOutlined />
            {{ t('device.playlist') }}
          </a-button>
          <a-button type="primary" @click="goToPublish">
            <SendOutlined />
            {{ t('device.publishContent') }}
          </a-button>
          <a-button @click="goToPublishHistory">
            <HistoryOutlined />
            {{ t('device.publishHistory') }}
          </a-button>
        </div>
      </div>

      <!-- 当前播放任务 -->
      <div class="task-section">
        <div class="section-header">
          <h3>
            <PlayCircleOutlined />
            {{ t('device.currentTask') }}
          </h3>
          <a-tag v-if="deviceTasks.length > 0" color="processing">
            {{ t('device.taskCount', { count: deviceTasks.length }) }}
          </a-tag>
        </div>

        <a-spin :spinning="taskLoading">
          <!-- 有任务时显示任务卡片 -->
          <div v-if="deviceTasks.length > 0" class="task-cards">
            <div
              v-for="task in deviceTasks"
              :key="task.taskId"
              class="task-card"
            >
              <!-- 任务头部 -->
              <div class="task-header">
                <div class="task-title">
                  <span class="task-id">{{ t('device.task') }} #{{ task.taskId }}</span>
                  <a-tag :color="getPlayModeColor(task.playMode)" size="small">
                    {{ task.playMode === 'SEQUENCE' ? t('device.sequence') : t('device.random') }}
                  </a-tag>
                </div>
                <a-tag :color="getStatusColor(task.status)">
                  {{ getStatusText(task.status) }}
                </a-tag>
              </div>

              <!-- 素材预览 -->
              <div class="materials-preview">
                <div
                  v-for="item in task.items.slice(0, 6)"
                  :key="item.materialId"
                  class="material-item"
                >
                  <MaterialThumbnail
                    :material="{ type: item.materialType, thumbnailUrl: item.thumbnailUrl, materialUrl: item.materialUrl, name: item.materialName }"
                    size="medium"
                  />
                  <div class="material-name" :title="item.materialName">
                    {{ item.materialName }}
                  </div>
                </div>
                <!-- 更多素材提示 -->
                <div v-if="task.items.length > 6" class="more-materials">
                  <span>+{{ task.items.length - 6 }}</span>
                </div>
              </div>

              <!-- 任务信息 -->
              <div class="task-info">
                <div class="info-item">
                  <span class="info-label">{{ t('device.materialCount') }}</span>
                  <span class="info-value">{{ task.items.length }} {{ t('common.items') }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">{{ t('device.playInterval') }}</span>
                  <span class="info-value">{{ task.playInterval }} {{ t('common.seconds') }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">{{ t('device.createdAt') }}</span>
                  <span class="info-value">{{ formatTime(task.createdAt) }}</span>
                </div>
              </div>

              <!-- 任务操作 -->
              <div class="task-actions" v-if="task.status === 0">
                <a-popconfirm
                  :title="t('device.cancelTaskConfirm')"
                  @confirm="handleCancelTask(task.taskId)"
                >
                  <a-button type="link" danger size="small">
                    {{ t('device.cancelTask') }}
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </div>

          <!-- 无任务时显示空状态 -->
          <div v-else class="empty-tasks">
            <a-empty :description="t('device.noTasks')">
              <a-button type="primary" @click="goToPublish">
                {{ t('device.publishNow') }}
              </a-button>
            </a-empty>
          </div>
        </a-spin>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeftOutlined,
  ClockCircleOutlined,
  PlayCircleOutlined,
  UnorderedListOutlined,
  SendOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'

const { t } = useI18n()
import { useDeviceStore } from '@/store/device'
import { publishApi, type DeviceTask, type TaskItem } from '@/api/publish'
import type { Device } from '@/api/device'
import MaterialThumbnail from '@/components/MaterialThumbnail.vue'

const route = useRoute()
const router = useRouter()
const deviceStore = useDeviceStore()

const loading = ref(false)
const taskLoading = ref(false)
const device = ref<Device | null>(null)
const deviceTasks = ref<DeviceTask[]>([])

function formatTime(time?: string | null): string {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function getStatusColor(status: number): string {
  const map: Record<number, string> = {
    0: 'warning',   // PENDING
    1: 'success',   // PUBLISHED
    2: 'processing', // PLAYING
    3: 'default',   // COMPLETED
    4: 'default'    // CANCELLED
  }
  return map[status] || 'default'
}

function getStatusText(status: number): string {
  const map: Record<number, string> = {
    0: t('device.pending'),
    1: t('device.published'),
    2: t('device.playing'),
    3: t('device.completed'),
    4: t('device.cancelled')
  }
  return map[status] || t('publish.unknown')
}

function getPlayModeColor(mode: string): string {
  return mode === 'SEQUENCE' ? 'blue' : 'purple'
}

async function fetchDevice(): Promise<void> {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    device.value = await deviceStore.fetchDeviceById(id)
  } catch (error: unknown) {
    const err = error as { message?: string }
    message.error(err.message || t('device.fetchDeviceFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchDeviceTasks(): Promise<void> {
  const id = Number(route.params.id)
  if (!id) return

  taskLoading.value = true
  try {
    const res = await publishApi.getDeviceTasks(id)
    deviceTasks.value = res.data
  } catch (error: unknown) {
    console.error('获取设备任务失败:', error)
  } finally {
    taskLoading.value = false
  }
}

async function handleCancelTask(taskId: number): Promise<void> {
  try {
    await publishApi.cancel(taskId)
    message.success(t('device.taskCancelled'))
    await fetchDeviceTasks()
  } catch (error: unknown) {
    const err = error as { message?: string }
    message.error(err.message || t('device.cancelTaskFailed'))
  }
}

function goToPublish(): void {
  router.push({
    path: '/publish/create',
    query: { deviceId: route.params.id }
  })
}

function goToContent(): void {
  router.push({
    name: 'DeviceContent',
    params: { id: route.params.id }
  })
}

function goToPublishHistory(): void {
  router.push({
    path: '/publish',
    query: { deviceId: route.params.id }
  })
}

onMounted(() => {
  fetchDevice()
  fetchDeviceTasks()
})
</script>

<style scoped>
.device-detail {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-right {
  display: flex;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: bold;
}

/* 设备信息概览 */
.device-overview {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 8px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.overview-left {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.status-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.status-dot.online {
  background-color: #52c41a;
  box-shadow: 0 0 8px rgba(82, 196, 26, 0.6);
  animation: pulse 2s infinite;
}

.status-dot.offline {
  background-color: #d9d9d9;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.heartbeat {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #8c8c8c;
  font-size: 13px;
}

.info-row {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.info-row .info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-row .label {
  font-size: 11px;
  color: #8c8c8c;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-row .value {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.info-row .value.bind-code {
  font-family: monospace;
  font-size: 16px;
  color: #1890ff;
  letter-spacing: 2px;
}

.info-row .value.token {
  font-family: monospace;
  font-size: 12px;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #595959;
}

.overview-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

/* 任务区域 */
.task-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 任务卡片 */
.task-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.task-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
  transition: box-shadow 0.2s;
}

.task-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-id {
  font-weight: 600;
  color: #262626;
}

/* 素材预览 */
.materials-preview {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}

.material-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 72px;
}

.material-name {
  font-size: 11px;
  color: #595959;
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-materials {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  background: #e6f7ff;
  border-radius: 4px;
  color: #1890ff;
  font-size: 14px;
  font-weight: 500;
}

/* 任务信息 */
.task-info {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  font-size: 13px;
}

.info-item {
  display: flex;
  gap: 4px;
}

.info-label {
  color: #8c8c8c;
}

.info-value {
  color: #262626;
}

.task-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

/* 空状态 */
.empty-tasks {
  padding: 40px 0;
  text-align: center;
}

/* 响应式 */
@media (max-width: 768px) {
  .device-overview {
    flex-direction: column;
    gap: 16px;
  }

  .overview-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .info-row {
    gap: 16px;
  }

  .task-info {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
