<template>
  <div
    class="material-thumbnail"
    :class="[
      `size-${size}`,
      { selected, rounded }
    ]"
    :style="containerStyle"
  >
    <!-- 有缩略图时显示缩略图 -->
    <img
      v-if="thumbnailSrc"
      :src="thumbnailSrc"
      :alt="material?.name"
      class="thumbnail-image"
      :style="{ objectFit }"
    />
    <!-- 无缩略图时显示类型图标 -->
    <div v-else class="type-icon-wrapper">
      <FileImageOutlined v-if="normalizedType === 'IMAGE'" :style="{ fontSize: iconSize + 'px' }" />
      <VideoCameraOutlined v-else-if="normalizedType === 'VIDEO'" :style="{ fontSize: iconSize + 'px' }" />
      <FilePdfOutlined v-else-if="normalizedType === 'PDF'" :style="{ fontSize: iconSize + 'px' }" />
      <FileTextOutlined v-else-if="normalizedType === 'PPT'" :style="{ fontSize: iconSize + 'px' }" />
      <FileOutlined v-else :style="{ fontSize: iconSize + 'px' }" />
    </div>
    <!-- 选中标记 -->
    <div v-if="selected" class="selected-mark">
      <CheckOutlined />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FileImageOutlined,
  VideoCameraOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'

interface MaterialData {
  type?: string
  thumbnailUrl?: string
  url?: string
  materialUrl?: string
  name?: string
}

interface Props {
  material?: MaterialData
  size?: 'small' | 'medium' | 'large'
  width?: number | string
  height?: number | string
  objectFit?: 'cover' | 'contain' | 'fill'
  selected?: boolean
  rounded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  objectFit: 'cover',
  selected: false,
  rounded: true
})

// 尺寸映射
const sizeMap = {
  small: { width: 48, height: 48, iconSize: 24 },
  medium: { width: 60, height: 60, iconSize: 28 },
  large: { width: '100%', height: 140, iconSize: 40 }
}

// 计算图标大小
const iconSize = computed(() => {
  return sizeMap[props.size].iconSize
})

// 计算容器样式
const containerStyle = computed(() => {
  if (props.width !== undefined || props.height !== undefined) {
    return {
      width: typeof props.width === 'number' ? `${props.width}px` : props.width,
      height: typeof props.height === 'number' ? `${props.height}px` : props.height
    }
  }
  const sizeConfig = sizeMap[props.size]
  return {
    width: typeof sizeConfig.width === 'number' ? `${sizeConfig.width}px` : sizeConfig.width,
    height: typeof sizeConfig.height === 'number' ? `${sizeConfig.height}px` : sizeConfig.height
  }
})

// 标准化素材类型 (处理大小写)
const normalizedType = computed(() => {
  return props.material?.type?.toUpperCase() || ''
})

// 获取缩略图URL
const thumbnailSrc = computed(() => {
  return props.material?.thumbnailUrl || props.material?.url || props.material?.materialUrl
})
</script>

<style scoped>
.material-thumbnail {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  overflow: hidden;
  flex-shrink: 0;
}

.material-thumbnail.rounded {
  border-radius: 4px;
}

.material-thumbnail.selected {
  border: 2px solid #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.thumbnail-image {
  width: 100%;
  height: 100%;
}

.type-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
}

.selected-mark {
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
  font-size: 12px;
}

/* 尺寸特定样式 */
.size-small .selected-mark,
.size-medium .selected-mark {
  top: 4px;
  right: 4px;
  width: 16px;
  height: 16px;
  font-size: 10px;
}
</style>
