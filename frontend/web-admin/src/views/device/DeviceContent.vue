<template>
  <div class="device-content">
    <a-card :loading="loading">
      <template #title>
        <div class="card-header">
          <div class="header-left">
            <a-button type="link" @click="$router.back()">
              <ArrowLeftOutlined />
              {{ t('common.back') }}
            </a-button>
            <span class="title">{{ device?.name || t('device.contentManage') }}</span>
          </div>
        </div>
      </template>

      <!-- 全局设置 -->
      <div class="settings-section">
        <h3>{{ t('content.playSettings') }}</h3>
        <a-form layout="inline">
          <a-form-item :label="t('content.playMode')">
            <a-radio-group v-model:value="playlist.playMode">
              <a-radio value="SEQUENCE">{{ t('content.sequence') }}</a-radio>
              <a-radio value="RANDOM">{{ t('content.random') }}</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item :label="t('content.playInterval')">
            <a-input-number
              v-model:value="playlist.playInterval"
              :min="1"
              :max="3600"
            />
            <span style="margin-left: 8px; color: #909399;">{{ t('common.seconds') }}</span>
          </a-form-item>
          <a-form-item :label="t('content.loopPlay')">
            <a-switch v-model:checked="playlist.loopPlay" />
          </a-form-item>
        </a-form>
      </div>

      <!-- 播放列表 -->
      <div class="playlist-section">
        <div class="section-header">
          <h3>{{ t('content.playlist') }}</h3>
          <a-button type="primary" @click="showMaterialDialog = true">
            <PlusOutlined />
            {{ t('content.addMaterial') }}
          </a-button>
        </div>

        <div class="playlist-empty" v-if="playlist.items.length === 0">
          <a-empty :description="t('content.noContent')" />
        </div>

        <draggable
          v-else
          v-model="playlist.items"
          item-key="id"
          handle=".drag-handle"
          animation="200"
          class="playlist-items"
          @end="updateSortOrder"
        >
          <template #item="{ element, index }">
            <div class="playlist-item">
              <HolderOutlined class="drag-handle" />
              <MaterialThumbnail
                :material="{ type: element.materialType, thumbnailUrl: element.thumbnailUrl, materialUrl: element.materialUrl, name: element.materialName }"
                :width="80"
                :height="60"
              />
              <div class="item-info">
                <div class="item-name">{{ element.materialName }}</div>
                <div class="item-meta">
                  <span>{{ t('content.duration') }}: {{ element.duration }}{{ t('common.seconds') }}</span>
                  <a-divider type="vertical" />
                  <span>{{ t('content.fitMode') }}: {{ getFitModeText(element.fitMode) }}</span>
                  <a-divider type="vertical" />
                  <span>{{ t('content.transition') }}: {{ getTransitionText(element.transition) }}</span>
                </div>
              </div>
              <div class="item-actions">
                <a-button type="link" @click="openItemSettings(index)">
                  <SettingOutlined />
                  {{ t('common.settings') }}
                </a-button>
                <a-popconfirm
                  :title="t('content.removeConfirm')"
                  @confirm="removeItem(index)"
                >
                  <a-button type="link" danger>
                    <DeleteOutlined />
                    {{ t('common.remove') }}
                  </a-button>
                </a-popconfirm>
              </div>
            </div>
          </template>
        </draggable>
      </div>

      <!-- 底部操作 -->
      <div class="footer-actions">
        <a-space>
          <a-button @click="$router.back()">{{ t('common.cancel') }}</a-button>
          <a-button type="primary" :loading="saving" @click="handleSave">
            {{ t('content.saveAndPublish') }}
          </a-button>
        </a-space>
      </div>
    </a-card>

    <!-- 素材选择对话框 -->
    <a-modal
      v-model:open="showMaterialDialog"
      :title="t('content.selectMaterial')"
      width="70%"
      @ok="confirmMaterialSelect"
    >
      <div class="material-dialog-content">
        <div class="filter-bar">
          <a-select
            v-model:value="dialogFilterType"
            :placeholder="t('content.materialType')"
            allowClear
            style="width: 120px"
            @change="fetchMaterials"
          >
            <a-select-option value="">{{ t('common.all') }}</a-select-option>
            <a-select-option value="IMAGE">{{ t('content.image') }}</a-select-option>
            <a-select-option value="VIDEO">{{ t('content.video') }}</a-select-option>
            <a-select-option value="PDF">{{ t('content.pdf') }}</a-select-option>
          </a-select>
        </div>

        <a-spin :spinning="materialStore.loading">
          <div class="material-grid">
            <div
              v-for="item in materialStore.materials"
              :key="item.id"
              class="material-card"
              :class="{ selected: tempSelectedIds.includes(item.id) || isMaterialInPlaylist(item.id) }"
              @click="toggleMaterialSelect(item)"
            >
              <MaterialThumbnail
                :material="item"
                :width="'100%'"
                :height="100"
                :selected="tempSelectedIds.includes(item.id) || isMaterialInPlaylist(item.id)"
              />
              <div class="material-info">
                <div class="material-name">{{ item.name }}</div>
                <div class="material-meta">{{ formatSize(item.fileSize) }}</div>
              </div>
            </div>
          </div>
        </a-spin>

        <div class="pagination-wrapper">
          <a-pagination
            v-model:current="dialogPage"
            v-model:pageSize="dialogPageSize"
            :total="materialStore.total"
            simple
            @change="fetchMaterials"
          />
        </div>
      </div>
    </a-modal>

    <!-- 素材设置弹窗 -->
    <a-modal
      v-model:open="showItemSettings"
      :title="t('content.materialSettings')"
      @ok="saveItemSettings"
    >
      <a-form :label-col="{ span: 6 }" v-if="currentEditItem">
        <a-form-item :label="t('content.fitModeLabel')">
          <a-radio-group v-model:value="currentEditItem.fitMode">
            <a-radio value="FILL">{{ t('content.fitFill') }}</a-radio>
            <a-radio value="FIT">{{ t('content.fitFit') }}</a-radio>
            <a-radio value="ORIGINAL">{{ t('content.fitOriginal') }}</a-radio>
            <a-radio value="STRETCH">{{ t('content.fitStretch') }}</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item :label="t('content.durationLabel')">
          <a-input-number
            v-model:value="currentEditItem.duration"
            :min="1"
            :max="3600"
          />
          <span style="margin-left: 8px; color: #909399;">{{ t('common.seconds') }}</span>
        </a-form-item>
        <a-form-item :label="t('content.transitionLabel')">
          <a-radio-group v-model:value="currentEditItem.transition">
            <a-radio value="NONE">{{ t('content.transitionNone') }}</a-radio>
            <a-radio value="FADE">{{ t('content.transitionFade') }}</a-radio>
            <a-radio value="SLIDE">{{ t('content.transitionSlide') }}</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  ArrowLeftOutlined,
  PlusOutlined,
  HolderOutlined,
  SettingOutlined,
  DeleteOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'

