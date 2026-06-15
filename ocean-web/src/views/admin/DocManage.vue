<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <h2>文档管理</h2>
      <div>
        <a-select v-model:value="ebookId" placeholder="选择电子书" style="width: 200px; margin-right: 12px" @change="loadDocs">
          <a-select-option v-for="e in ebooks" :key="e.id" :value="e.id">{{ e.name }}</a-select-option>
        </a-select>
        <a-button type="primary" :disabled="!ebookId" @click="showModal()">新增文档</a-button>
      </div>
    </div>
    <a-table v-if="ebookId" :columns="columns" :data-source="docTree" row-key="id" :pagination="false" default-expand-all-rows>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-popconfirm title="确定删除？子文档将一并删除" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
    <a-empty v-else description="请选择电子书" />
    <a-modal v-model:visible="modalVisible" :title="form.id ? '编辑文档' : '新增文档'" @ok="handleSave" width="800px">
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="form.name" /></a-form-item>
        <a-form-item label="父文档"><a-input-number v-model:value="form.parent" placeholder="0表示顶级" /></a-form-item>
        <a-form-item label="排序"><a-input-number v-model:value="form.sort" /></a-form-item>
        <a-form-item label="内容">
          <a-textarea v-model:value="form.content" :rows="10" />
          <div style="margin-top: 8px">
            <a-space>
              <a-button size="small" @click="aiDocAssist('doc_outline')">AI 生成大纲</a-button>
              <a-button size="small" @click="aiDocAssist('doc_expand')">AI 扩展章节</a-button>
              <a-button size="small" @click="aiDocAssist('doc_supplement')">AI 补充细节</a-button>
              <a-button size="small" @click="aiDocAssist('doc_polish')">AI 润色优化</a-button>
            </a-space>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { docApi, ebookApi, aiApi } from '../../api'
export default defineComponent({
  setup() {
    const ebooks = ref<any[]>([])
    const ebookId = ref<number | undefined>(undefined)
    const docTree = ref<any[]>([])
    const modalVisible = ref(false)
    const form = ref<any>({})
    const columns = [
      { title: '名称', dataIndex: 'name', key: 'name' },
      { title: '阅读数', dataIndex: 'viewCount', key: 'viewCount' },
      { title: '点赞数', dataIndex: 'voteCount', key: 'voteCount' },
      { title: '操作', key: 'action' }
    ]
    const loadEbooks = async () => { const res: any = await ebookApi.list({ page: 1, size: 100 }); ebooks.value = res.content.list }
    const loadDocs = async () => { if (!ebookId.value) return; const res: any = await docApi.list(ebookId.value); docTree.value = res.content }
    const showModal = (record?: any) => { form.value = record ? { ...record, ebookId: ebookId.value } : { ebookId: ebookId.value, parent: 0, sort: 0 }; modalVisible.value = true }
    const handleSave = async () => { await docApi.save(form.value); message.success('保存成功'); modalVisible.value = false; loadDocs() }
    const handleDelete = async (id: number) => { await docApi.delete(id); message.success('删除成功'); loadDocs() }
    const aiDocAssist = async (type: string) => {
      const topic = form.value.name || ''
      const selectedText = form.value.content?.substring(0, 500) || ''
      try {
        const res: any = await aiApi.generate({ type, topic, selectedText })
        form.value.content = res.content.text
        message.success('AI 生成完成')
      } catch (e: any) {
        // 错误已在拦截器中处理
      }
    }
    onMounted(loadEbooks)
    return { ebooks, ebookId, docTree, columns, modalVisible, form, showModal, handleSave, handleDelete, loadDocs, aiDocAssist }
  }
})
</script>