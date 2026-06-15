<template>
  <div id="app">
    <a-layout style="min-height: 100vh">
      <!-- 海洋主题导航栏（登录页不显示） -->
      <a-layout-header v-if="!isLoginPage" class="app-header">
        <div class="header-inner">
          <div class="header-left">
            <router-link to="/" class="logo">
              <span class="logo-icon">🌊</span>
              <span class="logo-text">海洋生物知识库</span>
            </router-link>
            <a-menu
              v-model:selectedKeys="selectedKeys"
              theme="dark"
              mode="horizontal"
              class="nav-menu"
            >
              <a-menu-item key="home">
                <home-outlined /> 首页
              </a-menu-item>
              <a-menu-item key="notes">
                <read-outlined /> 公开笔记
              </a-menu-item>
              <a-menu-item v-if="user.token" key="ai">
                <robot-outlined /> AI 问答
              </a-menu-item>
              <a-menu-item v-if="user.token && user.role === 'admin'" key="admin">
                <setting-outlined /> 管理后台
              </a-menu-item>
            </a-menu>
          </div>
          <div class="header-right">
            <template v-if="user.token">
              <div class="user-info">
                <a-avatar :size="32" class="user-avatar">{{ user.name?.charAt(0) }}</a-avatar>
                <span class="user-name">{{ user.name }}</span>
              </div>
              <a-button class="logout-btn" @click="logout">退出登录</a-button>
            </template>
            <template v-else>
              <a-button type="primary" class="login-btn" @click="$router.push('/login')">
                登录
              </a-button>
            </template>
          </div>
        </div>
      </a-layout-header>

      <!-- 主线 -->
      <a-layout-content class="app-content" :class="{ 'app-content--full': isLoginPage }">
        <router-view />
      </a-layout-content>

      <!-- 页脚（登录页不显示） -->
      <a-layout-footer v-if="!isLoginPage" class="app-footer">
        <div class="footer-wave"></div>
        <div class="footer-content">
          <div class="footer-links">
            <span>🌊 海洋生物知识库</span>
            <span class="footer-divider">|</span>
            <span>探索海洋奥秘</span>
            <span class="footer-divider">|</span>
            <span>保护蓝色星球</span>
          </div>
          <div class="footer-copyright">
            ©2026 Ocean Knowledge Base Team
          </div>
        </div>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { HomeOutlined, ReadOutlined, RobotOutlined, SettingOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'App',
  components: {
    HomeOutlined, ReadOutlined, RobotOutlined, SettingOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    const selectedKeys = ref<string[]>(['home'])
    const user = computed(() => store.state.user)
    const isLoginPage = computed(() => route.path === '/login')

    // 根据当前路由高亮菜单
    watch(() => route.path, (path) => {
      if (path === '/') selectedKeys.value = ['home']
      else if (path.startsWith('/notes')) selectedKeys.value = ['notes']
      else if (path.startsWith('/ai')) selectedKeys.value = ['ai']
      else if (path.startsWith('/admin')) selectedKeys.value = ['admin']
      else if (path.startsWith('/search')) selectedKeys.value = ['home']
    }, { immediate: true })

    const logout = async () => {
      try {
        await axios.get('/api/user/logout', {
          headers: { token: store.state.user.token }
        })
      } catch {
        // ignore logout error
      } finally {
        store.commit('setUser', {})
        sessionStorage.removeItem('user')
        router.push('/')
      }
    }

    return { selectedKeys, user, logout }
  }
})
</script>

<style scoped>
.app-header {
  height: 64px;
  line-height: 64px;
  padding: 0;
  background: linear-gradient(135deg, #0a1628 0%, #1a2a4a 50%, #0d2842 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 2px solid rgba(22, 119, 255, 0.3);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
}

.header-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  margin-right: 32px;
}

.logo-icon {
  font-size: 28px;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(90deg, #69b1ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-menu {
  flex: 1;
  background: transparent !important;
  border: none;
  height: 100%;
}

.nav-menu :deep(.ant-menu-item) {
  color: rgba(255, 255, 255, 0.7) !important;
  font-size: 14px;
  padding: 0 16px;
  border-bottom: 2px solid transparent !important;
  transition: all 0.3s ease;
}

.nav-menu :deep(.ant-menu-item:hover) {
  color: #fff !important;
  border-bottom-color: #1677ff !important;
}

.nav-menu :deep(.ant-menu-item-selected) {
  color: #fff !important;
  border-bottom-color: #1677ff !important;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}

.user-name {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
}

.login-btn {
  border-radius: 8px;
  font-weight: 500;
  height: 36px;
  padding: 0 20px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
}

.logout-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: rgba(255, 255, 255, 0.75);
  border-radius: 8px;
  height: 36px;
  font-size: 13px;
}

.logout-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.app-content {
  min-height: calc(100vh - 134px);
  background: linear-gradient(180deg, #e8f4fd 0%, #f0f9ff 50%, #f5faff 100%);
}

.app-content--full {
  min-height: 100vh;
}

.app-footer {
  padding: 0;
  background: #0a1628;
  position: relative;
}

.footer-wave {
  height: 8px;
  background: linear-gradient(90deg, #1677ff, #36cfc9, #1677ff);
  opacity: 0.6;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
  text-align: center;
}

.footer-links {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin-bottom: 8px;
}

.footer-divider {
  margin: 0 12px;
  color: rgba(255, 255, 255, 0.2);
}

.footer-copyright {
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
}
</style>