<template>
  <div class="material-upload">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>{{ t('material.uploadTitle') }}</span>
          <a-button type="link" @click="$router.push('/material/list')">
            {{ t('material.returnToList') }}
          </a-button>
        </div>
      </template>

      <a-upload-dragger
        v-model:fileList="fileList"
        :autoUpload="false"
        :accept="acceptTypes"
        :multiple="true"
        @change="handleFileChange"
      >
        <p class="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p class="ant-upload-text">{{ t('material.dragUpload') }}</p>
        <p class="ant-upload-hint">
          {{ t('material.uploadHint') }}
        </p>
      </a-upload-dragger>

      <!-- 上传队列 -->
      <div class="upload-queue" v-if="fileList.length > 0">
        <h3>{{ t('material.uploadQueue') }}</h3>
        <div class="queue-list">
          <div v-for="file in fileList" :key="file.uid" class="queue-item">
            <div class="file-info">
              <FileTextOutlined class="file-icon" />
              <div class="file-details">
                <span class="file-name">{{ file.name }}</span>
                <span class="file-size">{{ formatSize(file.size || 0) }}</span>
              </div>
            </div>
            <div class="file-status">
              <a-progress
                v-if="uploadingFiles[file.uid]"
                :percent="uploadingFiles[file.uid].progress"
                :status="uploadingFiles[file.uid].status"
              />
              <a-tag v-else-if="completedFiles.includes(file.uid)" color="success">
                {{ t('material.completed') }}
              </a-tag>
              <span v-else class="status-text">{{ t('material.waitingUpload') }}</span>
            </div>
          </div>
        </div>

        <div class="upload-actions">
          <a-button @click="clearFiles">{{ t('material.clearQueue') }}</a-button>
          <a-button
            type="primary"
            :loading="uploading"
            :disabled="pendingFiles.length === 0"
            @click="startUpload"
          >
            {{ t('material.startUpload') }}
          </a-button>
        </div>
      </div>

      <!-- 上传统计 -->
      <div class="upload-stats" v-if="totalFiles > 0">
        <a-statistic :title="t('material.totalFiles')" :value="totalFiles" />
        <a-statistic :title="t('material.uploaded')" :value="completedFiles.length" />
        <a-statistic :title="t('material.uploading')" :value="uploadingCount" />
        <a-statistic :title="t('material.pendingUpload')" :value="pendingFiles.length" />
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message, type UploadFile } from 'ant-design-vue'
import { InboxOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { useMaterialStore } from '@/store/material'

const { t } = useI18n()
const materialStore = useMaterialStore()

const fileList = ref<UploadFile[]>([])
const uploading = ref(false)
const uploadingFiles = ref<Record<string, { progress: number; status: '' | 'success' | 'exception' }>>({})
const completedFiles = ref<number[]>([])

const acceptTypes = '.jpg,.jpeg,.png,.gif,.bmp,.webp,.mp4,.webm,.mov,.pdf,.ppt,.pptx'

const totalFiles = computed(() => fileList.value.length)

const uploadingCount = computed(() => {
  return Object.values(uploadingFiles.value).filter(f => f.status === '').length
})

const pendingFiles = computed(() => {
  return fileList.value.filter(f =>
    !uploadingFiles.value[f.uid] &&
    !completedFiles.value.includes(f.uid as number)
  )
})

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function handleFileChange() {
  // 文件列表已自动更新
}

function clearFiles() {
  fileList.value = []
  uploadingFiles.value = {}
  completedFiles.value = []
}

async function startUpload() {
  if (pendingFiles.value.length === 0) return

  uploading.value = true

  for (const file of pendingFiles.value) {
    if (!file.originFileObj) continue

    uploadingFiles.value[file.uid as string] = { progress: 0, status: '' }

    try {
      await materialStore.uploadMaterial(file.originFileObj, (percent) => {
        uploadingFiles.value[file.uid as string].progress = percent
      })
      uploadingFiles.value[file.uid as string].status = 'success'
      completedFiles.value.push(file.uid as number)
    } catch (error: any) {
      uploadingFiles.value[file.uid as string].status = 'exception'
      message.error(t('material.uploadFailed', { name: file.name, error: error.message || t('publish.unknown') }))
    }
  }

  uploading.value = false

  const successCount = completedFiles.value.length
  const totalCount = fileList.value.length

  if (successCount === totalCount) {
    message.success(t('material.allUploadComplete'))
  } else {
    message.warning(t('material.partialUploadComplete', { success: successCount, total: totalCount }))
  }
}
</script>

<style scoped>
.material-upload {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-area {
  margin-bottom: 24px;
}

.upload-queue {
  margin-top: 24px;
}

.upload-queue h3 {
  margin-bottom: 16px;
  font-size: 16px;
  color: #303133;
}

.queue-list {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
}

.queue-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.queue-item:last-child {
  border-bottom: none;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-icon {
  font-size: 24px;
  color: #909399;
}

.file-details {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-size: 14px;
  color: #303133;
}

.file-size {
  font-size: 12px;
  color: #909399;
}

.file-status {
  min-width: 200px;
}

.status-text {
  color: #909399;
  font-size: 12px;
}

.upload-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.upload-stats {
  display: flex;
  gap: 40px;
  margin-top: 24px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}
</style>
