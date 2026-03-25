import { defineStore } from 'pinia'
import { ref } from 'vue'
import { deviceApi, type Device } from '@/api/device'

export const useDeviceStore = defineStore('device', () => {
  const devices = ref<Device[]>([])
  const currentDevice = ref<Device | null>(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchDevices(params?: { page?: number; size?: number; status?: number }) {
    loading.value = true
    try {
      const res = await deviceApi.list(params)
      devices.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchDeviceById(id: number) {
    loading.value = true
    try {
      const res = await deviceApi.getById(id)
      currentDevice.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function bindDevice(bindCode: string, name: string, groupId?: number) {
    const res = await deviceApi.bind({ bindCode, name, groupId })
    devices.value.unshift(res.data)
    return res.data
  }

  async function unbindDevice(id: number) {
    await deviceApi.unbind(id)
    const index = devices.value.findIndex(d => d.id === id)
    if (index > -1) {
      devices.value.splice(index, 1)
    }
  }

  async function renameDevice(id: number, name: string) {
    const res = await deviceApi.rename(id, name)
    const device = devices.value.find(d => d.id === id)
    if (device) {
      device.name = name
    }
    return res.data
  }

  return {
    devices,
    currentDevice,
    total,
    loading,
    fetchDevices,
    fetchDeviceById,
    bindDevice,
    unbindDevice,
    renameDevice
  }
})
