<template>
  <div style="padding: 24px 0">
    <a-card title="搜索结果">
      <template #extra>
        <a-input-search v-model:value="keyword" placeholder="搜索关键词" @search="onSearch" style="width: 300px" />
      </template>
      <a-list :data-source="results" :loading="loading">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>
                <span v-html="item.name" @click="goToDoc(item)" style="cursor: pointer; color: #1890ff"></span>
              </template>
              <template #description>
                <span v-html="item.content"></span>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
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