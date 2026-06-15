<template>
  <div style="padding: 24px 0">
    <a-tabs>
      <a-tab-pane key="public" tab="公开笔记">
        <a-list :data-source="publicNotes" :loading="loading">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta :title="item.title" :description="`👁 ${item.viewCount} 👍 ${item.voteCount}`" />
              <template #actions>
                <a-button type="link" @click="$router.push(`/note/edit/${item.id}`)">查看</a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="my" tab="我的笔记" v-if="user.token">
        <div style="margin-bottom: 16px">
          <a-button type="primary" @click="$router.push('/note/edit')">新建笔记</a-button>
        </div>
        <a-list :data-source="myNotes" :loading="loading">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta :title="item.title" :description="`${item.isPublic ? '公开' : '私有'} | 👁 ${item.viewCount} 👍 ${item.voteCount}`" />
              <template #actions>
                <a-button type="link" @click="$router.push(`/note/edit/${item.id}`)">编辑</a-button>
                <a-popconfirm title="确定删除？" @confirm="handleDelete(item.id)">
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
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { noteApi } from '../api'
import { message } from 'ant-design-vue'
export default defineComponent({
  setup() {
    const store = useStore()
    const user = computed(() => store.state.user)
    const publicNotes = ref<any[]>([])
    const myNotes = ref<any[]>([])
    const loading = ref(false)
    const loadPublic = async () => { loading.value = true; try { const res: any = await noteApi.publicList({ page: 1, size: 20 }); publicNotes.value = res.content.list } finally { loading.value = false } }
    const loadMy = async () => { if (!user.value.token) return; try { const res: any = await noteApi.myList({ page: 1, size: 20 }); myNotes.value = res.content.list } catch(e) {} }
    const handleDelete = async (id: number) => { await noteApi.delete(id); message.success('删除成功'); loadMy() }
    onMounted(() => { loadPublic(); loadMy() })
    return { user, publicNotes, myNotes, loading, handleDelete }
  }
})
</script>