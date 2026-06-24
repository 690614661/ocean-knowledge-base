<template>
  <div>
    <div class="page-header">
      <h2>👤 用户管理</h2>
      <div class="header-actions">
        <a-button danger class="batch-delete-btn" :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button type="primary" class="add-btn" @click="showModal()">+ 新增用户</a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="users"
      :pagination="{
        total, current: page, pageSize: size,
        showTotal: (t) => `共 ${t} 条`,
        onChange: (p) => { page = p; loadUsers() }
      }"
      row-key="id"
      :row-selection="{
        selectedRowKeys: selectedRowKeys,
        onChange: (keys: any[]) => { selectedRowKeys = keys }
      }"
      class="ocean-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-button type="link" @click="showResetModal(record)">重置密码</a-button>
          <a-popconfirm title="确定删除该用户？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <!-- 新增/编辑用户 -->
    <a-modal
      :open="modalVisible"
      @update:open="(v) => modalVisible = v"
      :title="form.id ? '编辑用户' : '新增用户'"
      @ok="handleSave"
      class="ocean-modal"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="登录名" :rules="[{ required: true, message: '请输入登录名' }]">
          <a-input v-model:value="form.loginName" placeholder="登录名" :disabled="!!form.id" />
        </a-form-item>
        <a-form-item label="昵称" :rules="[{ required: true, message: '请输入昵称' }]">
          <a-input v-model:value="form.name" placeholder="昵称" />
        </a-form-item>
        <a-form-item v-if="!form.id" label="密码" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" placeholder="6-32位，包含数字和字母" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 重置密码 -->
    <a-modal
      :open="resetModalVisible"
      @update:open="(v) => resetModalVisible = v"
      title="🔑 重置密码"
      @ok="handleResetPassword"
      class="ocean-modal"
    >
      <a-form layout="vertical">
        <a-form-item label="新密码" :rules="[{ required: true, message: '请输入新密码' }]">
          <a-input-password v-model:value="resetPasswd" placeholder="6-32位，包含数字和字母" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { userApi } from '../../api'
import anime from 'animejs/lib/anime.es.js'

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
    const resetPasswd = ref('')
    const selectedRowKeys = ref<number[]>([])
    const columns = [
      { title: '登录名', dataIndex: 'loginName', key: 'loginName' },
      { title: '昵称', dataIndex: 'name', key: 'name' },
      { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
      { title: '操作', key: 'action', width: 200 }
    ]

    const loadUsers = async () => {
      const res: any = await userApi.list({ page: page.value, size: size.value })
      users.value = res.content.list
      total.value = res.content.total
      nextTick(() => {
        const rows = document.querySelectorAll('.ant-table-row')
        if (rows.length) {
          anime.set(rows, { opacity: 0, translateY: 12 })
          anime({
            targets: rows,
            opacity: [0, 1],
            translateY: [12, 0],
            duration: 400,
            delay: anime.stagger(30),
            easing: 'easeOutCubic'
          })
        }
      })
    }

    const showModal = (record?: any) => {
      form.value = record ? { ...record } : {}
      modalVisible.value = true
    }

    const handleSave = async () => {
      await userApi.save(form.value)
      message.success('保存成功')
      modalVisible.value = false
      loadUsers()
    }

    const handleDelete = async (id: number) => {
      await userApi.delete(id)
      message.success('删除成功')
      loadUsers()
    }

    const handleBatchDelete = async () => {
      if (selectedRowKeys.value.length === 0) return
      try {
        await userApi.deleteBatch(selectedRowKeys.value)
        message.success(`批量删除成功，共删除 ${selectedRowKeys.value.length} 条`)
        selectedRowKeys.value = []
        loadUsers()
      } catch {
        message.error('批量删除失败')
      }
    }

    const showResetModal = (record: any) => {
      resetUserId.value = record.id
      resetPasswd.value = ''
      resetModalVisible.value = true
    }

    const handleResetPassword = async () => {
      await userApi.resetPassword({ userId: resetUserId.value, password: resetPasswd.value })
      message.success('密码重置成功')
      resetModalVisible.value = false
    }

    onMounted(loadUsers)

    return {
      users, total, page, size, columns, modalVisible, form, selectedRowKeys,
      resetModalVisible, resetPasswd,
      showModal, handleSave, handleDelete, handleBatchDelete, showResetModal, handleResetPassword, loadUsers
    }
  }
})
</script>

<style scoped>
.add-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
}
</style>