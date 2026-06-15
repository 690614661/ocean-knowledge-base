<template>
  <div style="padding: 24px 0; display: flex; gap: 16px; height: calc(100vh - 200px)">
    <!-- 对话列表 -->
    <a-card style="width: 250px" title="对话列表">
      <a-button type="primary" block @click="newConversation" style="margin-bottom: 12px">新对话</a-button>
      <a-list :data-source="conversations" size="small">
        <template #renderItem="{ item }">
          <a-list-item style="cursor: pointer; padding: 8px" :style="{ background: currentId === item.id ? '#e6f7ff' : '' }" @click="selectConversation(item)">
            {{ item.title || '新对话' }}
          </a-list-item>
        </template>
      </a-list>
    </a-card>
    <!-- 对话区 -->
    <a-card style="flex: 1; display: flex; flex-direction: column">
      <div style="flex: 1; overflow-y: auto; padding: 16px">
        <div v-for="msg in messages" :key="msg.id" :style="{ textAlign: msg.role === 'user' ? 'right' : 'left', marginBottom: '16px' }">
          <div :style="{ display: 'inline-block', maxWidth: '70%', padding: '12px 16px', borderRadius: '8px', background: msg.role === 'user' ? '#1890ff' : '#f0f0f0', color: msg.role === 'user' ? '#fff' : '#333' }">
            <div v-if="msg.role === 'assistant'" v-html="renderMarkdown(msg.content)"></div>
            <div v-else>{{ msg.content }}</div>
          </div>
        </div>
        <div v-if="loading" style="text-align: center; color: #999">AI 正在思考...</div>
      </div>
      <div style="display: flex; gap: 8px">
        <a-input v-model:value="inputMessage" placeholder="输入问题..." @pressEnter="sendMessage" />
        <a-button type="primary" :loading="loading" @click="sendMessage">发送</a-button>
      </div>
    </a-card>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { aiApi } from '../api'
import { marked } from 'marked'
export default defineComponent({
  setup() {
    const conversations = ref<any[]>([])
    const currentId = ref('')
    const messages = ref<any[]>([])
    const inputMessage = ref('')
    const loading = ref(false)
    const renderMarkdown = (content: string) => marked(content)
    const loadConversations = async () => { try { const res: any = await aiApi.conversations(); conversations.value = res.content } catch(e) {} }
    const selectConversation = async (conv: any) => { currentId.value = conv.id; const res: any = await aiApi.messages(conv.id); messages.value = res.content }
    const newConversation = () => { currentId.value = ''; messages.value = [] }
    const sendMessage = async () => {
      if (!inputMessage.value.trim() || loading.value) return
      const msg = inputMessage.value; inputMessage.value = ''
      messages.value.push({ id: Date.now().toString(), role: 'user', content: msg })
      loading.value = true
      try {
        const res: any = await aiApi.chat({ conversationId: currentId.value || undefined, message: msg })
        currentId.value = res.content.conversationId
        messages.value.push({ id: res.content.messageId, role: 'assistant', content: res.content.content })
        loadConversations()
      } finally { loading.value = false }
    }
    onMounted(loadConversations)
    return { conversations, currentId, messages, inputMessage, loading, renderMarkdown, selectConversation, newConversation, sendMessage }
  }
})
</script>