<template>
  <div class="publish-records">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>{{ t('publish.records') }}</span>
          <a-button type="primary" @click="$router.push('/publish/create')">
            <template #icon><PlusOutlined /></template>
            {{ t('publish.newPublish') }}
          </a-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-select v-model:value="filterDeviceId" :placeholder="t('publish.selectDevice')" allowClear style="width: 180px" @change="handleFilter">
          <a-select-option :value="undefined">{{ t('publish.allDevices') }}</a-select-option>
          <a-select-option
            v-for="device in deviceStore.devices"
            :key="device.id"
            :value="device.id"
          >
            {{ device.name }}
          </a-select-option>
        </a-select>
        <a-select v-model:value="filterStatus" :placeholder="t('publish.publishStatus')" allowClear style="width: 120px" @change="handleFilter">
          <a-select-option value="">{{ t('common.all') }}</a-select-option>
          <a-select-option value="0">{{ t('publish.statusPending') }}</a-select-option>
          <a-select-option value="1">{{ t('publish.statusPublished') }}</a-select-option>
          <a-select-option value="2">{{ t('publish.statusPlaying') }}</a-select-option>
          <a-select-option value="3">{{ t('publish.statusCompleted') }}</a-select-option>
          <a-select-option value="4">{{ t('publish.statusCancelled') }}</a-select-option>
        </a-select>
        <a-range-picker
          v-model:value="filterDateRange"
          @change="handleFilter"
        />
      </div>

      <!-- 记录表格 -->
      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 'max-content', y: 'calc(100vh - 380px)' }"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'taskName'">
            <span>{{ record.name || `${t('device.task')} ${record.id}` }}</span>
          </template>
          <template v-if="column.key === 'device'">
            <span>{{ record.deviceName || `${t('publish.device')} ${record.deviceId}` }}</span>
          </template>
          <template v-if="column.key === 'itemCount'">
            <a-tag>{{ t('publish.itemCount', { count: record.itemCount || 0 }) }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ record.statusDesc }}
            </a-tag>
          </template>
          <template v-if="column.key === 'publishedAt'">
            {{ record.publishedAt ? formatTime(record.publishedAt) : '-' }}
          </template>
          <template v-if="column.key === 'completedAt'">
            {{ record.completedAt ? formatTime(record.completedAt) : '-' }}
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="showDetail(record)">
                {{ t('common.detail') }}
              </a-button>
              <a-popconfirm
                v-if="record.status === 0 || record.status === 1"
                :title="t('publish.cancelConfirm')"
                @confirm="handleCancel(record.id)"
              >
                <a-button type="link" danger>{{ t('publish.cancel') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情对话框 -->
    <a-modal v-model:open="detailVisible" :title="t('publish.detail')" :footer="null" width="600px">
      <a-descriptions :column="2" bordered v-if="currentRecord">
        <a-descriptions-item :label="t('publish.taskId')">{{ currentRecord.id }}</a-descriptions-item>
        <a-descriptions-item :label="t('publish.batchId')">{{ currentRecord.batchId }}</a-descriptions-item>
        <a-descriptions-item :label="t('common.status')">
          <a-tag :color="getStatusColor(currentRecord.status)">
            {{ currentRecord.statusDesc }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="t('publish.materialCount')">{{ currentRecord.itemCount || 0 }} {{ t('common.items') }}</a-descriptions-item>
        <a-descriptions-item :label="t('publish.device')">
          {{ currentRecord.deviceName || `${t('publish.device')} ${currentRecord.deviceId}` }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('publish.taskName')">
          {{ currentRecord.name || '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('common.createTime')">{{ formatTime(currentRecord.createdAt) }}</a-descriptions-item>
        <a-descriptions-item :label="t('publish.publishTimeLabel')">
          {{ currentRecord.publishedAt ? formatTime(currentRecord.publishedAt) : '-' }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('publish.completeTime')">
          {{ currentRecord.completedAt ? formatTime(currentRecord.completedAt) : '-' }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { publishApi, type PublishRecord } from '@/api/publish'
import { useDeviceStore } from '@/store/device'
import dayjs from 'dayjs'

const { t } = useI18n()
const route = useRoute()
const deviceStore = useDeviceStore()

const loading = ref(false)
const records = ref<PublishRecord[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref('')
const filterDeviceId = ref<number | undefined>()
const filterDateRange = ref<any[]>([])

const detailVisible = ref(false)
const currentRecord = ref<PublishRecord | null>(null)

const columns = computed(() => [
  { title: t('publish.taskId'), dataIndex: 'id', key: 'id', width: 80 },
  { title: t('publish.taskName'), key: 'taskName', width: 150 },
  { title: t('publish.device'), key: 'device', width: 150 },
  { title: t('publish.materialCount'), key: 'itemCount', width: 100 },
  { title: t('common.status'), key: 'status', width: 100 },
  { title: t('publish.publishTime'), key: 'publishedAt', width: 160 },
  { title: t('publish.completeTime'), key: 'completedAt', width: 160 },
  { title: t('common.createTime'), key: 'createdAt', width: 160 },
  { title: t('common.action'), key: 'action', width: 120, fixed: 'right' }
])

const pagination = computed(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => t('common.totalItems', { count: total })
}))

const getStatusColor = (status: number) => {
  const colors: Record<number, string> = {
    0: 'default',
    1: 'blue',
    2: 'green',
    3: 'success',
    4: 'error'
  }
  return colors[status] || 'default'
}

const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await publishApi.list({
      page: currentPage.value,
      size: pageSize.value,
      status: filterStatus.value || undefined,
      deviceId: filterDeviceId.value
    })
    if (res.success) {
      records.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error: any) {
    message.error(error.message || t('publish.fetchRecordsFailed'))
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  currentPage.value = 1
  fetchRecords()
}

const handleTableChange = (pag: any) => {
  currentPage.value = pag.current
  pageSize.value = pag.pageSize
  fetchRecords()
}

const showDetail = (record: PublishRecord) => {
  currentRecord.value = record
  detailVisible.value = true
}

const handleCancel = async (taskId: number) => {
  try {
    const res = await publishApi.cancel(taskId)
    if (res.success) {
      message.success(t('publish.taskCancelled'))
      fetchRecords()
    }
  } catch (error: any) {
    message.error(error.message || t('publish.cancelFailed'))
  }
}

onMounted(() => {
  deviceStore.fetchDevices()
  fetchRecords()
})
</script>

<style scoped>
.publish-records {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-bar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}
</style>
