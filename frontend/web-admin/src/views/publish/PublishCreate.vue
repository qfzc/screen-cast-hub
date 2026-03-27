<template>
  <div class="publish-create">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>{{ t('publish.create') }}</span>
        </div>
      </template>

      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        :label-col="{ span: 4 }"
        style="max-width: 800px;"
      >
        <!-- 设备选择 -->
        <a-form-item :label="t('publish.targetDevice')" name="deviceIds">
          <a-select
            v-model:value="form.deviceIds"
            mode="multiple"
            :placeholder="t('publish.selectDevice')"
            style="width: 100%;"
          >
            <a-select-option
              v-for="device in deviceStore.devices"
              :key="device.id"
              :value="device.id"
            >
              <span>{{ device.name }}</span>
              <a-tag
                :color="device.status === 1 ? 'green' : 'default'"
                style="margin-left: 8px;"
              >
                {{ device.status === 1 ? t('device.online') : t('device.offline') }}
              </a-tag>
            </a-select-option>
          </a-select>
        </a-form-item>

        <!-- 素材选择 -->
        <a-form-item :label="t('publish.selectMaterial')" name="materialIds">
          <div class="material-selector">
            <div class="selected-materials" v-if="form.materialIds.length > 0">
              <div class="material-list">
                <div v-for="id in form.materialIds" :key="id" class="material-item">
                  <HolderOutlined class="drag-handle" />
                  <span class="material-name">{{ getMaterialName(id) }}</span>
                  <a-button type="link" danger size="small" @click="removeMaterial(id)">
                    {{ t('common.remove') }}
                  </a-button>
                </div>
              </div>
            </div>

            <a-button @click="showMaterialDialog = true">
              <PlusOutlined />
              {{ t('publish.addMaterial') }}
            </a-button>
          </div>
        </a-form-item>

        <!-- 播放设置 -->
        <a-form-item :label="t('publish.playMode')">
          <a-radio-group v-model:value="form.playMode">
            <a-radio value="SEQUENCE">{{ t('publish.sequence') }}</a-radio>
            <a-radio value="RANDOM">{{ t('publish.random') }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="t('publish.playInterval')">
          <a-input-number
            v-model:value="form.playInterval"
            :min="1"
            :max="3600"
          />
          <span style="margin-left: 8px; color: #909399;">{{ t('common.seconds') }}</span>
        </a-form-item>

        <!-- 定时发布 -->
        <a-form-item :label="t('publish.schedule')">
          <a-switch v-model:checked="enableSchedule" />
        </a-form-item>

        <a-form-item :label="t('publish.publishTime')" v-if="enableSchedule" name="scheduledAt">
          <a-date-picker
            v-model:value="form.scheduledAt"
            show-time
            :placeholder="t('publish.selectPublishTime')"
            :disabled-date="disabledDate"
          />
        </a-form-item>

        <!-- 提交按钮 -->
        <a-form-item :wrapper-col="{ offset: 4 }">
          <a-space>
            <a-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ t('publish.publishNow') }}
            </a-button>
            <a-button @click="resetForm">{{ t('common.reset') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 素材选择对话框 -->
    <a-modal v-model:open="showMaterialDialog" :title="t('publish.selectMaterial')" width="70%" @ok="confirmMaterialSelect">
      <div class="material-dialog-content">
        <div class="filter-bar">
          <a-select v-model:value="dialogFilterType" :placeholder="t('content.materialType')" allowClear style="width: 120px" @change="fetchMaterials">
            <a-select-option value="">{{ t('common.all') }}</a-select-option>
            <a-select-option value="IMAGE">{{ t('content.image') }}</a-select-option>
            <a-select-option value="VIDEO">{{ t('content.video') }}</a-select-option>
            <a-select-option value="PDF">{{ t('content.pdf') }}</a-select-option>
          </a-select>
          <a-button type="link" @click="goToMaterialUpload">
            <UploadOutlined />
            {{ t('material.uploadMaterial') }}
          </a-button>
        </div>

        <a-spin :spinning="materialStore.loading">
          <div class="material-grid">
            <div
              v-for="item in materialStore.materials"
              :key="item.id"
              class="material-card"
              :class="{ selected: tempSelectedIds.includes(item.id) || form.materialIds.includes(item.id) }"
              @click="toggleMaterialSelect(item)"
            >
              <div class="material-preview">
                <a-image
                  v-if="item.type === 'IMAGE'"
                  :src="item.thumbnailUrl || item.url"
                  style="width: 100%; height: 100%; object-fit: cover;"
                />
                <div v-else class="type-preview">
                  <FileTextOutlined />
                  <span>{{ item.type }}</span>
                </div>
                <div class="select-mark" v-if="tempSelectedIds.includes(item.id) || form.materialIds.includes(item.id)">
                  <CheckOutlined />
                </div>
              </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import { PlusOutlined, HolderOutlined, CheckOutlined, FileTextOutlined, UploadOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { useDeviceStore } from '@/store/device'
import { useMaterialStore } from '@/store/material'
import { publishApi } from '@/api/publish'
import type { Material } from '@/api/material'
import dayjs from 'dayjs'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const deviceStore = useDeviceStore()
const materialStore = useMaterialStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const enableSchedule = ref(false)

const form = reactive({
  deviceIds: [] as number[],
  materialIds: [] as number[],
  playMode: 'SEQUENCE' as 'SEQUENCE' | 'RANDOM',
  playInterval: 5,
  scheduledAt: null as any
})

const rules = {
  deviceIds: [
    { required: true, message: t('publish.selectDeviceRequired') }
  ],
  materialIds: [
    { required: true, message: t('publish.selectMaterialRequired') }
  ]
}

// 素材选择对话框
const showMaterialDialog = ref(false)
const dialogFilterType = ref('')
const dialogPage = ref(1)
const dialogPageSize = ref(12)
const tempSelectedIds = ref<number[]>([])

function disabledDate(current: dayjs.Dayjs) {
  return current && current < dayjs().startOf('day')
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function getMaterialName(id: number) {
  const material = materialStore.materials.find(m => m.id === id)
  return material?.name || `${t('publish.material')} ${id}`
}

function removeMaterial(id: number) {
  const index = form.materialIds.indexOf(id)
  if (index > -1) {
    form.materialIds.splice(index, 1)
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
  // 如果已经在已选列表中，不允许在对话框中取消
  if (form.materialIds.includes(material.id)) {
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
  // 将临时选择的素材添加到已选列表
  for (const id of tempSelectedIds.value) {
    if (!form.materialIds.includes(id)) {
      form.materialIds.push(id)
    }
  }
  tempSelectedIds.value = []
  showMaterialDialog.value = false
}

function goToMaterialUpload() {
  router.push({ name: 'MaterialUpload' })
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    await publishApi.create({
      deviceIds: form.deviceIds,
      materials: form.materialIds.map((materialId, sortOrder) => ({
        materialId,
        sortOrder
      })),
      playMode: form.playMode,
      playInterval: form.playInterval,
      scheduledAt: enableSchedule.value && form.scheduledAt ? form.scheduledAt.toISOString() : undefined
    })
    message.success(t('publish.publishSuccess'))
    router.push('/publish/records')
  } catch (error: any) {
    message.error(error.message || t('publish.publishFailed'))
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  formRef.value?.resetFields()
  form.deviceIds = []
  form.materialIds = []
  form.playMode = 'SEQUENCE'
  form.playInterval = 5
  form.scheduledAt = null
  enableSchedule.value = false
}

onMounted(async () => {
  // 获取设备列表
  await deviceStore.fetchDevices({ size: 100 })

  // 获取素材列表
  await fetchMaterials()

  // 如果从设备详情页跳转过来，预选设备
  const deviceId = route.query.deviceId
  if (deviceId) {
    form.deviceIds = [Number(deviceId)]
  }
})
</script>

<style scoped>
.publish-create {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.material-selector {
  width: 100%;
}

.selected-materials {
  margin-bottom: 12px;
}

.material-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.material-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.material-item:last-child {
  border-bottom: none;
}

.drag-handle {
  cursor: move;
  color: #909399;
}

.material-name {
  flex: 1;
}

/* 素材选择对话框 */
.material-dialog-content {
  min-height: 400px;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.material-preview {
  position: relative;
  width: 100%;
  height: 100px;
  background: #fafafa;
}

.type-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.select-mark {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: #1890ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
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
