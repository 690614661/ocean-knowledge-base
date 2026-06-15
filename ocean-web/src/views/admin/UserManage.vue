<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>用户管理</h2>
      <a-button type="primary" @click="showModal()">新增用户</a-button>
    </div>
    <a-table :columns="columns" :data-source="users" :pagination="{ total, current: page, pageSize: size, onChange: (p) => { page = p; loadUsers() } }" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-button type="link" @click="showResetModal(record)">重置密码</a-button>
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
    <a-modal v-model:visible="modalVisible" :title="form.id ? '编辑用户' : '新增用户'" @ok="handleSave">
      <a-form :model="form" layout="vertical">
        <a-form-item label="登录名"><a-input v-model:value="form.loginName" :disabled="!!form.id" /></a-form-item>
        <a-form-item label="昵称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item v-if="!form.id" label="密码"><a-input-password v-model:value="form.password" /></a-form-item>
      </a-form>
    </a-modal>
    <a-modal v-model:visible="resetModalVisible" title="重置密码" @ok="handleResetPassword">
      <a-form layout="vertical">
        <a-form-item label="新密码"><a-input-password v-model:value="resetPassword" /></a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { userApi } from '../../api'
export default defineComponent({
  setup() {
    const users = ref<any[]>([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(10)
    const modalVisible = ref(false)
    const form = ref<any>({})
    const resetModalVisible = ref(false)
    const resetUserId = ref<number>(0)
    const resetPassword = ref('')
    const columns = [
      { title: '登录名', dataIndex: 'loginName', key: 'loginName' },
      { title: '昵称', dataIndex: 'name', key: 'name' },
      { title: '操作', key: 'action' }
    ]
    const loadUsers = async () => { const res: any = await userApi.list({ page: page.value, size: size.value }); users.value = res.content.list; total.value = res.content.total }
    const showModal = (record?: any) => { form.value = record ? { ...record } : {}; modalVisible.value = true }
    const handleSave = async () => { await userApi.save(form.value); message.success('保存成功'); modalVisible.value = false; loadUsers() }
    const handleDelete = async (id: number) => { await userApi.delete(id); message.success('删除成功'); loadUsers() }
    const showResetModal = (record: any) => { resetUserId.value = record.id; resetPassword.value = ''; resetModalVisible.value = true }
    const handleResetPassword = async () => { await userApi.resetPassword({ userId: resetUserId.value, password: resetPassword.value }); message.success('密码重置成功'); resetModalVisible.value = false }
    onMounted(loadUsers)
    return { users, total, page, size, columns, modalVisible, form, resetModalVisible, resetPassword, showModal, handleSave, handleDelete, showResetModal, handleResetPassword, loadUsers }
  }
})
</script>