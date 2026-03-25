<template>
  <div class="material-list">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>{{ t('material.library') }}</span>
          <a-button type="primary" @click="$router.push('/material/upload')">
            <template #icon><UploadOutlined /></template>
            {{ t('material.uploadMaterial') }}
          </a-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-select v-model:value="filterType" :placeholder="t('material.materialType')" allowClear style="width: 120px" @change="handleFilter">
          <a-select-option value="">{{ t('common.all') }}</a-select-option>
          <a-select-option value="image">{{ t('material.image') }}</a-select-option>
          <a-select-option value="video">{{ t('material.video') }}</a-select-option>
          <a-select-option value="pdf">{{ t('material.pdf') }}</a-select-option>
        </a-select>
      </div>

      <!-- 素材网格 -->
      <a-spin :spinning="materialStore.loading">
        <div class="material-grid">
          <div
            v-for="item in materialStore.materials"
            :key="item.id"
            class="material-card"
            :class="{ selected: selectedIds.includes(item.id) }"
            @click="toggleSelect(item.id)"
          >
            <div class="material-preview">
              <MaterialThumbnail :material="item" size="large" :selected="selectMode && selectedIds.includes(item.id)" />
              <div class="conversion-status" v-if="item.convertStatus === 1 || item.convertStatus === 3">
                <a-tag
                  :color="item.convertStatus === 3 ? 'red' : 'orange'"
                >
                  {{ getConversionStatusText(item.convertStatus) }}
                </a-tag>
              </div>
              <div class="select-checkbox" v-if="selectMode">
                <a-checkbox :checked="selectedIds.includes(item.id)" />
              </div>
            </div>
            <div class="material-info">
              <div class="material-name" :title="item.name">{{ item.name }}</div>
              <div class="material-meta">
                <span class="type-tag">{{ item.type }}</span>
                <span class="size-text">{{ formatSize(item.fileSize) }}</span>
              </div>
            </div>
            <div class="material-actions">
              <a-button type="link" size="small" @click.stop="handlePreview(item)">
                {{ t('material.preview') }}
              </a-button>
              <a-popconfirm
                :title="t('material.deleteConfirm')"
                @confirm="handleDelete(item.id)"
              >
                <a-button type="link" danger size="small" @click.stop>
                  {{ t('material.delete') }}
                </a-button>
              </a-popconfirm>
            </div>
          </div>

          <a-empty v-if="materialStore.materials.length === 0 && !materialStore.loading" :description="t('material.noMaterials')" />
        </div>
      </a-spin>

      <!-- 批量操作栏 -->
      <div class="batch-actions" v-if="selectMode">
        <span class="selected-count">{{ t('material.selectedMaterials', { count: selectedIds.length }) }}</span>
        <a-button @click="selectMode = false">{{ t('material.cancelSelect') }}</a-button>
        <a-button type="primary" danger :disabled="selectedIds.length === 0" @click="handleBatchDelete">
          {{ t('material.batchDelete') }}
        </a-button>
      </div>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <a-pagination
          v-model:current="currentPage"
          v-model:pageSize="pageSize"
          :pageSizeOptions="['12', '24', '48']"
          :total="materialStore.total"
          show-size-changer
          show-quick-jumper
          :show-total="(total: number) => t('common.total', { count: total })"
          @change="fetchData"
          @showSizeChange="fetchData"
        />
      </div>
    </a-card>

    <!-- 预览对话框 -->
    <a-modal v-model:open="previewVisible" :title="previewMaterial?.name" width="80%" :footer="null">
      <div class="preview-content">
        <img
          v-if="previewMaterial?.type === 'image'"
          :src="getMaterialUrl(previewMaterial)"
          :alt="previewMaterial?.name"
          style="width: 100%; max-height: 70vh; object-fit: contain;"
        />
        <video
          v-else-if="previewMaterial?.type === 'video'"
          :src="getMaterialUrl(previewMaterial)"
          controls
          style="width: 100%; max-height: 70vh;"
        />
        <iframe
          v-else-if="previewMaterial?.type === 'pdf' || previewMaterial?.type === 'ppt'"
          :src="getMaterialUrl(previewMaterial)"
          style="width: 100%; height: 70vh; border: none;"
        />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { UploadOutlined } from '@ant-design/icons-vue'

const { t } = useI18n()
import { useMaterialStore } from '@/store/material'
import type { Material } from '@/api/material'
import MaterialThumbnail from '@/components/MaterialThumbnail.vue'

const materialStore = useMaterialStore()

const currentPage = ref(1)
const pageSize = ref(12)
const filterType = ref('')

const selectMode = ref(false)
const selectedIds = ref<number[]>([])

const previewVisible = ref(false)
const previewMaterial = ref<Material | null>(null)

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function getConversionStatusText(status: number) {
  const map: Record<number, string> = {
    1: t('material.converting'),
    3: t('material.convertFailed')
  }
  return map[status] || ''
}

async function fetchData() {
  await materialStore.fetchMaterials({
    page: currentPage.value,
    size: pageSize.value,
    type: filterType.value || undefined
  })
}

function handleFilter() {
  currentPage.value = 1
  fetchData()
}

function toggleSelect(id: number) {
  if (!selectMode.value) {
    selectMode.value = true
  }
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
    if (selectedIds.value.length === 0) {
      selectMode.value = false
    }
  } else {
    selectedIds.value.push(id)
  }
}

function handlePreview(material: Material) {
  previewMaterial.value = material
  previewVisible.value = true
}

async function handleDelete(id: number) {
  try {
    await materialStore.deleteMaterial(id)
    message.success('删除成功')
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

async function handleBatchDelete() {
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedIds.value.length} 个素材吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      for (const id of selectedIds.value) {
        await materialStore.deleteMaterial(id)
      }
      message.success('批量删除成功')
      selectedIds.value = []
      selectMode.value = false
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.material-list {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  margin-bottom: 16px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  min-height: 200px;
}

.material-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
}

.material-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.material-card.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.material-preview {
  position: relative;
  width: 100%;
  height: 140px;
}

.conversion-status {
  position: absolute;
  top: 8px;
  right: 8px;
}

.select-checkbox {
  position: absolute;
  top: 8px;
  left: 8px;
}

.material-info {
  padding: 12px;
}

.material-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: 12px;
}

.type-tag {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #606266;
}

.size-text {
  color: #909399;
}

.material-actions {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-top: 16px;
}

.selected-count {
  color: #606266;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.preview-content {
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>