const { t } = useI18n()
import draggable from 'vuedraggable'
import { useDeviceStore } from '@/store/device'
import { useMaterialStore } from '@/store/material'
import { playlistApi, type DevicePlaylist, type PlaylistItem } from '@/api/playlist'
import { publishApi } from '@/api/publish'
import type { Material } from '@/api/material'
import type { Device } from '@/api/device'
import MaterialThumbnail from '@/components/MaterialThumbnail.vue'

const route = useRoute()
const router = useRouter()
const deviceStore = useDeviceStore()
const materialStore = useMaterialStore()

const loading = ref(false)
const saving = ref(false)
const device = ref<Device | null>(null)

const playlist = reactive<DevicePlaylist>({
  deviceId: 0,
  playMode: 'SEQUENCE',
  playInterval: 5,
  loopPlay: true,
  items: []
})

// 素材选择对话框
const showMaterialDialog = ref(false)
const dialogFilterType = ref('')
const dialogPage = ref(1)
const dialogPageSize = ref(12)
const tempSelectedIds = ref<number[]>([])

// 素材设置弹窗
const showItemSettings = ref(false)
const currentEditIndex = ref(-1)
const currentEditItem = ref<PlaylistItem | null>(null)

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function getFitModeText(mode: string) {
  const map: Record<string, string> = {
    FILL: t('content.fitFill'),
    FIT: t('content.fitFit'),
    ORIGINAL: t('content.fitOriginal'),
    STRETCH: t('content.fitStretch')
  }
  return map[mode] || mode
}

function getTransitionText(transition: string) {
  const map: Record<string, string> = {
    NONE: t('content.transitionNone'),
    FADE: t('content.transitionFade'),
    SLIDE: t('content.transitionSlide')
  }
  return map[transition] || transition
}

function isMaterialInPlaylist(materialId: number) {
  return playlist.items.some(item => item.materialId === materialId)
}

