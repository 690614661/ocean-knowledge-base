<template>
  <a-layout style="padding: 24px 0">
    <a-layout-sider width="200" style="background: #fff">
      <a-menu mode="inline" :selectedKeys="[selectedKey]" @click="onMenuClick">
        <a-menu-item key="dashboard">📊 仪表盘</a-menu-item>
        <a-menu-item key="ebook">📖 电子书管理</a-menu-item>
        <a-menu-item key="category">📂 分类管理</a-menu-item>
        <a-menu-item key="doc">📄 文档管理</a-menu-item>
        <a-menu-item key="user">👤 用户管理</a-menu-item>
      </a-menu>
    </a-layout-sider>
    <a-layout-content style="padding: 0 24px; background: #fff; margin-left: 16px; border-radius: 8px">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import { useRouter } from 'vue-router'

export default defineComponent({
  name: 'AdminLayout',
  setup() {
    const router = useRouter()
    const selectedKey = ref('dashboard')

    const onMenuClick = ({ key }: { key: string }) => {
      selectedKey.value = key
      router.push(key === 'dashboard' ? '/admin' : `/admin/${key}`)
    }

    return { selectedKey, onMenuClick }
  }
})
</script>