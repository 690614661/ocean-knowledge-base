<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="search-header">
      <h1 class="search-title">🔍 搜索结果</h1>
      <div class="search-bar">
        <a-input-search
          v-model:value="keyword"
          placeholder="输入关键词搜索文档..."
          enter-button="搜索"
          size="large"
          style="max-width: 600px"
          @search="onSearch"
        />
      </div>
      <div class="search-tips" v-if="!loading && keyword">
        <span>关于 "<strong>{{ keyword }}</strong>" 的搜索结果，共 {{ results.length }} 条</span>
      </div>
    </div>

    <div class="search-results" v-if="results.length > 0">
      <div
        v-for="item in results"
        :key="item.id"
        class="result-card"
        @click="goToDoc(item)"
      >
        <div class="result-title" v-html="item.name"></div>
        <div class="result-snippet" v-html="item.content"></div>
        <div class="result-meta">
          <span>📄 来自电子书</span>
        </div>
      </div>
    </div>

    <div v-else-if="!loading && keyword" class="empty-state">
      <div class="empty-icon">🐡</div>
      <p>没有找到相关结果</p>
      <p class="empty-hint">试试其他关键词</p>
    </div>

    <div v-else-if="loading" class="loading-state">
      <a-spin size="large" tip="搜索中..." />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '../api'

export default defineComponent({
  name: 'Search',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const keyword = ref('')
    const results = ref<any[]>([])
    const loading = ref(false)

    const onSearch = async () => {
      if (!keyword.value) return
      loading.value = true
      try {
        const res: any = await searchApi.search({ keyword: keyword.value })
        results.value = res.content?.list || []
      } finally {
        loading.value = false
      }
    }

    const goToDoc = (item: any) => {
      router.push(`/ebook/${item.ebookId}`)
    }

    onMounted(() => {
      keyword.value = (route.query.keyword as string) || ''
      if (keyword.value) onSearch()
    })

    return { keyword, results, loading, onSearch, goToDoc }
  }
})
</script>

<style scoped>
.search-header {
  padding: 32px 0;
  text-align: center;
}

.search-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 20px;
}

.search-bar {
  margin-bottom: 16px;
}

.search-bar :deep(.ant-input) {
  border-radius: 12px 0 0 12px;
  height: 48px;
}

.search-bar :deep(.ant-btn) {
  border-radius: 0 12px 12px 0;
  height: 48px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 600;
}

.search-tips {
  color: #666;
  font-size: 14px;
}

.search-results {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
}

.result-card:hover {
  transform: translateX(4px);
  box-shadow: 0 4px 20px rgba(22, 119, 255, 0.1);
  border-color: #1677ff;
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  color: #1677ff;
  margin-bottom: 8px;
}

.result-title :deep(em) {
  color: #ff4d4f;
  font-style: normal;
  background: #fff0f0;
  padding: 0 4px;
  border-radius: 2px;
}

.result-snippet {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 12px;
}

.result-snippet :deep(em) {
  color: #ff4d4f;
  font-style: normal;
  background: #fff0f0;
  padding: 0 4px;
  border-radius: 2px;
}

.result-meta {
  font-size: 12px;
  color: #999;
}

.empty-state, .loading-state {
  text-align: center;
  padding: 80px 0;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-hint {
  font-size: 13px;
  color: #ccc;
}
</style>