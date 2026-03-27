<template>
  <div class="device-list">
    <a-card>
      <template #title>
        <div class="card-header">
          <span>{{ t('device.list') }}</span>
          <a-button type="primary" @click="showBindDialog">
            <template #icon><PlusOutlined /></template>
            {{ t('device.bindDevice') }}
          </a-button>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <a-select v-model:value="filterStatus" :placeholder="t('device.deviceStatus')" allowClear style="width: 120px" @change="handleFilter">
          <a-select-option :value="undefined">{{ t('common.all') }}</a-select-option>
          <a-select-option :value="1">{{ t('device.online') }}</a-select-option>
          <a-select-option :value="2">{{ t('device.offline') }}</a-select-option>
          <a-select-option :value="0">{{ t('device.unbound') }}</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="searchKeyword"
          :placeholder="t('device.searchDevice')"
          allowClear
          style="width: 200px"
          @search="handleFilter"
        />
      </div>

      <!-- 设备表格 -->
      <a-table
        :columns="columns"
        :data-source="deviceStore.devices"
        :loading="deviceStore.loading"
        :pagination="pagination"
        :scroll="{ x: 'max-content', y: 'calc(100vh - 380px)' }"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-button type="link" @click="$router.push(`/device/${record.id}`)">
              {{ record.name }}
            </a-button>
          </template>
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'lastHeartbeat'">
            {{ formatTime(record.lastHeartbeat) }}
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatTime(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="showRenameDialog(record)">{{ t('common.rename') }}</a-button>
              <a-button type="link" @click="goToPublish(record)">{{ t('device.publish') }}</a-button>
              <a-popconfirm
                :title="t('device.unbindConfirm')"
                @confirm="handleUnbind(record.id)"
              >
                <a-button type="link" danger>{{ t('device.unbind') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 绑定设备对话框 -->
    <a-modal v-model:open="bindDialogVisible" :title="t('device.bindDevice')" @ok="handleBind" :confirmLoading="bindLoading">
      <a-form ref="bindFormRef" :model="bindForm" :rules="bindRules" :label-col="{ span: 6 }">
        <a-form-item :label="t('device.bindCode')" name="bindCode">
          <a-input v-model:value="bindForm.bindCode" :placeholder="t('device.bindCodePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('device.deviceName')" name="name">
          <a-input v-model:value="bindForm.name" :placeholder="t('device.deviceNamePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重命名对话框 -->
    <a-modal v-model:open="renameDialogVisible" :title="t('device.renameDevice')" @ok="handleRename" :confirmLoading="renameLoading">
      <a-form ref="renameFormRef" :model="renameForm" :rules="renameRules" :label-col="{ span: 6 }">
        <a-form-item :label="t('device.deviceName')" name="name">
          <a-input v-model:value="renameForm.name" :placeholder="t('device.newNamePlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useDeviceStore } from '@/store/device'
import type { Device } from '@/api/device'

const { t } = useI18n()
const router = useRouter()
const deviceStore = useDeviceStore()

const currentPage = ref(1)
const pageSize = ref(10)
const filterStatus = ref<number | undefined>()
const searchKeyword = ref('')

const columns = computed(() => [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: t('device.deviceName'), dataIndex: 'name', key: 'name' },
  { title: t('device.bindCode'), dataIndex: 'bindCode', key: 'bindCode', width: 120 },
  { title: t('common.status'), dataIndex: 'status', key: 'status', width: 100 },
  { title: t('device.lastHeartbeat'), dataIndex: 'lastHeartbeat', key: 'lastHeartbeat', width: 180 },
  { title: t('common.createTime'), dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: t('common.actions'), key: 'action', width: 200 }
])

const pagination = computed(() => ({
  current: currentPage.value,
  pageSize: pageSize.value,
  total: deviceStore.total,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => t('common.total', { count: total })
}))

// 绑定对话框
const bindDialogVisible = ref(false)
const bindFormRef = ref<FormInstance>()
const bindLoading = ref(false)
const bindForm = reactive({
  bindCode: '',
  name: ''
})
const bindRules = computed(() => ({
  bindCode: [{ required: true, message: t('device.bindCodeRequired') }],
  name: [{ required: true, message: t('device.deviceNameRequired') }]
}))

// 重命名对话框
const renameDialogVisible = ref(false)
const renameFormRef = ref<FormInstance>()
const renameLoading = ref(false)
const currentDevice = ref<Device | null>(null)
const renameForm = reactive({
  name: ''
})
const renameRules = computed(() => ({
  name: [{ required: true, message: t('device.deviceNameRequired') }]
}))

function formatTime(time: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function getStatusColor(status: number): string {
  const colorMap: Record<number, string> = {
    0: 'default',
    1: 'green',
    2: 'orange'
  }
  return colorMap[status] ?? 'default'
}

function getStatusText(status: number): string {
  const textMap: Record<number, string> = {
    0: t('device.unbound'),
    1: t('device.online'),
    2: t('device.offline')
  }
  return textMap[status] ?? t('device.unbound')
}

async function fetchData() {
  await deviceStore.fetchDevices({
    page: currentPage.value,
    size: pageSize.value,
    status: filterStatus.value
  })
}

function handleFilter() {
  currentPage.value = 1
  fetchData()
}

function handleTableChange(pag: any) {
  currentPage.value = pag.current
  pageSize.value = pag.pageSize
  fetchData()
}

function showBindDialog() {
  bindForm.bindCode = ''
  bindForm.name = ''
  bindDialogVisible.value = true
}

async function handleBind() {
  try {
    await bindFormRef.value?.validate()
  } catch {
    return
  }

  bindLoading.value = true
  try {
    await deviceStore.bindDevice(bindForm.bindCode, bindForm.name)
    message.success(t('device.bindSuccess'))
    bindDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    message.error(error.message || t('device.bindFailed'))
  } finally {
    bindLoading.value = false
  }
}

function showRenameDialog(device: Device) {
  currentDevice.value = device
  renameForm.name = device.name
  renameDialogVisible.value = true
}

async function handleRename() {
  try {
    await renameFormRef.value?.validate()
  } catch {
    return
  }

  if (!currentDevice.value) return

  renameLoading.value = true
  try {
    await deviceStore.renameDevice(currentDevice.value.id, renameForm.name)
    message.success(t('device.renameSuccess'))
    renameDialogVisible.value = false
    fetchData()
  } catch (error: any) {
    message.error(error.message || t('device.renameFailed'))
  } finally {
    renameLoading.value = false
  }
}

async function handleUnbind(id: number) {
  try {
    await deviceStore.unbindDevice(id)
    message.success(t('device.unbindSuccess'))
    fetchData()
  } catch (error: any) {
    message.error(error.message || t('device.unbindFailed'))
  }
}

function goToPublish(device: Device) {
  router.push({
    path: '/publish/create',
    query: { deviceId: String(device.id) }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.device-list {
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
