<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-bubble bubble-1"></div>
      <div class="bg-bubble bubble-2"></div>
      <div class="bg-bubble bubble-3"></div>
      <div class="bg-bubble bubble-4"></div>
      <div class="bg-bubble bubble-5"></div>
      <!-- 学校水印 -->
      <div class="school-watermark">广东海洋大学</div>
    </div>
    <div class="login-container">
      <div class="login-brand">
        <div class="brand-icon">🌊</div>
        <div class="brand-title">海洋生物知识库</div>
        <div class="brand-subtitle">Guangdong Ocean University</div>
        <div class="brand-desc">探索海洋奥秘，保护蓝色星球</div>
        <div class="brand-decoration">
          <div class="deco-line"></div>
          <span class="deco-fish">🐟</span>
          <div class="deco-line"></div>
        </div>
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
          <div class="login-hint">实训项目 · 广东海洋大学</div>
          <div class="login-credits">@2026 Ocean Knowledge Base</div>
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
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0a1628 0%, #1a3a5c 30%, #0d4f6b 60%, #1677ff 100%);
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
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #69b1ff, transparent);
  top: -150px;
  left: -50px;
  animation: bubbleFloat 8s ease-in-out infinite;
}

.bubble-2 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #36cfc9, transparent);
  bottom: 5%;
  right: 5%;
  animation: bubbleFloat 6s ease-in-out infinite reverse;
}

.bubble-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #1677ff, transparent);
  top: 30%;
  right: 20%;
  animation: bubbleFloat 10s ease-in-out infinite;
}

.bubble-4 {
  width: 120px;
  height: 120px;
  background: radial-gradient(circle, #69b1ff, transparent);
  bottom: 25%;
  left: 15%;
  animation: bubbleFloat 7s ease-in-out infinite reverse;
}

.bubble-5 {
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, #36cfc9, transparent);
  top: 60%;
  left: 35%;
  animation: bubbleFloat 9s ease-in-out infinite;
}

@keyframes bubbleFloat {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(20px, -30px) scale(1.05); }
  50% { transform: translate(-10px, -50px) scale(0.95); }
  75% { transform: translate(15px, -20px) scale(1.02); }
}

.school-watermark {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.15);
  letter-spacing: 6px;
  font-weight: 300;
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
  padding: 32px;
}

.brand-icon {
  font-size: 80px;
  margin-bottom: 16px;
  animation: brandFloat 3s ease-in-out infinite;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2));
}

@keyframes brandFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-12px); }
}

.brand-title {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #69b1ff, #36cfc9);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 4px;
}

.brand-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 2px;
  margin-bottom: 12px;
  text-transform: uppercase;
}

.brand-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 20px;
}

.brand-decoration {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.deco-line {
  width: 60px;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
}

.deco-fish {
  font-size: 20px;
  animation: fishSwim 2s ease-in-out infinite;
}

@keyframes fishSwim {
  0%, 100% { transform: translateX(0); }
  50% { transform: translateX(6px); }
}

.login-card {
  width: 380px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 40px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.1);
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
  margin-top: 24px;
  text-align: center;
}

.login-hint {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.login-credits {
  font-size: 11px;
  color: #ccc;
}
</style>
