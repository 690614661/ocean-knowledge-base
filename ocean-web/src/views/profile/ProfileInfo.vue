<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>个人信息</h3>
    </div>
    <div class="card-body">
      <a-skeleton :loading="loading" active>
        <div class="avatar-info" v-if="profile.avatar" style="text-align:center;margin-bottom:20px">
          <a-avatar :size="72" :src="profile.avatar" />
        </div>
        <div class="info-item">
          <span class="info-label">登录名</span>
          <span class="info-value">{{ profile.loginName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">昵称</span>
          <span class="info-value">{{ profile.name }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">邮箱</span>
          <span class="info-value">{{ profile.email || '未绑定' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">角色</span>
          <span class="info-value">
            <a-tag :color="profile.role === 'admin' ? 'gold' : 'blue'">
              {{ profile.role === 'admin' ? '管理员' : '普通用户' }}
            </a-tag>
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">注册时间</span>
          <span class="info-value">{{ profile.createTime?.slice(0, 19) }}</span>
        </div>
      </a-skeleton>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { userApi } from '../../api'

export default defineComponent({
  name: 'ProfileInfo',
  setup() {
    const profile = ref<any>({})
    const loading = ref(true)

    onMounted(async () => {
      try {
        const res: any = await userApi.profile()
        profile.value = res.content || {}
      } catch {}
      loading.value = false
    })

    return { profile, loading }
  }
})
</script>

<style scoped>
.profile-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.card-body {
  padding: 24px;
}

.info-item {
  display: flex;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  width: 100px;
  font-size: 14px;
  color: #999;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #1a1a2e;
  font-weight: 500;
}

</style>
