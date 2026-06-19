<template>
  <div class="profile-card">
    <div class="card-header">
      <h3>我的收藏</h3>
    </div>
    <div class="card-body">
      <a-empty v-if="!loading && list.length === 0" description="暂无收藏" />
      <a-list v-else :data-source="list" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <a @click="goToDoc(item)">{{ item.docName }}</a>
              </template>
              <template #description>
                所属电子书：{{ item.ebookName || '未知' }} | 收藏时间：{{ item.createTime?.slice(0, 10) }}
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
import { defineComponent, ref, onMounted } from 'vue'
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

    const goToDoc = (item: any) => {
      router.push(`/ebook/${item.ebookId || ''}`)
    }

    const handleRemove = async (item: any) => {
      try {
        const res: any = await favoriteApi.toggle(item.docId)
        if (!res.content?.favorited) {
          message.success('已取消收藏')
          loadList()
        }
      } catch {
        message.error('操作失败')
      }
    }

    onMounted(loadList)

    return { list, total, page, pageSize, loading, loadList, goToDoc, handleRemove }
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
