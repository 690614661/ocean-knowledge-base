<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>修改密码</h3>
    </div>
    <div class="card-body">
      <a-form :model="form" layout="vertical">
        <a-form-item label="旧密码" name="oldPassword"
          :rules="[{ required: true, message: '请输入旧密码' }]">
          <a-input-password v-model:value="form.oldPassword" placeholder="请输入旧密码" />
        </a-form-item>
        <a-form-item label="新密码" name="newPassword"
          :rules="[{ required: true, message: '请输入新密码' }]"
          :validate-status="passwordError ? 'error' : ''"
          :help="passwordError">
          <a-input-password v-model:value="form.newPassword" placeholder="6-32位，包含数字和字母" />
        </a-form-item>
        <a-form-item label="确认新密码" name="confirmPassword"
          :rules="[
            { required: true, message: '请确认新密码' },
            { validator: (rule: any, value: string) => value === form.newPassword ? Promise.resolve() : Promise.reject('两次密码不一致') }
          ]">
          <a-input-password v-model:value="form.confirmPassword" placeholder="再次输入新密码" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleChange" :loading="saving" style="border-radius: 6px;">
            修改密码
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref } from 'vue'
import { message } from 'ant-design-vue'
import { userApi } from '../../api'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'

export default defineComponent({
  name: 'ChangePassword',
  setup() {
    const router = useRouter()
    const store = useStore()
    const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
    const saving = ref(false)
    const passwordError = ref('')

    const handleChange = async () => {
      const pwdRegex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,32}$/
      if (!pwdRegex.test(form.value.newPassword)) {
        passwordError.value = '密码必须6-32位，包含数字和字母'
        return
      }
      if (form.value.newPassword !== form.value.confirmPassword) {
        passwordError.value = '两次密码不一致'
        return
      }
      passwordError.value = ''
      saving.value = true
      try {
        await userApi.changePassword({
          oldPassword: form.value.oldPassword,
          newPassword: form.value.newPassword
        })
        message.success('密码修改成功，请重新登录')
        store.commit('setUser', {})
        sessionStorage.removeItem('user')
        router.push('/login')
      } catch (e: any) {
        message.error(e?.response?.data?.message || '修改失败')
      } finally {
        saving.value = false
      }
    }

    return { form, saving, passwordError, handleChange }
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
  max-width: 400px;
}

</style>
