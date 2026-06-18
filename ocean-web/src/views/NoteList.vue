<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="notes-header">
      <h1 class="notes-title">📝 学习笔记</h1>
      <p class="notes-subtitle">记录你的学习心得，与他人分享知识</p>
    </div>

    <a-tabs class="notes-tabs" v-model:activeKey="activeTab">
      <a-tab-pane key="public" tab="🌍 公开笔记">
        <div class="notes-toolbar">
          <a-input-search
            v-if="activeTab === 'public'"
            v-model:value="searchKeyword"
            placeholder="搜索公开笔记..."
            style="width: 300px"
            @search="loadPublic"
          />
        </div>
        <a-list
          :data-source="publicNotes"
          :loading="loading"
          :locale="{ emptyText: '暂无公开笔记' }"
          class="notes-list"
        >
          <template #renderItem="{ item }">
            <a-list-item class="note-card">
              <a-list-item-meta>
                <template #title>
                  <a @click="$router.push(`/note/edit/${item.id}`)">{{ item.title }}</a>
                </template>
                <template #description>
                  <div class="note-desc">
                    <span>👁 {{ item.viewCount }} 阅读</span>
                    <span>👍 {{ item.voteCount }} 点赞</span>
                    <span>📅 {{ item.createTime?.slice(0, 10) }}</span>
                  </div>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>

      <a-tab-pane key="my" tab="📂 我的笔记" v-if="user.token">
        <div class="notes-toolbar">
          <a-button type="primary" class="create-btn" @click="$router.push('/note/edit')">
            ✏️ 新建笔记
          </a-button>
        </div>
        <a-list
          :data-source="myNotes"
          :loading="loading"
          :locale="{ emptyText: '还没有创建笔记' }"
          class="notes-list"
        >
          <template #renderItem="{ item }">
            <a-list-item class="note-card">
              <a-list-item-meta>
                <template #title>
                  <a @click="$router.push(`/note/edit/${item.id}`)">{{ item.title }}</a>
                </template>
                <template #description>
                  <div class="note-desc">
                    <span :class="item.isPublic ? 'badge-public' : 'badge-private'">
                      {{ item.isPublic ? '🌍 公开' : '🔒 私有' }}
                    </span>
                    <span>👁 {{ item.viewCount }}</span>
                    <span>👍 {{ item.voteCount }}</span>
                    <span>📅 {{ item.createTime?.slice(0, 10) }}</span>
                  </div>
                </template>
              </a-list-item-meta>
              <template #actions>
                <a-button type="link" @click="$router.push(`/note/edit/${item.id}`)">编辑</a-button>
                <a-popconfirm title="确定删除这篇笔记？" @confirm="handleDelete(item.id)">
                  <a-button type="link" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch, nextTick, onMounted } from 'vue'
import { useStore } from 'vuex'
import { noteApi } from '../api'
import { message } from 'ant-design-vue'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'NoteList',
  setup() {
    const store = useStore()
    const user = computed(() => store.state.user)
    const activeTab = ref('public')
    const publicNotes = ref<any[]>([])
    const myNotes = ref<any[]>([])
    const loading = ref(false)
    const searchKeyword = ref('')

    const animateCards = () => {
      const cards = document.querySelectorAll('.ant-list-item')
      if (cards.length) {
        anime.set(cards, { opacity: 0, translateY: 20 })
        anime({
          targets: cards,
          opacity: [0, 1],
          translateY: [20, 0],
          duration: 500,
          delay: anime.stagger(60),
          easing: 'easeOutCubic'
        })
      }
    }

    watch([publicNotes, myNotes], () => {
      nextTick(() => animateCards())
    })

    watch(activeTab, () => {
      nextTick(() => setTimeout(animateCards, 100))
    })

    const loadPublic = async () => {
      loading.value = true
      try {
        const res: any = await noteApi.publicList({ page: 1, size: 20 })
        publicNotes.value = res.content?.list || []
      } finally { loading.value = false }
    }

    const loadMy = async () => {
      if (!user.value.token) return
      try {
        const res: any = await noteApi.myList({ page: 1, size: 20 })
        myNotes.value = res.content?.list || []
      } catch {}
    }

    const handleDelete = async (id: number) => {
      await noteApi.delete(id)
      message.success('删除成功')
      loadMy()
    }

    onMounted(() => { loadPublic(); loadMy() })

    return { user, activeTab, publicNotes, myNotes, loading, searchKeyword, loadPublic, handleDelete }
  }
})
</script>

<style scoped>
.notes-header {
  text-align: center;
  padding: 32px 0 24px;
}

.notes-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.notes-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

.notes-tabs {
  background: white;
  border-radius: 16px;
  padding: 20px 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.notes-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.create-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
}

.note-card {
  padding: 16px 20px !important;
  border-radius: 12px !important;
  transition: all 0.2s ease;
  margin-bottom: 4px;
}

.note-card:hover {
  background: #f8f9ff;
}

.note-desc {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.badge-public {
  color: #52c41a;
  font-weight: 500;
}

.badge-private {
  color: #fa8c16;
  font-weight: 500;
}
</style>