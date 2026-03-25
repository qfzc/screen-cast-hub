<template>
  <div class="dashboard">
    <a-row :gutter="20" class="stats-row">
      <a-col :span="6">
        <a-card class="stat-card" hoverable>
          <div class="stat-content">
            <div class="stat-icon online">
              <DesktopOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.onlineDevices }}</div>
              <div class="stat-label">{{ t('dashboard.onlineDevices') }}</div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card class="stat-card" hoverable>
          <div class="stat-content">
            <div class="stat-icon offline">
              <DesktopOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.offlineDevices }}</div>
              <div class="stat-label">{{ t('dashboard.offlineDevices') }}</div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card class="stat-card" hoverable>
          <div class="stat-content">
            <div class="stat-icon material">
              <FolderOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalMaterials }}</div>
              <div class="stat-label">{{ t('dashboard.totalMaterials') }}</div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="6">
        <a-card class="stat-card" hoverable>
          <div class="stat-content">
            <div class="stat-icon task">
              <FileTextOutlined />
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.pendingTasks }}</div>
              <div class="stat-label">{{ t('dashboard.pendingTasks') }}</div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="20">
      <a-col :span="16">
        <a-card class="content-card">
          <template #title>
            <div class="card-header">
              <span>{{ t('dashboard.recentDevices') }}</span>
              <a-button type="link" @click="$router.push('/device/list')">
                {{ t('dashboard.viewAll') }}
              </a-button>
            </div>
          </template>
          <a-table
            :columns="deviceColumns"
            :data-source="recentDevices"
            :loading="deviceLoading"
            :pagination="false"
            :scroll="{ x: 'max-content', y: 250 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'default'">
                  {{ record.status === 1 ? t('device.online') : t('device.offline') }}
                </a-tag>
              </template>
              <template v-if="column.key === 'lastHeartbeat'">
                {{ formatTime(record.lastHeartbeat) }}
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" @click="$router.push(`/device/${record.id}`)">
                  {{ t('common.detail') }}
                </a-button>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card class="content-card">
          <template #title>
            <div class="card-header">
              <span>{{ t('dashboard.recentMaterials') }}</span>
              <a-button type="link" @click="$router.push('/material/list')">
                {{ t('dashboard.viewAll') }}
              </a-button>
            </div>
          </template>
          <a-spin :spinning="materialLoading">
            <div class="material-list">
            <div
              v-for="item in recentMaterials"
              :key="item.id"
              class="material-item"
            >
              <MaterialThumbnail :material="item" size="small" />
              <div class="material-info">
                <div class="material-name">{{ item.name }}</div>
                <div class="material-meta">{{ item.type }} · {{ formatSize(item.fileSize) }}</div>
              </div>
            </div>
            <a-empty v-if="recentMaterials.length === 0" :description="t('dashboard.noMaterials')" />
            </div>
          </a-spin>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { DesktopOutlined, FolderOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { useDeviceStore } from '@/store/device'
import { useMaterialStore } from '@/store/material'
import MaterialThumbnail from '@/components/MaterialThumbnail.vue'

const { t } = useI18n()
const deviceStore = useDeviceStore()
const materialStore = useMaterialStore()

const deviceLoading = ref(false)
const materialLoading = ref(false)

const stats = ref({
  onlineDevices: 0,
  offlineDevices: 0,
  totalMaterials: 0,
  pendingTasks: 0
})

const deviceColumns = computed(() => [
  { title: t('dashboard.deviceName'), dataIndex: 'name', key: 'name' },
  { title: t('common.status'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('dashboard.lastHeartbeat'), dataIndex: 'lastHeartbeat', key: 'lastHeartbeat', width: 180 },
  { title: t('common.actions'), key: 'action', width: 100 }
])

const recentDevices = ref<any[]>([])
const recentMaterials = ref<any[]>([])

function formatTime(time: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(async () => {
  deviceLoading.value = true
  materialLoading.value = true

  try {
    // 获取设备列表
    await deviceStore.fetchDevices({ page: 1, size: 5 })
    recentDevices.value = deviceStore.devices

    // 计算在线/离线设备
    const allDevices = deviceStore.devices
    stats.value.onlineDevices = allDevices.filter((d: any) => d.status === 1).length
    stats.value.offlineDevices = allDevices.filter((d: any) => d.status === 0).length
  } finally {
    deviceLoading.value = false
  }

  try {
    // 获取素材列表
    await materialStore.fetchMaterials({ page: 1, size: 5 })
    recentMaterials.value = materialStore.materials
    stats.value.totalMaterials = materialStore.total
  } finally {
    materialLoading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: #fff;
}

.stat-icon.online {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon.offline {
  background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
}

.stat-icon.material {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
}

.stat-icon.task {
  background: linear-gradient(135deg, #e6a23c 0%, #ebb563 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.content-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.material-list {
  max-height: 300px;
  overflow-y: auto;
}

.material-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.material-item:last-child {
  border-bottom: none;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
