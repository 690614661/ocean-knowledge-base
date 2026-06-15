<template>
  <div id="app">
    <a-layout style="min-height: 100vh">
      <a-layout-header style="display: flex; align-items: center; justify-content: space-between; padding: 0 24px">
        <div style="display: flex; align-items: center">
          <router-link to="/" style="color: #fff; font-size: 18px; font-weight: bold; margin-right: 32px; text-decoration: none">
            🐠 海洋生物知识库
          </router-link>
          <a-menu
            v-model:selectedKeys="selectedKeys"
            theme="dark"
            mode="horizontal"
            style="flex: 1; border: none"
          >
            <a-menu-item key="home" @click="$router.push('/')">首页</a-menu-item>
            <a-menu-item key="notes" @click="$router.push('/notes')">公开笔记</a-menu-item>
            <a-menu-item v-if="user.token" key="ai" @click="$router.push('/ai')">AI 问答</a-menu-item>
            <a-menu-item v-if="user.token && user.role === 'admin'" key="admin" @click="$router.push('/admin')">管理后台</a-menu-item>
          </a-menu>
        </div>
        <div>
          <template v-if="user.token">
            <span style="color: #fff; margin-right: 16px">{{ user.name }}</span>
            <a-button type="link" style="color: #fff" @click="logout">退出</a-button>
          </template>
          <template v-else>
            <a-button type="primary" @click="$router.push('/login')">登录</a-button>
          </template>
        </div>
      </a-layout-header>
      <a-layout-content style="padding: 0 24px; background: #f0f2f5">
        <router-view />
      </a-layout-content>
      <a-layout-footer style="text-align: center; color: #999">
        海洋生物知识库 ©2026 Created by Ocean Team
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import axios from 'axios'

export default defineComponent({
  name: 'App',
  setup() {
    const store = useStore()
    const router = useRouter()
    const selectedKeys = ref<string[]>(['home'])
    const user = computed(() => store.state.user)

    const logout = () => {
      axios.get('/api/user/logout', {
        headers: { token: store.state.user.token }
      }).finally(() => {
        store.commit('setUser', {})
        sessionStorage.removeItem('user')
        router.push('/')
      })
    }

    return { selectedKeys, user, logout }
  }
})
</script>

<style>
#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>