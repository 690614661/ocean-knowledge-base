<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>我的收藏</h3>
    </div>
    <div class="card-body">
      <div class="filter-bar" style="margin-bottom: 16px">
        <a-space>
          <a-button :type="filterType === 'all' ? 'primary' : 'default'" size="small" @click="filterType = 'all'">全部</a-button>
          <a-button :type="filterType === 1 ? 'primary' : 'default'" size="small" @click="filterType = 1">📄 文档</a-button>
          <a-button :type="filterType === 2 ? 'primary' : 'default'" size="small" @click="filterType = 2">📝 笔记</a-button>
        </a-space>
      </div>
      <a-empty v-if="!loading && filteredList.length === 0" description="暂无收藏" />
      <a-list v-else :data-source="filteredList" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a @click="goToDetail(item)">
                  <span v-if="item.targetType === 2" style="color: #52c41a; margin-right: 6px">📝</span>
                  <span v-else style="margin-right: 6px">📄</span>
                  {{ item.name }}
                </a>
              </template>
              <template #description>
                <template v-if="item.targetType === 2">
                  笔记 | 收藏时间：{{ item.createTime?.slice(0, 10) }}
                </template>
                <template v-else>
                  所属电子书：{{ item.ebookName || '未知' }} | 收藏时间：{{ item.createTime?.slice(0, 10) }}
                </template>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button size="small" danger @click="handleRemove(item)">取消收藏</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <div class="pagination-wrap" v-if="total > pageSize">
        <a-pagination
          v-model:current="page"
          :page-size="pageSize"
          :total="total"
          @change="loadList"
          size="small"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { favoriteApi } from '../../api'

export default defineComponent({
  name: 'Favorites',
  setup() {
    const router = useRouter()
    const list = ref<any[]>([])
    const total = ref(0)
    const page = ref(1)
    const pageSize = ref(10)
    const loading = ref(true)
    const filterType = ref<number | string>('all')

    const filteredList = computed(() => {
      if (filterType.value === 'all') return list.value
      return list.value.filter((item: any) => item.targetType === filterType.value)
    })

    const loadList = async () => {
      loading.value = true
      try {
        const res: any = await favoriteApi.list({ page: page.value, size: pageSize.value })
        list.value = res.content?.list || []
        total.value = res.content?.total || 0
      } catch {
        message.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const goToDetail = (item: any) => {
      if (item.targetType === 2) {
        // 笔记
        router.push(`/note/edit/${item.docId}`)
      } else {
        // 文档
        router.push(`/ebook/${item.ebookId || ''}`)
      }
    }

    const handleRemove = async (item: any) => {
      try {
        let res: any
        if (item.targetType === 2) {
          res = await favoriteApi.noteToggle(item.docId)
        } else {
          res = await favoriteApi.toggle(item.docId)
        }
        if (!res.content?.favorited) {
          message.success('已取消收藏')
          loadList()
        }
      } catch {
        message.error('操作失败')
      }
    }

    onMounted(loadList)

    return { list, total, page, pageSize, loading, filterType, filteredList, loadList, goToDetail, handleRemove }
  }
})
</script>

<style scoped>
.profile-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.card-body {
  padding: 24px;
}

.pagination-wrap {
  margin-top: 16px;
  text-align: center;
}

</style>
