<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="note-edit-container">
      <div class="note-edit-header">
        <a-button class="back-btn" @click="$router.push('/notes')">← 返回</a-button>
        <h2>{{ form.id ? '编辑笔记' : '新建笔记' }}</h2>
        <a-switch
          v-model:checked="isPublic"
          checked-children="🌍 公开"
          un-checked-children="🔒 私有"
          class="visibility-switch"
        />
      </div>

      <div class="note-edit-card">
        <a-input
          v-model:value="form.title"
          placeholder="输入笔记标题..."
          class="title-input"
          size="large"
        />
        <a-textarea
          v-model:value="form.content"
          :rows="16"
          placeholder="开始写下你的学习心得..."
          class="content-editor"
        />
      </div>

      <div class="note-actions">
        <div class="ai-buttons">
          <a-button class="ai-btn" :loading="generating === 'note_generate'" :disabled="!!generating" @click="aiGenerate('note_generate')">
            🤖 AI 生成
          </a-button>
          <a-button class="ai-btn" :loading="generating === 'note_polish'" :disabled="!!generating" @click="aiGenerate('note_polish')">
            ✨ AI 润色
          </a-button>
          <a-button class="ai-btn" :loading="generating === 'note_expand'" :disabled="!!generating" @click="aiGenerate('note_expand')">
            📝 AI 扩写
          </a-button>
          <a-button class="ai-btn" :loading="generating === 'note_summarize'" :disabled="!!generating" @click="aiGenerate('note_summarize')">
            📋 AI 总结
          </a-button>
        </div>
        <div class="save-actions">
          <a-button @click="$router.push('/notes')">取消</a-button>
          <a-button type="primary" class="save-btn" @click="handleSave" :loading="saving">
            💾 保存笔记
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noteApi, aiApi } from '../api'
import { message } from 'ant-design-vue'

export default defineComponent({
  name: 'NoteEdit',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const form = ref<any>({ title: '', content: '', isPublic: 0 })
    const saving = ref(false)
    const generating = ref<string | null>(null)
    const isPublic = computed({
      get: () => form.value.isPublic === 1,
      set: (val: boolean) => { form.value.isPublic = val ? 1 : 0 }
    })

    onMounted(async () => {
      const id = route.params.id as string
      if (id) {
        try {
          const res: any = await noteApi.detail(id)
          form.value = res.content
        } catch {
          message.error('笔记加载失败')
        }
      }
    })

    const handleSave = async () => {
      if (!form.value.title?.trim()) {
        message.warning('请输入笔记标题')
        return
      }
      saving.value = true
      try {
        await noteApi.save(form.value)
        message.success('🎉 保存成功')
        router.push('/notes')
      } catch {} finally {
        saving.value = false
      }
    }

    const aiGenerate = async (type: string) => {
      if (generating.value) return
      generating.value = type
      try {
        const selectedText = window.getSelection()?.toString() || ''
        const topic = form.value.title || ''
        const res: any = await aiApi.generate({
          type,
          topic,
          selectedText: selectedText || form.value.content?.substring(0, 500) || ''
        })
        form.value.content = res.content.text
        message.success('🤖 AI 生成完成')
      } catch {
        message.error('AI 生成失败，请稍后重试')
      } finally {
        generating.value = null
      }
    }

    return { form, isPublic, saving, generating, handleSave, aiGenerate }
  }
})
</script>

<style scoped>
.note-edit-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px 0;
}

.note-edit-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.note-edit-header h2 {
  flex: 1;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.back-btn {
  border-radius: 8px;
  color: #666;
}

.visibility-switch {
  font-size: 13px;
}

.note-edit-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.title-input {
  margin-bottom: 16px;
}

.title-input :deep(.ant-input) {
  font-size: 20px;
  font-weight: 600;
  border: none;
  border-bottom: 2px solid #e8ecf0;
  border-radius: 0;
  padding: 8px 0;
  transition: border-color 0.3s;
}

.title-input :deep(.ant-input:focus) {
  border-bottom-color: #1677ff;
  box-shadow: none;
}

.content-editor {
  border: none;
  border-radius: 0;
  font-size: 15px;
  line-height: 1.8;
  padding: 8px 0;
  resize: vertical;
}

.content-editor :deep(.ant-input) {
  border: none;
  padding: 0;
  box-shadow: none;
}

.content-editor:focus, .content-editor :deep(.ant-input:focus) {
  box-shadow: none;
}

.note-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.ai-buttons {
  display: flex;
  gap: 8px;
}

.ai-btn {
  border-radius: 10px;
  color: #1677ff;
  border-color: #d6e4ff;
  font-size: 13px;
  transition: all 0.2s;
}

.ai-btn:hover {
  background: #1677ff;
  color: white;
  border-color: #1677ff;
  transform: translateY(-1px);
}

.save-actions {
  display: flex;
  gap: 8px;
}

.save-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
  padding: 0 24px;
}
</style>