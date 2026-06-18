<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="ai-layout">
      <!-- 对话列表侧栏 -->
      <div class="conv-sidebar">
        <div class="conv-header">
          <span class="conv-title">💬 对话列表</span>
        </div>
        <a-button type="primary" class="new-conv-btn" @click="newConversation">
          + 新对话
        </a-button>
        <div class="conv-list">
          <div
            v-for="conv in conversations"
            :key="conv.id"
            class="conv-item"
            :class="{ active: currentId === conv.id }"
            @click="selectConversation(conv)"
          >
            <div class="conv-item-icon">💬</div>
            <div class="conv-item-text">
              <div class="conv-item-title">{{ conv.title || '新对话' }}</div>
              <div class="conv-item-time">{{ conv.updateTime?.slice(0, 10) }}</div>
            </div>
            <a-button
              type="text"
              size="small"
              class="conv-delete-btn"
              danger
              @click.stop="handleDelete(conv.id)"
            >✕</a-button>
          </div>
          <div v-if="conversations.length === 0" class="conv-empty">暂无对话记录</div>
        </div>
      </div>

      <!-- 对话区 -->
      <div class="chat-area">
        <div class="chat-header">
          <span>{{ currentId ? '当前对话' : '开始新对话' }}</span>
        </div>

        <div class="chat-messages" ref="messageListRef">
          <div v-if="messages.length === 0" class="chat-welcome">
            <div class="welcome-icon">🐋</div>
            <h2>有什么关于海洋的问题？</h2>
            <p>可以问关于海洋生物、生态、分类等方面的问题</p>
            <div class="welcome-suggestions">
              <div
                v-for="suggestion in suggestions"
                :key="suggestion"
                class="suggestion-chip"
                @click="quickAsk(suggestion)"
              >
                {{ suggestion }}
              </div>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="msg.id || idx" class="message-row" :class="msg.role">
            <div class="message-avatar">
              <span v-if="msg.role === 'user'">👤</span>
              <span v-else class="ai-avatar">🤖</span>
            </div>
            <div class="message-content" v-if="msg.role === 'user'">{{ msg.content }}</div>
            <div class="message-content ai-content" v-else v-html="renderMarkdown(msg.content)"></div>
          </div>

          <div v-if="loading" class="message-row assistant">
            <div class="message-avatar"><span class="ai-avatar">🤖</span></div>
            <div class="message-content ai-content">
              <span class="typing-dots">
                <span class="dot">.</span><span class="dot">.</span><span class="dot">.</span>
              </span>
            </div>
          </div>
        </div>

        <div class="chat-input-bar">
          <a-input
            v-model:value="inputMessage"
            placeholder="输入你的问题..."
            size="large"
            class="chat-input"
            @pressEnter="sendMessage"
            :disabled="loading"
          />
          <a-button
            type="primary"
            class="send-btn"
            :loading="loading"
            @click="sendMessage"
          >
            {{ loading ? '思考中...' : '发送' }}
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, nextTick } from 'vue'
import { aiApi } from '../api'
import { message } from 'ant-design-vue'
import { marked } from 'marked'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'AiChat',
  setup() {
    const conversations = ref<any[]>([])
    const currentId = ref('')
    const messages = ref<any[]>([])
    const inputMessage = ref('')
    const loading = ref(false)
    const messageListRef = ref<HTMLElement>()

    const suggestions = [
      '鲸鱼为什么是哺乳动物？',
      '珊瑚礁生态系统的特点',
      '深海中有什么生物？',
      '海洋生物如何分类？'
    ]

    const renderMarkdown = (content: string) => {
      try {
        return marked.parse(content || '')
      } catch {
        return content
      }
    }

    const animateMessageIn = () => {
      const rows = document.querySelectorAll('.chat-messages .message-row')
      if (rows.length) {
        const lastRow = rows[rows.length - 1] as HTMLElement
        anime.set(lastRow, { opacity: 0, translateY: 20, scale: 0.98 })
        anime({
          targets: lastRow,
          opacity: [0, 1],
          translateY: [20, 0],
          scale: [0.98, 1],
          duration: 400,
          easing: 'easeOutCubic'
        })
      }
    }

    const animateAllMessages = () => {
      const rows = document.querySelectorAll('.chat-messages .message-row')
      if (rows.length > 2) {
        anime.set(rows, { opacity: 0, translateY: 15 })
        anime({
          targets: rows,
          opacity: [0, 1],
          translateY: [15, 0],
          duration: 400,
          delay: anime.stagger(30),
          easing: 'easeOutCubic'
        })
      }
    }

    const scrollToBottom = async () => {
      await nextTick()
      if (messageListRef.value) {
        messageListRef.value.scrollTop = messageListRef.value.scrollHeight
      }
      animateMessageIn()
    }

    const loadConversations = async () => {
      try {
        const res: any = await aiApi.conversations()
        conversations.value = res.content || []
      } catch {}
    }

    const selectConversation = async (conv: any) => {
      currentId.value = conv.id
      try {
        const res: any = await aiApi.messages(conv.id)
        messages.value = res.content || []
        await scrollToBottom()
        animateAllMessages()
      } catch {
        message.error('加载对话失败')
      }
    }

    const handleDelete = async (id: string) => {
      try {
        await aiApi.delete(id)
        message.success('会话已删除')
        if (currentId.value === id) {
          currentId.value = ''
          messages.value = []
        }
        loadConversations()
      } catch {
        message.error('删除失败')
      }
    }

    const newConversation = () => {
      currentId.value = ''
      messages.value = []
      loadConversations()
    }

    const sendMessage = async () => {
      if (!inputMessage.value.trim() || loading.value) return
      const msg = inputMessage.value
      inputMessage.value = ''
      messages.value.push({ id: Date.now().toString(), role: 'user', content: msg })
      await scrollToBottom()
      loading.value = true
      try {
        const res: any = await aiApi.chat({
          conversationId: currentId.value || undefined,
          message: msg
        })
        currentId.value = res.content.conversationId
        messages.value.push({
          id: res.content.messageId,
          role: 'assistant',
          content: res.content.content
        })
        loadConversations()
        await scrollToBottom()
      } catch {} finally {
        loading.value = false
      }
    }

    const quickAsk = (question: string) => {
      inputMessage.value = question
      sendMessage()
    }

    onMounted(() => {
      loadConversations()
    })

    return {
      conversations, currentId, messages, inputMessage, loading, messageListRef,
      suggestions, renderMarkdown, selectConversation, newConversation, handleDelete,
      sendMessage, quickAsk
    }
  }
})
</script>

