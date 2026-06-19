<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>编辑资料</h3>
    </div>
    <div class="card-body">
      <a-form :model="form" layout="vertical">
        <a-form-item label="昵称" name="name"
          :rules="[{ required: true, message: '昵称不能为空' }]"
          :validate-status="nameError ? 'error' : ''"
          :help="nameError">
          <a-input v-model:value="form.name" placeholder="请输入昵称" :maxlength="50" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleSave" :loading="saving" style="border-radius: 6px;">
            保存修改
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { userApi } from '../../api'
import { useStore } from 'vuex'

export default defineComponent({
  name: 'ProfileEdit',
  setup() {
    const store = useStore()
    const form = ref({ name: '' })
    const saving = ref(false)
    const nameError = ref('')

    onMounted(async () => {
      try {
        const res: any = await userApi.profile()
        if (res.content) {
          form.value.name = res.content.name || ''
        }
      } catch {}
    })

    const handleSave = async () => {
      if (!form.value.name.trim()) {
        nameError.value = '昵称不能为空'
        return
      }
      nameError.value = ''
      saving.value = true
      try {
        await userApi.updateProfile({ name: form.value.name.trim() })
        message.success('保存成功')
        // 同步更新 Vuex 中的用户信息
        const user = { ...store.state.user, name: form.value.name.trim() }
        store.commit('setUser', user)
      } catch (e: any) {
        message.error(e?.response?.data?.message || '保存失败')
      } finally {
        saving.value = false
      }
    }

    return { form, saving, nameError, handleSave }
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
