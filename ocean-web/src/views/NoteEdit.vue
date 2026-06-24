<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="note-edit-container">
      <div class="note-edit-header">
        <a-button class="back-btn" @click="$router.push('/notes')">← 返回</a-button>
        <h2>{{ readonly ? '查看笔记' : isOwner ? '编辑笔记' : form.id ? '查看笔记' : '新建笔记' }}</h2>
        <template v-if="isOwner">
          <a-switch
            v-model:checked="isPublic"
            checked-children="🌍 公开"
            un-checked-children="🔒 私有"
            class="visibility-switch"
          />
        </template>
        <a-button
          v-if="form.id"
          class="fav-btn"
          :class="{ 'fav-active': favorited }"
          @click="handleFavorite"
          :loading="favLoading"
        >
          {{ favorited ? '⭐ 已收藏' : '☆ 收藏' }}
        </a-button>
        <a-button
          v-if="form.id"
          class="vote-btn"
          :loading="voteLoading"
          @click="handleVote"
        >
          👍 {{ form.voteCount || 0 }}
        </a-button>
      </div>

      <div class="note-edit-card">
        <template v-if="!isOwner && form.id">
          <div class="readonly-title">{{ form.title }}</div>
          <div class="readonly-content">{{ form.content }}</div>
        </template>
        <template v-else>
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
        </template>
      </div>

      <div v-if="isOwner" class="note-actions">
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

      <div v-else-if="form.id" class="note-readonly-footer">
        <a-button @click="$router.push('/notes')">← 返回笔记列表</a-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { noteApi, aiApi, favoriteApi } from '../api'
import { message } from 'ant-design-vue'

export default defineComponent({
  name: 'NoteEdit',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const store = useStore()
    const user = computed(() => store.state.user)
    const form = ref<any>({ title: '', content: '', isPublic: 0 })
    const saving = ref(false)
    const generating = ref<string | null>(null)
    const favorited = ref(false)
    const favLoading = ref(false)
    const readonly = computed(() => route.query.readonly === '1')
    const isOwner = computed(() => {
      if (readonly.value) return false
      return !form.value.id || form.value.userId === user.value.userId
    })
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
          // 检查收藏状态
          try {
            const favRes: any = await favoriteApi.noteCheck(id)
            favorited.value = favRes.content?.favorited || false
          } catch {}
        } catch {
          message.error('笔记加载失败')
        }
      }
    })

    const handleFavorite = async () => {
      if (!form.value.id) return
      favLoading.value = true
      try {
        const res: any = await favoriteApi.noteToggle(form.value.id)
        favorited.value = res.content?.favorited || false
        message.success(favorited.value ? '⭐ 收藏成功！' : '已取消收藏')
      } catch {
        message.error('操作失败')
      } finally {
        favLoading.value = false
      }
    }

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

    const voteLoading = ref(false)
    const handleVote = async () => {
      if (!user.value.token) {
        message.warning('请先登录')
        return
      }
      voteLoading.value = true
      try {
        await noteApi.vote(form.value.id)
        message.success('点赞成功！')
        form.value.voteCount = (form.value.voteCount || 0) + 1
      } catch (e: any) {
        message.error(e?.response?.data?.message || '点赞失败')
      } finally {
        voteLoading.value = false
      }
    }

    return { form, isPublic, isOwner, saving, generating, favorited, favLoading, voteLoading, handleFavorite, handleSave, aiGenerate, handleVote }
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

.fav-btn {
  border-radius: 10px;
  font-size: 13px;
  transition: all 0.2s;
}

.fav-btn.fav-active {
  color: #faad14;
  border-color: #faad14;
  background: #fffbe6;
}

.save-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
  padding: 0 24px;
}

.note-readonly-footer {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding: 16px 24px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.vote-btn {
  border-radius: 10px;
  font-size: 13px;
  color: #ff4d4f;
  border-color: #ffccc7;
  transition: all 0.2s;
}
.vote-btn:hover {
  color: white !important;
  background: #ff4d4f;
  border-color: #ff4d4f;
}

/* 只读笔记阅读样式 */
.readonly-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.4;
  padding: 8px 0 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.readonly-content {
  font-size: 16px;
  line-height: 1.9;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 0 0 8px;
  min-height: 300px;
}
</style>