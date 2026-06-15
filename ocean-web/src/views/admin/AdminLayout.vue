<template>
  <div class="page-container" style="padding-bottom: 32px">
    <div class="admin-layout">
      <!-- 侧边栏 -->
      <div class="admin-sider">
        <div class="admin-sider-header">
          <span class="admin-sider-title">⚙️ 管理后台</span>
        </div>
        <a-menu
          mode="inline"
          :selectedKeys="[selectedKey]"
          class="admin-menu"
          @click="onMenuClick"
        >
          <a-menu-item key="dashboard">
            <dashboard-outlined /> 仪表盘
          </a-menu-item>
          <a-menu-item key="ebook">
            <book-outlined /> 电子书管理
          </a-menu-item>
          <a-menu-item key="category">
            <folder-outlined /> 分类管理
          </a-menu-item>
          <a-menu-item key="doc">
            <file-text-outlined /> 文档管理
          </a-menu-item>
          <a-menu-item key="user">
            <team-outlined /> 用户管理
          </a-menu-item>
        </a-menu>
        <div class="admin-sider-footer">
          <a-button type="link" @click="$router.push('/')">← 返回首页</a-button>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DashboardOutlined, BookOutlined, FolderOutlined,
  FileTextOutlined, TeamOutlined
} from '@ant-design/icons-vue'

export default defineComponent({
  name: 'AdminLayout',
  components: {
    DashboardOutlined, BookOutlined, FolderOutlined,
    FileTextOutlined, TeamOutlined
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const selectedKey = ref('dashboard')

    // 跟踪路由变化
    watch(() => route.path, (path) => {
      if (path === '/admin') selectedKey.value = 'dashboard'
      else if (path.includes('ebook')) selectedKey.value = 'ebook'
      else if (path.includes('category')) selectedKey.value = 'category'
      else if (path.includes('doc')) selectedKey.value = 'doc'
      else if (path.includes('user')) selectedKey.value = 'user'
    }, { immediate: true })

    const onMenuClick = ({ key }: { key: string }) => {
      selectedKey.value = key
      router.push(key === 'dashboard' ? '/admin' : `/admin/${key}`)
    }

    return { selectedKey, onMenuClick }
  }
})
</script>

<style scoped>
.admin-layout {
  display: flex;
  gap: 24px;
  min-height: calc(100vh - 200px);
  padding: 24px 0;
}

.admin-sider {
  width: 220px;
  flex-shrink: 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: sticky;
  top: 88px;
  height: fit-content;
}

.admin-sider-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.admin-sider-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a2e;
}

.admin-menu {
  border: none !important;
  padding: 8px;
}

.admin-menu :deep(.ant-menu-item) {
  border-radius: 10px !important;
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  font-size: 14px;
  transition: all 0.2s;
}

.admin-menu :deep(.ant-menu-item:hover) {
  background: #e6f4ff !important;
  color: #1677ff !important;
}

.admin-menu :deep(.ant-menu-item-selected) {
  background: linear-gradient(135deg, #e6f4ff, #f0f9ff) !important;
  color: #1677ff !important;
  font-weight: 600;
}

.admin-sider-footer {
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.admin-content {
  flex: 1;
  min-width: 0;
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
</style>