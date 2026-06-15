<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>电子书管理</h2>
      <a-button type="primary" @click="showModal()">新增电子书</a-button>
    </div>
    <a-table :columns="columns" :data-source="ebooks" :pagination="{ total, current: page, pageSize: size, onChange: (p) => { page = p; loadEbooks() } }" row-key="id">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:visible="modalVisible" :title="form.id ? '编辑电子书' : '新增电子书'" @ok="handleSave">
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="一级分类">
          <a-select v-model:value="form.category1Id" @change="form.category2Id = undefined">
            <a-select-option v-for="c in category1List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="二级分类">
          <a-select v-model:value="form.category2Id">
            <a-select-option v-for="c in category2List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述"><a-textarea v-model:value="form.description" /></a-form-item>
        <a-form-item label="封面">
          <a-upload
            :customRequest="handleCoverUpload"
            :show-upload-list="false"
            accept="image/*"
          >
            <a-button size="small">上传封面</a-button>
          </a-upload>
          <div v-if="form.cover" style="margin-top: 8px">
            <img :src="form.cover" style="max-width: 200px; max-height: 100px" />
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ebookApi, categoryApi, fileApi } from '../../api'
export default defineComponent({
  setup() {
    const ebooks = ref<any[]>([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(10)
    const categories = ref<any[]>([])
    const modalVisible = ref(false)
    const form = ref<any>({})
    const columns = [
      { title: '名称', dataIndex: 'name', key: 'name' },
      { title: '文档数', dataIndex: 'docCount', key: 'docCount' },
      { title: '阅读数', dataIndex: 'viewCount', key: 'viewCount' },
      { title: '点赞数', dataIndex: 'voteCount', key: 'voteCount' },
      { title: '操作', key: 'action' }
    ]
    const category1List = computed(() => categories.value.filter(c => c.parent === 0))
    const category2List = computed(() => categories.value.filter(c => c.parent === form.value.category1Id))
    const loadEbooks = async () => { const res: any = await ebookApi.list({ page: page.value, size: size.value }); ebooks.value = res.content.list; total.value = res.content.total }
    const showModal = (record?: any) => { form.value = record ? { ...record } : {}; modalVisible.value = true }
    const handleSave = async () => { await ebookApi.save(form.value); message.success('保存成功'); modalVisible.value = false; loadEbooks() }
    const handleDelete = async (id: number) => { await ebookApi.delete(id); message.success('删除成功'); loadEbooks() }
    const handleCoverUpload = async (options: any) => {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res: any = await fileApi.upload(formData)
        form.value.cover = res.content
        message.success('封面上传成功')
      } catch (e: any) {
        message.error('封面上传失败')
      }
    }
    onMounted(async () => { loadEbooks(); const res: any = await categoryApi.all(); categories.value = res.content })
    return { ebooks, total, page, size, columns, categories, category1List, category2List, modalVisible, form, showModal, handleSave, handleDelete, loadEbooks, handleCoverUpload }
  }
})
</script>