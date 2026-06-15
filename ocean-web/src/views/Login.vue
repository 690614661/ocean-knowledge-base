<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-bubble bubble-1"></div>
      <div class="bg-bubble bubble-2"></div>
      <div class="bg-bubble bubble-3"></div>
      <div class="bg-bubble bubble-4"></div>
    </div>
    <div class="login-container">
      <div class="login-brand">
        <div class="brand-icon">🌊</div>
        <div class="brand-title">海洋生物知识库</div>
        <div class="brand-desc">探索海洋奥秘，保护蓝色星球</div>
      </div>
      <div class="login-card">
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号</p>
        </div>
        <a-form :model="form" @finish="onLogin" layout="vertical" size="large">
          <a-form-item
            name="loginName"
            :rules="[{ required: true, message: '请输入登录名' }]"
          >
            <a-input
              v-model:value="form.loginName"
              placeholder="登录名"
              prefix="👤"
              class="login-input"
            />
          </a-form-item>
          <a-form-item
            name="password"
            :rules="[{ required: true, message: '请输入密码' }]"
          >
            <a-input-password
              v-model:value="form.password"
              placeholder="密码"
              prefix="🔒"
              class="login-input"
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              :loading="loading"
              block
              class="login-submit-btn"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </a-button>
          </a-form-item>
        </a-form>
        <div class="login-footer">
          <span class="login-hint">测试账号：admin / admin123</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'
import { userApi } from '../api'

export default defineComponent({
  name: 'Login',
  setup() {
    const router = useRouter()
    const store = useStore()
    const loading = ref(false)
    const form = ref({ loginName: '', password: '' })

    const onLogin = async () => {
      loading.value = true
      try {
        const res: any = await userApi.login(form.value)
        if (res.success) {
          store.commit('setUser', res.content)
          message.success('🎉 登录成功')
          router.push('/')
        }
      } finally {
        loading.value = false
      }
    }

    return { form, loading, onLogin }
  }
})
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 134px);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e8f4fd 0%, #d6eaf8 30%, #aed6f1 60%, #e8f4fd 100%);
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.bg-bubble {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.bubble-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #1677ff, transparent);
  top: -100px;
  left: -50px;
  animation: bubbleFloat 8s ease-in-out infinite;
}

.bubble-2 {
  width: 250px;
  height: 250px;
  background: radial-gradient(circle, #36cfc9, transparent);
  bottom: 10%;
  right: 10%;
  animation: bubbleFloat 6s ease-in-out infinite reverse;
}

.bubble-3 {
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, #1677ff, transparent);
  top: 40%;
  right: 25%;
  animation: bubbleFloat 10s ease-in-out infinite;
}

.bubble-4 {
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, #69b1ff, transparent);
  bottom: 30%;
  left: 20%;
  animation: bubbleFloat 7s ease-in-out infinite reverse;
}

@keyframes bubbleFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, -30px) scale(1.05); }
  50% { transform: translate(-10px, -50px) scale(0.95); }
  75% { transform: translate(15px, -20px) scale(1.02); }
}

.login-container {
  position: relative;
  z-index: 2;
  display: flex;
  gap: 48px;
  align-items: center;
  max-width: 900px;
  width: 100%;
  padding: 24px;
}

.login-brand {
  flex: 1;
  text-align: center;
  padding: 48px;
}

.brand-icon {
  font-size: 80px;
  margin-bottom: 16px;
  animation: logoFloat 3s ease-in-out infinite;
}

@keyframes logoFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 8px;
}

.brand-desc {
  font-size: 15px;
  color: #5a6a7a;
}

.login-card {
  width: 380px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  box-shadow:
    0 8px 32px rgba(22, 119, 255, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.5);
}

.login-header {
  margin-bottom: 28px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.login-header p {
  font-size: 14px;
  color: #5a6a7a;
  margin: 0;
}

.login-input {
  height: 48px;
  border-radius: 12px;
}

.login-input :deep(.ant-input) {
  padding-left: 8px;
}

.login-submit-btn {
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.3);
  transition: all 0.3s ease;
}

.login-submit-btn:hover {
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.45);
  transform: translateY(-1px);
}

.login-footer {
  margin-top: 20px;
  text-align: center;
}

.login-hint {
  font-size: 12px;
  color: #999;
}
</style>