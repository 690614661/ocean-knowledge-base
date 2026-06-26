<template>
  <div class="page-container" style="padding-bottom: 48px">
    <div class="search-header">
      <h1 class="search-title">🔍 搜索结果</h1>
      <div class="search-bar">
        <a-input-search
          v-model:value="keyword"
          placeholder="输入关键词搜索文档和笔记..."
          enter-button="搜索"
          size="large"
          style="max-width: 600px"
          @search="onSearch"
        />
      </div>
    </div>

    <!-- 过滤器 -->
    <div class="filter-bar" v-if="loaded">
      <div class="filter-row">
        <div class="filter-item">
          <span class="filter-label">类型</span>
          <a-select v-model:value="filterType" style="width: 120px" @change="onFilterChange">
            <a-select-option value="all">全部</a-select-option>
            <a-select-option value="doc">📄 文档</a-select-option>
            <a-select-option value="note">📝 笔记</a-select-option>
          </a-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">分类</span>
          <a-cascader
            v-model:value="filterCategory"
            :options="categoryOptions"
            placeholder="全部"
            style="width: 200px"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            change-on-select
            allow-clear
            @change="onFilterChange"
          />
        </div>
        <div class="filter-item">
          <span class="filter-label">排序</span>
          <a-select v-model:value="filterSortBy" style="width: 130px" @change="onFilterChange">
            <a-select-option value="relevance">相关度</a-select-option>
            <a-select-option value="time">最新</a-select-option>
            <a-select-option value="hot">最热</a-select-option>
          </a-select>
        </div>
      </div>
      <!-- 激活的过滤标签 -->
      <div class="active-tags" v-if="activeTagCount > 0">
        <a-tag closable v-if="filterType !== 'all'" @close="filterType = 'all'; onFilterChange()">
          类型：{{ typeLabel }}
        </a-tag>
        <a-tag closable v-if="filterCategory && filterCategory.length > 0" @close="filterCategory = []; onFilterChange()">
          分类已选
        </a-tag>
        <a-tag closable v-if="filterSortBy !== 'relevance'" @close="filterSortBy = 'relevance'; onFilterChange()">
          排序：{{ sortByLabel }}
        </a-tag>
      </div>
    </div>

    <div class="search-tips" v-if="!loading && (keyword || activeTagCount > 0)">
      <span v-if="keyword">关于 "<strong>{{ keyword }}</strong>" 的搜索结果，共 {{ totalCount }} 条</span>
      <span v-else>共 {{ totalCount }} 条结果</span>
    </div>

    <div class="search-results" v-if="results.length > 0">
      <div
        v-for="item in results"
        :key="item.id"
        class="result-card"
        @click="goToDoc(item)"
      >
        <div class="result-title" v-html="item.title || item.name"></div>
        <div class="result-snippet" v-html="item.content"></div>
        <div class="result-meta">
          <span v-if="item._index === 'note_index'" style="color:#52c41a">📝 来自笔记</span>
          <span v-else>📄 来自电子书</span>
          <span style="margin-left: 12px">· {{ item.viewCount || 0 }} 次阅读</span>
        </div>
      </div>

      <div class="pagination-wrapper" v-if="totalCount > pageSize">
        <a-pagination
          v-model:current="currentPage"
          :total="totalCount"
          :page-size="pageSize"
          :show-total="total => `共 ${total} 条`"
          @change="onPageChange"
        />
      </div>
    </div>

    <div v-else-if="!loading && (keyword || activeTagCount > 0)" class="empty-state">
      <div class="empty-icon">🐡</div>
      <p>没有找到相关结果</p>
      <p class="empty-hint">试试其他关键词或调整筛选条件</p>
    </div>

    <div v-else-if="loading" class="loading-state">
      <a-spin size="large" tip="搜索中..." />
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>输入关键词开始搜索</p>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi, categoryApi } from '../api'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'Search',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const keyword = ref('')
    const results = ref<any[]>([])
    const totalCount = ref(0)
    const loading = ref(false)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const loaded = ref(false)

    // 过滤器
    const filterType = ref('all')
    const filterCategory = ref<any[]>([])
    const filterSortBy = ref('relevance')
    const categories = ref<any[]>([])

    const typeLabel = computed(() => {
      const map: any = { all: '全部', doc: '文档', note: '笔记' }
      return map[filterType.value] || '全部'
    })
    const sortByLabel = computed(() => {
      const map: any = { relevance: '相关度', time: '最新', hot: '最热' }
      return map[filterSortBy.value] || '相关度'
    })
    const activeTagCount = computed(() => {
      let count = 0
      if (filterType.value !== 'all') count++
      if (filterCategory.value.length > 0) count++
      if (filterSortBy.value !== 'relevance') count++
      return count
    })

    const categoryOptions = computed(() => {
      return categories.value.filter((c: any) => c.parent === 0).map((c1: any) => {
        return {
          ...c1,
          children: categories.value.filter((c: any) => c.parent === c1.id)
        }
      })
    })

    const buildParams = () => {
      const params: any = { page: currentPage.value, size: pageSize.value }
      if (keyword.value) params.keyword = keyword.value
      if (filterType.value !== 'all') params.type = filterType.value
      if (filterSortBy.value !== 'relevance') params.sortBy = filterSortBy.value
      if (filterCategory.value && filterCategory.value.length > 0) {
        // cascader 返回 [一级ID] 或 [一级ID, 二级ID]
        params.categoryId = filterCategory.value[filterCategory.value.length - 1]
      }
      return params
    }

    const doSearch = async () => {
      if (!keyword.value && activeTagCount.value === 0) {
        results.value = []
        totalCount.value = 0
        return
      }
      loading.value = true
      try {
        const res: any = await searchApi.search(buildParams())
        results.value = res.content?.list || []
        totalCount.value = parseInt(res.content?.total) || 0
      } finally {
        loading.value = false
      }
    }

    const animateResults = () => {
      const cards = document.querySelectorAll('.result-card')
      if (cards.length) {
        anime.set(cards, { opacity: 0, translateX: 30 })
        anime({
          targets: cards,
          opacity: [0, 1],
          translateX: [30, 0],
          duration: 500,
          delay: anime.stagger(80),
          easing: 'easeOutCubic'
        })
      }
    }

    watch(loading, () => {
      if (!loading.value) {
        nextTick(() => {
          if (results.value.length > 0) {
            animateResults()
          } else {
            const icon = document.querySelector('.empty-icon')
            if (icon) {
              anime({
                targets: icon,
                translateY: [0, -10, 0],
                scale: [1, 1.1, 1],
                duration: 2000,
                loop: true,
                easing: 'easeInOutSine'
              })
            }
          }
        })
      }
    })

    const onSearch = () => {
      currentPage.value = 1
      doSearch()
    }

    const onFilterChange = () => {
      currentPage.value = 1
      doSearch()
    }

    const onPageChange = (page: number) => {
      currentPage.value = page
      doSearch()
    }

    const goToDoc = (item: any) => {
      if (item._index === 'note_index') {
        router.push(`/note/edit/${item.id}?readonly=1`)
      } else {
        router.push(`/ebook/${item.ebookId}`)
      }
    }

    onMounted(async () => {
      keyword.value = (route.query.keyword as string) || ''
      // 加载分类
      try {
        const res: any = await categoryApi.all()
        categories.value = res.content || []
      } catch {}
      loaded.value = true
      if (keyword.value) onSearch()
    })

    return {
      keyword, results, totalCount, loading, currentPage, pageSize,
      filterType, filterCategory, filterSortBy, loaded,
      categories, categoryOptions, typeLabel, sortByLabel, activeTagCount,
      onSearch, onFilterChange, onPageChange, goToDoc
    }
  }
})
</script>

<style scoped>
.search-header {
  padding: 32px 0 16px;
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

.filter-bar {
  max-width: 800px;
  margin: 0 auto 16px;
  background: white;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.active-tags {
  margin-top: 10px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.search-tips {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 20px;
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

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding: 16px 0;
}
</style>
