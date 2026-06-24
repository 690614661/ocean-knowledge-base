<template>
  <div id="app">
    <router-view v-if="isLoginPage" />
    <a-layout v-else style="min-height: 100vh">
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
              <!-- 通知铃铛 -->
              <a-badge :count="unreadCount" :overflow-count="99" class="notification-badge">
                <a-button class="bell-btn" shape="circle" @click="showNotificationDrawer">
                  <bell-outlined />
                </a-button>
              </a-badge>
              <!-- 通知抽屉 -->
              <a-drawer
                title="🔔 消息通知"
                placement="right"
                :open="notifDrawerOpen"
                @close="notifDrawerOpen = false"
                width="360"
              >
                <div class="notif-header-actions">
                  <a-button size="small" @click="markAllRead" v-if="unreadCount > 0">全部已读</a-button>
                </div>
                <a-list :data-source="notifList" :loading="notifLoading">
                  <template #renderItem="{ item }">
                    <a-list-item class="notif-item" :class="{ 'notif-unread': !item.isRead }">
                      <a-list-item-meta>
                        <template #title>
                          <span v-html="item.title"></span>
                        </template>
                        <template #description>
                          <span class="notif-time">{{ item.createTime?.slice(0, 16) }}</span>
                        </template>
                      </a-list-item-meta>
                      <template #actions>
                        <a-button v-if="!item.isRead" type="link" size="small" @click="markRead(item.id)">标已读</a-button>
                      </template>
                    </a-list-item>
                  </template>
                  <template v-if="notifList.length === 0">
                    <a-empty description="暂无通知" />
                  </template>
                </a-list>
              </a-drawer>
              <a-dropdown>
                <div class="user-info" style="cursor: pointer">
                  <a-avatar :size="30" class="user-avatar" v-if="user.avatar" :src="user.avatar" />
                  <a-avatar :size="30" class="user-avatar" v-else>{{ user.name?.charAt(0) }}</a-avatar>
                  <span class="user-name">{{ user.name }}</span>
                  <down-outlined style="color: rgba(255,255,255,0.5); font-size: 10px; margin-left: 2px;" />
                </div>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="$router.push('/profile')">
                      <user-outlined /> 个人中心
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item @click="showLogoutConfirm">
                      <logout-outlined /> 退出登录
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
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
import { defineComponent, computed, ref, onMounted, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import { notificationApi } from './api'
import wsClient from './utils/WebSocketClient'
import { Modal } from 'ant-design-vue'
import { HomeOutlined, ReadOutlined, RobotOutlined, SettingOutlined, DownOutlined, UserOutlined, LogoutOutlined, BellOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'App',
  components: {
    HomeOutlined, ReadOutlined, RobotOutlined, SettingOutlined,
    DownOutlined, UserOutlined, LogoutOutlined, BellOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    const user = computed(() => store.state.user)
    const isLoginPage = computed(() => route.path === '/login')
    const currentPath = computed(() => route.path)

    // 通知
    const unreadCount = ref(0)
    const notifDrawerOpen = ref(false)
    const notifList = ref<any[]>([])
    const notifLoading = ref(false)

    const loadUnreadCount = async () => {
      try {
        const res: any = await notificationApi.unreadCount()
        unreadCount.value = res.content?.count || 0
      } catch {}
    }

    const loadNotifList = async () => {
      notifLoading.value = true
      try {
        const res: any = await notificationApi.list({ page: 1, size: 20 })
        notifList.value = res.content?.list || []
      } catch {}
      notifLoading.value = false
    }

    const showNotificationDrawer = () => {
      notifDrawerOpen.value = true
      loadNotifList()
    }

    const markRead = async (id: number) => {
      await notificationApi.markRead(id)
      loadNotifList()
      loadUnreadCount()
    }

    const markAllRead = async () => {
      await notificationApi.markAllRead()
      loadNotifList()
      loadUnreadCount()
    }

    // 连接WebSocket
    const connectWs = () => {
      const u = store.state.user
      if (!u.token) return
      wsClient.connect(u.token)
      wsClient.on('unread_count', (data: any) => {
        unreadCount.value = data?.count || 0
      })
      wsClient.on('notification', (data: any) => {
        if (notifDrawerOpen.value) loadNotifList()
      })
    }

    onMounted(() => {
      if (user.value.token) {
        loadUnreadCount()
        connectWs()
      }
    })

    onUnmounted(() => {
      wsClient.disconnect()
    })

    const toLogin = () => router.push('/login')

    const showLogoutConfirm = () => {
      Modal.confirm({
        title: '确认退出登录？',
        content: '退出后需要重新登录',
        okText: '确认退出',
        cancelText: '取消',
        onOk: () => logout()
      })
    }

    const logout = async () => {
      try {
        await axios.get('/api/user/logout', {
          headers: { token: store.state.user.token }
        })
      } catch {}
      finally {
        wsClient.disconnect()
        store.commit('setUser', {})
        sessionStorage.removeItem('user')
        router.push('/')
      }
    }

    return { user, logout, showLogoutConfirm, isLoginPage, currentPath, toLogin,
      unreadCount, notifDrawerOpen, notifList, notifLoading,
      showNotificationDrawer, markRead, markAllRead }
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

.logo-icon { font-size: 22px; }

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

.nav-item:hover { color: #fff; border-bottom-color: #1677ff; }
.nav-item.active { color: #fff; border-bottom-color: #1677ff; font-weight: 600; }

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
}

.school-text {
  background: linear-gradient(90deg, #69b1ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.notification-badge { line-height: 1; }

.bell-btn {
  background: rgba(255,255,255,0.1);
  border: 1px solid rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.75);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.bell-btn:hover { color: #fff !important; border-color: rgba(255,255,255,0.4) !important; }

.notif-header-actions { text-align: right; margin-bottom: 12px; }
.notif-item { padding: 12px 0 !important; }
.notif-unread { background: #f0f5ff; border-radius: 8px; padding: 12px 8px !important; }
.notif-time { font-size: 12px; color: #bbb; }

.user-info { display: flex; align-items: center; gap: 6px; }
.user-avatar { background: linear-gradient(135deg, #1677ff, #36cfc9); color: #fff; font-weight: 600; font-size: 12px; }
.user-name { color: rgba(255, 255, 255, 0.85); font-size: 13px; }

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

.app-content { min-height: calc(100vh - 134px); background: linear-gradient(180deg, #e8f4fd 0%, #f0f9ff 50%, #f5faff 100%); }

.app-footer { padding: 0; background: #0a1628; position: relative; }
.footer-wave { height: 6px; background: linear-gradient(90deg, #1677ff, #36cfc9, #1677ff); opacity: 0.6; }
.footer-content { max-width: 1200px; margin: 0 auto; padding: 20px; text-align: center; }
.footer-links { color: rgba(255, 255, 255, 0.5); font-size: 13px; margin-bottom: 8px; }
.footer-divider { margin: 0 10px; color: rgba(255, 255, 255, 0.2); }
.footer-copyright { color: rgba(255, 255, 255, 0.3); font-size: 12px; }
</style>
