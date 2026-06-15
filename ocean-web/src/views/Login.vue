<template>
  <div style="display: flex; justify-content: center; align-items: center; min-height: 400px">
    <a-card title="用户登录" style="width: 400px">
      <a-form :model="form" @finish="onLogin">
        <a-form-item name="loginName" :rules="[{ required: true, message: '请输入登录名' }]">
          <a-input v-model:value="form.loginName" placeholder="登录名" size="large" />
        </a-form-item>
        <a-form-item name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" placeholder="密码" size="large" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" :loading="loading" block>登录</a-button>
        </a-form-item>
      </a-form>
    </a-card>
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
          message.success('登录成功')
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