<template>
  <a-config-provider :locale="antdLocale">
    <router-view />
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { antdLocales } from './main'

const { locale } = useI18n()

const antdLocale = computed(() => {
  return antdLocales[locale.value] || antdLocales['zh-CN']
})
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}
</style>
