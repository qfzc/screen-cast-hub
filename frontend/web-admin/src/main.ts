import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import enUS from 'ant-design-vue/es/locale/en_US'
import * as AntdIcons from '@ant-design/icons-vue'

import App from './App.vue'
import router from './router'
import i18n, { getLocale } from './locales'
import './styles/index.scss'

// Ant Design Vue locale map
export const antdLocales: Record<string, any> = {
  'zh-CN': zhCN,
  'en-US': enUS
}

const app = createApp(App)

// Register Ant Design Icons
for (const [key, component] of Object.entries(AntdIcons)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(i18n)
app.use(Antd, { locale: antdLocales[getLocale()] })

app.mount('#app')
