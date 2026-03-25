<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>{{ t('login.title') }}</h1>
        <p>{{ t('login.subtitle') }}</p>
      </div>

      <a-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @finish="handleLogin"
      >
        <a-form-item name="username">
          <a-input
            v-model:value="loginForm.username"
            :placeholder="t('login.usernamePlaceholder')"
            size="large"
          >
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>

        <a-form-item name="password">
          <a-input-password
            v-model:value="loginForm.password"
            :placeholder="t('login.passwordPlaceholder')"
            size="large"
            @pressEnter="handleLogin"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            html-type="submit"
          >
            {{ t('login.login') }}
          </a-button>
        </a-form-item>
      </a-form>

      <!-- Language Switcher -->
      <div class="lang-switcher">
        <a-button type="link" @click="toggleLanguage">
          {{ locale === 'zh-CN' ? 'English' : '中文' }}
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, type FormInstance } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/store/auth'
import { setLocale } from '@/locales'

const { t, locale } = useI18n()
const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = computed(() => ({
  username: [
    { required: true, message: t('login.usernameRequired') }
  ],
  password: [
    { required: true, message: t('login.passwordRequired') },
    { min: 6, message: t('login.passwordMinLength') }
  ]
}))

function toggleLanguage() {
  const newLocale = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  setLocale(newLocale)
  window.location.reload()
}

async function handleLogin() {
  loading.value = true
  try {
    await authStore.login(loginForm.username, loginForm.password)
    message.success(t('login.loginSuccess'))

    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } catch (error: any) {
    message.error(error.message || t('login.loginFailed'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: #909399;
}

.login-form {
  width: 100%;
}

.login-btn {
  width: 100%;
}

.lang-switcher {
  text-align: center;
  margin-top: 16px;
}
</style>