<style scoped>
.ai-layout {
  display: flex;
  gap: 24px;
  height: calc(100vh - 200px);
  padding: 24px 0;
}

/* 对话侧栏 */
.conv-sidebar {
  width: 260px;
  flex-shrink: 0;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conv-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.conv-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.new-conv-btn {
  margin: 12px 16px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
}

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
}

.conv-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.conv-item:hover {
  background: #e6f4ff;
}

.conv-item.active {
  background: #e6f4ff;
  border: 1px solid #1677ff;
}

.conv-item-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.conv-item-text {
  flex: 1;
  min-width: 0;
}

.conv-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-item-time {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.conv-delete-btn {
  opacity: 0;
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  font-size: 11px;
  line-height: 1;
  padding: 0;
  margin-top: 2px;
  transition: opacity 0.2s ease;
}

.conv-item:hover .conv-delete-btn {
  opacity: 1;
}

.conv-empty {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 13px;
}

/* 对话区 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

/* 欢迎区 */
.chat-welcome {
  text-align: center;
  padding: 48px 0;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.chat-welcome h2 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.chat-welcome p {
  color: #999;
  margin: 0 0 24px;
}

.welcome-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 500px;
  margin: 0 auto;
}

.suggestion-chip {
  padding: 8px 18px;
  border-radius: 20px;
  background: #f0f5ff;
  color: #1677ff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #d6e4ff;
}

.suggestion-chip:hover {
  background: #1677ff;
  color: white;
  border-color: #1677ff;
  transform: translateY(-1px);
}

/* 消息气泡 */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.message-row.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.ai-avatar {
  display: inline-flex;
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.message-row.user .message-content {
  background: linear-gradient(135deg, #1677ff, #4096ff);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-row.assistant .message-content {
  background: #f0f2f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.ai-content {
  font-size: 14px;
}

.ai-content :deep(p) {
  margin: 8px 0;
}

.ai-content :deep(ul), .ai-content :deep(ol) {
  padding-left: 20px;
}

.ai-content :deep(code) {
  background: #e8e8e8;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}

.ai-content :deep(pre) {
  background: #1e1e2e;
  color: #c9d1d9;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
}

.typing-dots {
  display: inline-flex;
  gap: 2px;
}

.dot {
  animation: dotBounce 1.4s infinite ease-in-out;
  font-size: 24px;
  line-height: 1;
  color: #999;
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotBounce {
  0%, 80%, 100% { opacity: 0.3; transform: translateY(0); }
  40% { opacity: 1; transform: translateY(-4px); }
}

/* 输入栏 */
.chat-input-bar {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.chat-input {
  border-radius: 12px;
}

.chat-input :deep(.ant-input) {
  border-radius: 12px;
}

.send-btn {
  border-radius: 12px;
  height: 40px;
  padding: 0 24px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 600;
}
</style>