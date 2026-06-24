<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>编辑资料</h3>
    </div>
    <div class="card-body">
      <!-- 头像上传 -->
      <div class="avatar-section">
        <div class="avatar-wrapper">
          <a-avatar :size="80" class="avatar-preview" v-if="form.avatar">
            <img :src="form.avatar" alt="头像" />
          </a-avatar>
          <a-avatar :size="80" class="avatar-preview avatar-placeholder" v-else>
            {{ (form.name || '?').charAt(0) }}
          </a-avatar>
          <div class="avatar-upload-overlay">
            <a-upload
              :customRequest="handleAvatarUpload"
              :show-upload-list="false"
              accept="image/*"
            >
              <a-button size="small" class="avatar-upload-btn">📷 更换头像</a-button>
            </a-upload>
          </div>
        </div>
      </div>

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
import { userApi, fileApi } from '../../api'
import { useStore } from 'vuex'

export default defineComponent({
  name: 'ProfileEdit',
  setup() {
    const store = useStore()
    const form = ref({ name: '', avatar: '' })
    const saving = ref(false)
    const nameError = ref('')

    onMounted(async () => {
      try {
        const res: any = await userApi.profile()
        if (res.content) {
          form.value.name = res.content.name || ''
          form.value.avatar = res.content.avatar || ''
        }
      } catch {}
    })

    const handleAvatarUpload = async (options: any) => {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res: any = await fileApi.upload(formData, 'avatar')
        form.value.avatar = res.content
        message.success('头像上传成功')
      } catch {
        message.error('头像上传失败')
      }
    }

    const handleSave = async () => {
      if (!form.value.name.trim()) {
        nameError.value = '昵称不能为空'
        return
      }
      nameError.value = ''
      saving.value = true
      try {
        const updateData: any = { name: form.value.name.trim() }
        if (form.value.avatar) {
          updateData.avatar = form.value.avatar
        }
        await userApi.updateProfile(updateData)
        message.success('保存成功')
        // 同步更新 Vuex 中的用户信息
        const user = { ...store.state.user, name: form.value.name.trim(), avatar: form.value.avatar }
        store.commit('setUser', user)
      } catch (e: any) {
        message.error(e?.response?.data?.message || '保存失败')
      } finally {
        saving.value = false
      }
    }

    return { form, saving, nameError, handleAvatarUpload, handleSave }
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

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  text-align: center;
}

.avatar-preview {
  border-radius: 50%;
  border: 3px solid #e8ecf0;
  margin-bottom: 8px;
}

.avatar-preview img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: white;
  font-size: 28px;
  font-weight: 600;
  line-height: 80px;
}

.avatar-upload-btn {
  border-radius: 8px;
  font-size: 12px;
}
</style>
