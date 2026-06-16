<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-container">
      <div class="login-brand">
        <div class="brand-icon">🌊</div>
        <div class="brand-title">海洋生物知识库</div>
        <div class="brand-sub">Guangdong Ocean University</div>
        <div class="brand-desc">探索海洋奥秘 · 保护蓝色星球</div>
      </div>
      <div class="login-card">
        <div class="login-header">
          <h2>登录</h2>
          <p>请输入账号密码</p>
        </div>
        <div class="error-box" v-if="errorMsg">{{ errorMsg }}</div>
        <div class="success-box" v-if="successMsg">{{ successMsg }}</div>
        <div class="form-group">
          <input v-model="form.loginName" placeholder="登录名" class="input-field" />
        </div>
        <div class="form-group">
          <input v-model="form.password" type="password" placeholder="密码" class="input-field" />
        </div>
        <button @click="doLogin" :disabled="loading" class="login-btn">{{ loading ? '登录中...' : '登 录' }}</button>
        <div class="login-footer">默认账号 admin / admin123</div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import axios from 'axios'
export default defineComponent({
  name: 'LoginPage',
  setup() {
    const form = ref({ loginName: '', password: '' })
    const loading = ref(false)
    const errorMsg = ref('')
    const successMsg = ref('')

    const doLogin = async () => {
      loading.value = true
      errorMsg.value = ''
      successMsg.value = ''
      try {
        const res = await axios.post('/api/user/login', form.value, { timeout: 10000 })
        const data = res.data
        if (data.success) {
          successMsg.value = '登录成功！'
          sessionStorage.setItem('user', JSON.stringify(data.content))
          window.location.href = '/'
        } else {
          errorMsg.value = data.message || '登录失败'
        }
      } catch (e: any) {
        if (e.response) {
          const msg = e.response.data?.message || e.message
          errorMsg.value = '错误 ' + e.response.status + ': ' + msg
        } else if (e.request) {
          errorMsg.value = '网络错误：无法连接到服务器'
        } else {
          errorMsg.value = e.message
        }
      }
      loading.value = false
    }
    return { form, loading, errorMsg, successMsg, doLogin }
  }
})
</script>

<style scoped>
.login-page { min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#0a1628,#1a3a5c,#0d4f6b,#1677ff);position:relative;overflow:hidden; }
.login-container { position:relative;z-index:2;display:flex;gap:48px;align-items:center;max-width:880px;width:100%;padding:24px; }
.login-brand { flex:1;text-align:center;padding:32px; }
.brand-icon { font-size:72px;margin-bottom:16px;animation:float 4s ease-in-out infinite; }
@keyframes float { 0%,100%{transform:translateY(0)} 50%{transform:translateY(-14px)} }
.brand-title { font-size:32px;font-weight:800;color:#fff;margin:0 0 4px; }
.brand-sub { font-size:12px;color:rgba(255,255,255,0.5);letter-spacing:2px;margin:0 0 12px;text-transform:uppercase; }
.brand-desc { font-size:14px;color:rgba(255,255,255,0.7);margin:0; }
.login-card { width:360px;background:rgba(255,255,255,0.95);border-radius:16px;padding:32px;box-shadow:0 8px 32px rgba(0,0,0,0.2); }
.login-header { margin-bottom:24px;text-align:center; }
.login-header h2 { font-size:22px;font-weight:700;color:#1a1a2e;margin:0 0 4px; }
.login-header p { font-size:14px;color:#5a6a7a;margin:0; }
.error-box { background:#fff1f0;border:1px solid #ffccc7;border-radius:8px;padding:8px 12px;margin-bottom:12px;color:#cf1322;font-size:13px;white-space:pre-wrap;word-break:break-all; }
.success-box { background:#f6ffed;border:1px solid #b7eb8f;border-radius:8px;padding:8px 12px;margin-bottom:12px;color:#389e0d;font-size:13px; }
.form-group { margin-bottom:16px; }
.input-field { width:100%;padding:10px 14px;border-radius:8px;border:1px solid #d9d9d9;font-size:14px;outline:none;box-sizing:border-box;transition:border-color 0.3s; }
.input-field:focus { border-color:#1677ff;box-shadow:0 0 0 2px rgba(22,119,255,0.1); }
.login-btn { width:100%;padding:10px;border-radius:8px;border:none;background:linear-gradient(135deg,#1677ff,#4096ff);color:#fff;font-size:16px;font-weight:600;cursor:pointer;transition:all 0.3s;margin-bottom:12px; }
.login-btn:hover { opacity:0.9;transform:translateY(-1px); }
.login-btn:disabled { opacity:0.6;cursor:not-allowed; }
.login-footer { text-align:center;font-size:12px;color:#999; }
</style>