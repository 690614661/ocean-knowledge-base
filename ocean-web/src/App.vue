<template>
  <div id="app">
    <!-- 登录页独立渲染，不受布局影响 -->
    <router-view v-if="isLoginPage" />

    <a-layout v-else style="min-height: 100vh">
      <!-- 导航栏 -->
      <a-layout-header class="app-header">
        <div class="header-inner">
          <router-link to="/" class="logo">
            <span class="logo-icon">🌊</span>
            <span class="logo-text">海洋生物知识库</span>
          </router-link>

          <nav class="nav-links">
            <router-link to="/" class="nav-item" :class="{ active: currentPath === '/' }">
              <home-outlined /> 首页
            </router-link>
            <router-link to="/notes" class="nav-item" :class="{ active: currentPath.startsWith('/notes') }">
              <read-outlined /> 公开笔记
            </router-link>
            <router-link v-if="user.token" to="/ai" class="nav-item" :class="{ active: currentPath.startsWith('/ai') }">
              <robot-outlined /> AI 问答
            </router-link>
            <router-link v-if="user.token && user.role === 'admin'" to="/admin" class="nav-item" :class="{ active: currentPath.startsWith('/admin') }">
              <setting-outlined /> 管理后台
            </router-link>
          </nav>

          <div class="header-right">
            <div class="school-badge" title="广东海洋大学">
              <span class="school-text">GDOU</span>
            </div>
            <template v-if="user.token">
              <div class="user-info">
                <a-avatar :size="30" class="user-avatar">{{ user.name?.charAt(0) }}</a-avatar>
                <span class="user-name">{{ user.name }}</span>
              </div>
              <a-button class="logout-btn" size="small" @click="logout">退出</a-button>
            </template>
            <template v-else>
              <a-button type="primary" class="login-btn" size="small" @click="toLogin">登录</a-button>
            </template>
          </div>
        </div>
      </a-layout-header>

      <a-layout-content class="app-content">
        <router-view />
      </a-layout-content>

      <a-layout-footer class="app-footer">
        <div class="footer-wave"></div>
        <div class="footer-content">
          <div class="footer-links">
            <span>🌊 海洋生物知识库</span>
            <span class="footer-divider">|</span>
            <span>广东海洋大学 实训项目</span>
            <span class="footer-divider">|</span>
            <span>探索海洋奥秘 · 保护蓝色星球</span>
          </div>
          <div class="footer-copyright">
            ©2026 Guangdong Ocean University - Ocean Knowledge Base Team
          </div>
        </div>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed } from 'vue'
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
    const user = computed(() => store.state.user)
    const isLoginPage = computed(() => route.path === '/login')
    const currentPath = computed(() => route.path)

    const toLogin = () => router.push('/login')

    const logout = async () => {
      try {
        await axios.get('/api/user/logout', {
          headers: { token: store.state.user.token }
        })
      } catch {
        // ignore
      } finally {
        store.commit('setUser', {})
        sessionStorage.removeItem('user')
        router.push('/')
      }
    }

    return { user, logout, isLoginPage, currentPath, toLogin }
  }
})
</script>

<style scoped>
.app-header {
  height: 56px;
  line-height: 56px;
  padding: 0;
  background: linear-gradient(135deg, #0a1628 0%, #1a3a5c 50%, #0d2842 100%);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 2px solid rgba(22, 119, 255, 0.3);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  align-items: center;
  height: 100%;
  gap: 8px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(90deg, #69b1ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  margin: 0 auto 0 24px;
  flex: 1;
}

.nav-item {
  color: rgba(255, 255, 255, 0.65);
  font-size: 13px;
  padding: 0 14px;
  height: 56px;
  line-height: 56px;
  text-decoration: none;
  border-bottom: 2px solid transparent;
  transition: all 0.25s ease;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.nav-item:hover {
  color: #fff;
  border-bottom-color: #1677ff;
}

.nav-item.active {
  color: #fff;
  border-bottom-color: #1677ff;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.school-badge {
  padding: 2px 10px;
  border: 1px solid rgba(54, 207, 201, 0.35);
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 1px;
  cursor: default;
}

.school-text {
  background: linear-gradient(90deg, #69b1ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-avatar {
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: #fff;
  font-weight: 600;
  font-size: 12px;
}

.user-name {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.login-btn {
  border-radius: 6px;
  font-weight: 500;
  height: 32px;
  padding: 0 16px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  box-shadow: 0 4px 12px rgba(22, 119, 255, 0.3);
  font-size: 13px;
}

.logout-btn {
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: rgba(255, 255, 255, 0.75);
  border-radius: 6px;
  height: 32px;
  font-size: 12px;
}

.logout-btn:hover {
  border-color: #ff4d4f;
  color: #ff4d4f;
}

.app-content {
  min-height: calc(100vh - 134px);
  background: linear-gradient(180deg, #e8f4fd 0%, #f0f9ff 50%, #f5faff 100%);
}

.app-footer {
  padding: 0;
  background: #0a1628;
  position: relative;
}

.footer-wave {
  height: 6px;
  background: linear-gradient(90deg, #1677ff, #36cfc9, #1677ff);
  opacity: 0.6;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  text-align: center;
}

.footer-links {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin-bottom: 8px;
}

.footer-divider {
  margin: 0 10px;
  color: rgba(255, 255, 255, 0.2);
}

.footer-copyright {
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
}
</style>
