<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="profile-layout">
      <div class="profile-sidebar">
        <div class="profile-user">
          <a-avatar :size="64" class="profile-avatar" v-if="user.avatar" :src="user.avatar" />
          <a-avatar :size="64" class="profile-avatar" v-else>{{ user.name?.charAt(0) }}</a-avatar>
          <div class="profile-username">{{ user.name }}</div>
          <div class="profile-loginname">{{ user.loginName }}</div>
          <div class="profile-role-tag">
            <a-tag :color="user.role === 'admin' ? 'gold' : 'blue'">
              {{ user.role === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </div>
        </div>
        <a-menu
          :selected-keys="[activeKey]"
          mode="vertical"
          class="profile-menu"
          @click="onMenuClick"
        >
          <a-menu-item key="info">
            <UserOutlined /> 个人信息
          </a-menu-item>
          <a-menu-item key="edit">
            <EditOutlined /> 编辑资料
          </a-menu-item>
          <a-menu-item key="password">
            <LockOutlined /> 修改密码
          </a-menu-item>
          <a-menu-item key="favorites">
            <StarOutlined /> 我的收藏
          </a-menu-item>
          <a-menu-item key="history">
            <HistoryOutlined /> 浏览历史
          </a-menu-item>
        </a-menu>
      </div>
      <div class="profile-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { UserOutlined, EditOutlined, LockOutlined, StarOutlined, HistoryOutlined } from '@ant-design/icons-vue'

export default defineComponent({
  name: 'ProfileLayout',
  components: { UserOutlined, EditOutlined, LockOutlined, StarOutlined, HistoryOutlined },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    const user = computed(() => store.state.user)

    const activeKey = computed(() => {
      const path = route.path
      if (path === '/profile' || path === '/profile/info') return 'info'
      if (path === '/profile/edit') return 'edit'
      if (path === '/profile/password') return 'password'
      if (path === '/profile/favorites') return 'favorites'
      if (path === '/profile/history') return 'history'
      return 'info'
    })

    const onMenuClick = ({ key }: { key: string }) => {
      router.push(`/profile/${key === 'info' ? '' : key}`)
    }

    return { user, activeKey, onMenuClick }
  }
})
</script>

<style scoped>
.profile-layout {
  display: flex;
  gap: 24px;
  padding: 24px 0;
  min-height: calc(100vh - 200px);
}

.profile-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  align-self: flex-start;
  position: sticky;
  top: 88px;
}

.profile-user {
  text-align: center;
  padding: 32px 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.profile-avatar {
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: #fff;
  font-weight: 600;
  font-size: 24px;
  margin-bottom: 12px;
}

.profile-username {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.profile-loginname {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}

.profile-role-tag {
  margin-top: 4px;
}

.profile-menu {
  border-right: none !important;
}

.profile-menu :deep(.ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 4px 0 !important;
}

.profile-content {
  flex: 1;
  min-width: 0;
}



</style>
