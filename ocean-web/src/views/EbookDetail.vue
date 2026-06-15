<template>
  <div style="display: flex; gap: 16px; padding: 24px 0">
    <!-- 目录树 -->
    <a-card style="width: 300px; flex-shrink: 0" title="文档目录">
      <a-tree
        :tree-data="docTree"
        :field-names="{ title: 'name', key: 'id', children: 'children' }"
        @select="onDocSelect"
      />
    </a-card>

    <!-- 文档内容 -->
    <a-card style="flex: 1" :title="currentDoc?.name || '请选择文档'">
      <template #extra>
        <a-button v-if="currentDoc && user.token" type="primary" @click="handleVote">
          👍 点赞 ({{ currentDoc?.voteCount || 0 }})
        </a-button>
      </template>
      <div v-if="currentDoc" v-html="currentDoc.content" class="doc-content"></div>
      <a-empty v-else description="请从左侧目录选择文档" />
    </a-card>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'
import { docApi } from '../api'
import { computed } from 'vue'

export default defineComponent({
  name: 'EbookDetail',
  setup() {
    const route = useRoute()
    const store = useStore()
    const user = computed(() => store.state.user)
    const docTree = ref<any[]>([])
    const currentDoc = ref<any>(null)

    const loadDocs = async () => {
      const ebookId = Number(route.params.id)
      const res: any = await docApi.list(ebookId)
      docTree.value = res.content
    }

    const onDocSelect = async (selectedKeys: any[]) => {
      if (selectedKeys.length === 0) return
      const docId = selectedKeys[0]
      const res: any = await docApi.detail(docId)
      currentDoc.value = res.content
    }

    const handleVote = async () => {
      if (!currentDoc.value) return
      try {
        await docApi.vote(currentDoc.value.id)
        message.success('点赞成功')
        currentDoc.value.voteCount++
      } catch (e: any) {
        // 已点赞提示
      }
    }

    onMounted(loadDocs)

    return { docTree, currentDoc, user, onDocSelect, handleVote }
  }
})
</script>

<style scoped>
.doc-content :deep(img) { max-width: 100%; }
.doc-content :deep(table) { border-collapse: collapse; width: 100%; }
.doc-content :deep(td), .doc-content :deep(th) { border: 1px solid #ddd; padding: 8px; }
</style>