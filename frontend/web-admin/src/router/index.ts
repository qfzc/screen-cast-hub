import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/Dashboard.vue'),
          meta: { title: 'menu.dashboard' }
        },
        {
          path: 'device',
          name: 'Device',
          redirect: '/device/list',
          children: [
            {
              path: 'list',
              name: 'DeviceList',
              component: () => import('@/views/device/DeviceList.vue'),
              meta: { title: 'menu.deviceList' }
            },
            {
              path: ':id',
              name: 'DeviceDetail',
              component: () => import('@/views/device/DeviceDetail.vue'),
              meta: { title: 'device.detail' }
            },
            {
              path: ':id/content',
              name: 'DeviceContent',
              component: () => import('@/views/device/DeviceContent.vue'),
              meta: { title: 'device.contentManage' }
            }
          ]
        },
        {
          path: 'material',
          name: 'Material',
          redirect: '/material/list',
          children: [
            {
              path: 'list',
              name: 'MaterialList',
              component: () => import('@/views/material/MaterialList.vue'),
              meta: { title: 'menu.materialList' }
            },
            {
              path: 'upload',
              name: 'MaterialUpload',
              component: () => import('@/views/material/MaterialUpload.vue'),
              meta: { title: 'menu.materialUpload' }
            }
          ]
        },
        {
          path: 'publish',
          name: 'Publish',
          redirect: '/publish/create',
          children: [
            {
              path: 'create',
              name: 'PublishCreate',
              component: () => import('@/views/publish/PublishCreate.vue'),
              meta: { title: 'menu.publishCreate' }
            },
            {
              path: 'records',
              name: 'PublishRecords',
              component: () => import('@/views/publish/PublishRecords.vue'),
              meta: { title: 'menu.publishRecords' }
            }
          ]
        }
      ]
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  // 如果有 token 但没有用户信息，先获取用户信息
  if (authStore.isLoggedIn && !authStore.user) {
    try {
      await authStore.fetchCurrentUser()
    } catch {
      // 获取用户信息失败，会自动 logout
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }

  if (to.meta.requiresAuth !== false && !authStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (to.name === 'Login' && authStore.isLoggedIn) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
