<template>
  <div>
    <div class="page-header">
      <h2>📄 文档管理</h2>
      <div class="header-actions">
        <a-select v-model:value="ebookId" placeholder="选择电子书" style="width: 200px; margin-right: 12px" @change="loadDocs">
          <a-select-option v-for="e in ebooks" :key="e.id" :value="e.id">{{ e.name }}</a-select-option>
        </a-select>
        <a-button danger class="batch-delete-btn" :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
          批量删除 ({{ selectedRowKeys.length }})
        </a-button>
        <a-button type="primary" class="add-btn" :disabled="!ebookId" @click="showModal()">+ 新增文档</a-button>
      </div>
    </div>

    <a-empty v-if="!ebookId" description="请先选择电子书" />
    <a-table
      v-else
      :columns="columns"
      :data-source="docTree"
      row-key="id"
      :pagination="false"
      :row-selection="{
        selectedRowKeys: selectedRowKeys,
        onChange: (keys: any[]) => { selectedRowKeys = keys }
      }"
      default-expand-all-rows
      class="ocean-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-popconfirm title="子文档将一并删除，确定？" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal
            :open="modalVisible"
      @update:open="(v) => modalVisible = v"
      :title="form.id ? '编辑文档' : '新增文档'"
      @ok="handleSave"
      width="800px"
      class="ocean-modal"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="名称" :rules="[{ required: true, message: '请输入文档名称' }]">
          <a-input v-model:value="form.name" placeholder="文档名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="父文档">
              <a-input-number v-model:value="form.parent" placeholder="0 表示顶级" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序">
              <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="内容">
          <a-textarea v-model:value="form.content" :rows="12" />
          <div class="ai-assist" style="margin-top: 12px">
            <span class="ai-assist-label">🤖 AI 辅助：</span>
            <a-space>
              <a-button size="small" class="ai-assist-btn" :loading="assisting === 'doc_outline'" :disabled="!!assisting" @click="aiDocAssist('doc_outline')">生成大纲</a-button>
              <a-button size="small" class="ai-assist-btn" :loading="assisting === 'doc_expand'" :disabled="!!assisting" @click="aiDocAssist('doc_expand')">扩展章节</a-button>
              <a-button size="small" class="ai-assist-btn" :loading="assisting === 'doc_supplement'" :disabled="!!assisting" @click="aiDocAssist('doc_supplement')">补充细节</a-button>
              <a-button size="small" class="ai-assist-btn" :loading="assisting === 'doc_polish'" :disabled="!!assisting" @click="aiDocAssist('doc_polish')">润色优化</a-button>
            </a-space>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { docApi, ebookApi, aiApi } from '../../api'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  setup() {
    const ebooks = ref<any[]>([])
    const ebookId = ref<number | undefined>(undefined)
    const docTree = ref<any[]>([])
    const modalVisible = ref(false)
    const form = ref<any>({})
    const assisting = ref<string | null>(null)
    const selectedRowKeys = ref<number[]>([])
    const columns = [
      { title: '名称', dataIndex: 'name', key: 'name' },
      { title: '阅读数', dataIndex: 'viewCount', key: 'viewCount', width: 80 },
      { title: '点赞数', dataIndex: 'voteCount', key: 'voteCount', width: 80 },
      { title: '操作', key: 'action', width: 140 }
    ]

    const loadEbooks = async () => {
      const res: any = await ebookApi.list({ page: 1, size: 100 })
      ebooks.value = res.content.list
    }

    const loadDocs = async () => {
      if (!ebookId.value) return
      const res: any = await docApi.list(ebookId.value)
      docTree.value = res.content
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
      form.value = record ? { ...record, ebookId: ebookId.value } : { ebookId: ebookId.value, parent: 0, sort: 0 }
      modalVisible.value = true
    }

    const handleSave = async () => {
      await docApi.save(form.value)
      message.success('保存成功')
      modalVisible.value = false
      loadDocs()
    }

    const handleDelete = async (id: number) => {
      await docApi.delete(id)
      message.success('删除成功')
      loadDocs()
    }

    const handleBatchDelete = async () => {
      if (selectedRowKeys.value.length === 0) return
      try {
        await docApi.deleteBatch(selectedRowKeys.value)
        message.success(`批量删除成功，共删除 ${selectedRowKeys.value.length} 条`)
        selectedRowKeys.value = []
        loadDocs()
      } catch {
        message.error('批量删除失败')
      }
    }

    const aiDocAssist = async (type: string) => {
      if (assisting.value) return
      assisting.value = type
      const topic = form.value.name || ''
      const selectedText = form.value.content?.substring(0, 500) || ''
      try {
        const res: any = await aiApi.generate({ type, topic, selectedText })
        form.value.content = res.content.text
        message.success('🤖 AI 生成完成')
      } catch {
        message.error('AI 生成失败')
      } finally {
        assisting.value = null
      }
    }

    onMounted(loadEbooks)

    return { ebooks, ebookId, docTree, columns, selectedRowKeys, modalVisible, form, assisting, showModal, handleSave, handleDelete, handleBatchDelete, loadDocs, aiDocAssist }
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
  position: relative;
  padding-left: 16px;
}

.page-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border-radius: 2px;
}

.header-actions {
  display: flex;
  align-items: center;
}

.add-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
}

.ai-assist {
  padding: 12px;
  background: #f8f9ff;
  border-radius: 8px;
  border: 1px dashed #d6e4ff;
}

.ai-assist-label {
  font-size: 13px;
  color: #1677ff;
  font-weight: 500;
  margin-right: 8px;
}

.ai-assist-btn {
  border-radius: 8px;
  font-size: 12px;
  color: #1677ff;
  border-color: #d6e4ff;
}

.ai-assist-btn:hover {
  background: #1677ff;
  color: white;
  border-color: #1677ff;
}
</style>