import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type MessageSchema = typeof zhCN

const STORAGE_KEY = 'app-language'

// 获取存储的语言或默认语言
function getDefaultLocale(): string {
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored && (stored === 'zh-CN' || stored === 'en-US')) {
    return stored
  }
  // 根据浏览器语言判断
  const browserLang = navigator.language || (navigator as any).userLanguage
  if (browserLang && browserLang.toLowerCase().includes('zh')) {
    return 'zh-CN'
  }
  return 'en-US'
}

const i18n = createI18n<[MessageSchema], 'zh-CN' | 'en-US'>({
  legacy: false,
  locale: getDefaultLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export function setLocale(locale: 'zh-CN' | 'en-US'): void {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  document.querySelector('html')?.setAttribute('lang', locale)
}

export function getLocale(): 'zh-CN' | 'en-US' {
  return i18n.global.locale.value as 'zh-CN' | 'en-US'
}

export default i18n
