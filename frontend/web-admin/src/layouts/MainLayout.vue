<template>
  <a-layout class="main-layout">
    <a-layout-sider
      v-model:collapsed="isCollapse"
      :width="220"
      :collapsedWidth="64"
      :trigger="null"
      collapsible
      class="sidebar"
    >
      <div class="logo">
        <img src="@/assets/images/logo.png" alt="Logo" class="logo-img" />
        <span v-if="!isCollapse" class="logo-text">{{ t('login.title') }}</span>
      </div>
      <div class="menu-wrapper">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          theme="dark"
          :inline-collapsed="isCollapse"
          @click="handleMenuClick"
        >
          <a-menu-item key="/dashboard">
            <template #icon>
              <HomeOutlined />
            </template>
            {{ t('menu.dashboard') }}
          </a-menu-item>

          <a-sub-menu key="/device">
            <template #icon>
              <DesktopOutlined />
            </template>
            <template #title>{{ t('menu.device') }}</template>
            <a-menu-item key="/device/list">{{ t('menu.deviceList') }}</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="/material">
            <template #icon>
              <FolderOutlined />
            </template>
            <template #title>{{ t('menu.material') }}</template>
            <a-menu-item key="/material/list">{{ t('menu.materialList') }}</a-menu-item>
            <a-menu-item key="/material/upload">{{ t('menu.materialUpload') }}</a-menu-item>
          </a-sub-menu>

          <a-sub-menu key="/publish">
            <template #icon>
              <UploadOutlined />
            </template>
            <template #title>{{ t('menu.publish') }}</template>
            <a-menu-item key="/publish/create">{{ t('menu.publishCreate') }}</a-menu-item>
            <a-menu-item key="/publish/records">{{ t('menu.publishRecords') }}</a-menu-item>
          </a-sub-menu>
        </a-menu>
      </div>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="header">
        <div class="header-left">
          <span class="collapse-btn" @click="isCollapse = !isCollapse">
            <MenuUnfoldOutlined v-if="isCollapse" />
            <MenuFoldOutlined v-else />
          </span>
          <a-breadcrumb>
            <a-breadcrumb-item>
              <router-link to="/">{{ t('menu.home') }}</router-link>
            </a-breadcrumb-item>
            <a-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <!-- Language Switcher -->
          <a-dropdown>
            <span class="lang-dropdown">
              <GlobalOutlined />
              <span class="lang-text">{{ locale === 'zh-CN' ? '中文' : 'EN' }}</span>
              <DownOutlined />
            </span>
            <template #overlay>
              <a-menu @click="handleLanguageChange">
                <a-menu-item key="zh-CN" :class="{ 'ant-dropdown-menu-item-selected': locale === 'zh-CN' }">
                  中文
                </a-menu-item>
                <a-menu-item key="en-US" :class="{ 'ant-dropdown-menu-item-selected': locale === 'en-US' }">
                  English
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>

          <!-- User Dropdown -->
          <a-dropdown>
            <span class="user-dropdown">
              <UserOutlined />
              {{ authStore.user?.username || t('menu.user') }}
              <DownOutlined />
            </span>
            <template #overlay>
              <a-menu @click="handleCommand">
                <a-menu-item key="logout">{{ t('menu.logout') }}</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout-content class="main-content">
        <div class="page-wrapper">
          <router-view />
        </div>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/store/auth'
import { setLocale } from '@/locales'
import {
  HomeOutlined,
  DesktopOutlined,
  FolderOutlined,
  UploadOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  DownOutlined,
  GlobalOutlined
} from '@ant-design/icons-vue'

const { t, locale } = useI18n()
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCollapse = ref(false)
const selectedKeys = ref<string[]>([route.path])

const currentTitle = computed(() => {
  const titleKey = route.meta.title as string | undefined
  return titleKey ? t(titleKey) : undefined
})

watch(() => route.path, (newPath) => {
  selectedKeys.value = [newPath]
})

function handleMenuClick({ key }: { key: string }) {
  router.push(key)
}

function handleCommand({ key }: { key: string }) {
  if (key === 'logout') {
    authStore.logout()
  }
}

function handleLanguageChange({ key }: { key: string }) {
  if (key === 'zh-CN' || key === 'en-US') {
    setLocale(key)
    // Reload the page to apply Ant Design locale
    window.location.reload()
  }
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #001529 !important;
}

.sidebar :deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background-color: #002140;
  padding: 0 16px;
}

.logo-img {
  width: 48px;
  height: 48px;
  object-fit: contain;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
  overflow: hidden;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #1890ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.lang-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  padding: 0 8px;
}

.lang-dropdown:hover {
  color: #1890ff;
}

.lang-text {
  font-size: 14px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
}

.user-dropdown:hover {
  color: #1890ff;
}

.main-content {
  background-color: #f0f2f5;
  height: calc(100vh - 64px);
  overflow: hidden;
}

.page-wrapper {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
}

.menu-wrapper {
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.menu-wrapper::-webkit-scrollbar,
.page-wrapper::-webkit-scrollbar {
  width: 6px;
}

.menu-wrapper::-webkit-scrollbar-thumb,
.page-wrapper::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

.page-wrapper::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
}

.page-wrapper::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}
</style>
