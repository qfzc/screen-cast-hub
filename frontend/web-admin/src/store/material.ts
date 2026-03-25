import { defineStore } from 'pinia'
import { ref } from 'vue'
import { materialApi, type Material } from '@/api/material'

export const useMaterialStore = defineStore('material', () => {
  const materials = ref<Material[]>([])
  const currentMaterial = ref<Material | null>(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchMaterials(params?: { page?: number; size?: number; type?: string }) {
    loading.value = true
    try {
      const res = await materialApi.list(params)
      materials.value = res.data.list
      total.value = res.data.total
    } finally {
      loading.value = false
    }
  }

  async function fetchMaterialById(id: number) {
    loading.value = true
    try {
      const res = await materialApi.getById(id)
      currentMaterial.value = res.data
      return res.data
    } finally {
      loading.value = false
    }
  }

  async function uploadMaterial(file: File, onProgress?: (percent: number) => void) {
    const res = await materialApi.upload(file, onProgress)
    // Refresh list after upload
    await fetchMaterials()
    return res.data
  }

  async function deleteMaterial(id: number) {
    await materialApi.delete(id)
    const index = materials.value.findIndex(m => m.id === id)
    if (index > -1) {
      materials.value.splice(index, 1)
    }
  }

  return {
    materials,
    currentMaterial,
    total,
    loading,
    fetchMaterials,
    fetchMaterialById,
    uploadMaterial,
    deleteMaterial
  }
})