async function fetchDevice() {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    device.value = await deviceStore.fetchDeviceById(id)
  } catch (error: any) {
    message.error(error.message || t('device.fetchDeviceFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchPlaylist() {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await playlistApi.getPlaylist(id)
    Object.assign(playlist, res.data)
  } catch (error: any) {
    console.error(t('content.fetchPlaylistFailed'), error)
  } finally {
    loading.value = false
  }
}

async function fetchMaterials() {
  await materialStore.fetchMaterials({
    page: dialogPage.value,
    size: dialogPageSize.value,
    type: dialogFilterType.value || undefined
  })
}

function toggleMaterialSelect(material: Material) {
  // 如果已经在播放列表中，不允许在对话框中取消
  if (isMaterialInPlaylist(material.id)) {
    return
  }

  const index = tempSelectedIds.value.indexOf(material.id)
  if (index > -1) {
    tempSelectedIds.value.splice(index, 1)
  } else {
    tempSelectedIds.value.push(material.id)
  }
}

function confirmMaterialSelect() {
  // 将临时选择的素材添加到播放列表
  for (const id of tempSelectedIds.value) {
    if (!isMaterialInPlaylist(id)) {
      const material = materialStore.materials.find(m => m.id === id)
      if (material) {
        const newItem: PlaylistItem = {
          id: Date.now() + id, // 临时ID
          materialId: material.id,
          materialName: material.name,
          materialType: material.type,
          materialUrl: material.url,
          thumbnailUrl: material.thumbnailUrl,
          sortOrder: playlist.items.length,
          fitMode: 'FILL',
          duration: material.duration || 10,
          transition: 'NONE'
        }
        playlist.items.push(newItem)
      }
    }
  }
  tempSelectedIds.value = []
  showMaterialDialog.value = false
}

function removeItem(index: number) {
  playlist.items.splice(index, 1)
  updateSortOrder()
}

function updateSortOrder() {
  playlist.items.forEach((item, index) => {
    item.sortOrder = index
  })
}

function openItemSettings(index: number) {
  currentEditIndex.value = index
  currentEditItem.value = { ...playlist.items[index] }
  showItemSettings.value = true
}

function saveItemSettings() {
  if (currentEditIndex.value >= 0 && currentEditItem.value) {
    playlist.items[currentEditIndex.value] = { ...currentEditItem.value }
  }
  showItemSettings.value = false
  currentEditItem.value = null
  currentEditIndex.value = -1
}

async function handleSave() {
  const id = Number(route.params.id)
  if (!id) return

  saving.value = true
  try {
    // 1. 保存播放列表配置（用于下次编辑）
    await playlistApi.updatePlaylist(id, {
      playMode: playlist.playMode,
      playInterval: playlist.playInterval,
      loopPlay: playlist.loopPlay,
      items: playlist.items.map(item => ({
        materialId: item.materialId,
        sortOrder: item.sortOrder,
        fitMode: item.fitMode,
        duration: item.duration,
        transition: item.transition
      }))
    })

    // 2. 发布到设备（通过 /publish/task API）
    await publishApi.create({
      deviceIds: [id],
      materials: playlist.items.map(item => ({
        materialId: item.materialId,
        sortOrder: item.sortOrder,
        duration: item.duration,
        fitMode: item.fitMode,
        transition: item.transition
      })),
      playMode: playlist.playMode,
      playInterval: playlist.playInterval,
      loopPlay: playlist.loopPlay,
      autoPlay: true
    })

    message.success(t('content.saveSuccess'))
    router.push({ name: 'DeviceDetail', params: { id } })
  } catch (error: any) {
    message.error(error.message || t('content.saveFailed'))
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  await fetchDevice()
  await fetchPlaylist()
  await fetchMaterials()
})
</script>

<style scoped>
.device-content {
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

.title {
  font-size: 18px;
  font-weight: bold;
}

.settings-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}

.settings-section h3 {
  margin-bottom: 16px;
  font-size: 16px;
  color: #303133;
}

.playlist-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.playlist-empty {
  padding: 40px;
  background: #fafafa;
  border-radius: 8px;
  text-align: center;
}

.playlist-items {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.playlist-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  transition: background 0.2s;
}

.playlist-item:last-child {
  border-bottom: none;
}

.playlist-item:hover {
  background: #fafafa;
}

.drag-handle {
  cursor: move;
  color: #909399;
  font-size: 16px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-meta {
  font-size: 12px;
  color: #909399;
}

.item-actions {
  flex-shrink: 0;
}

.footer-actions {
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
}

/* 素材选择对话框 */
.material-dialog-content {
  min-height: 400px;
}

.filter-bar {
  margin-bottom: 16px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
  min-height: 300px;
}

.material-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.material-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.material-card.selected {
  border-color: #1890ff;
}

.material-info {
  padding: 8px;
}

.material-info .material-name {
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-info .material-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
