<template>
  <div style="padding: 24px 0; max-width: 900px; margin: 0 auto">
    <a-card :title="form.id ? '编辑笔记' : '新建笔记'">
      <a-form :model="form" layout="vertical">
        <a-form-item label="标题"><a-input v-model:value="form.title" /></a-form-item>
        <a-form-item label="内容"><a-textarea v-model:value="form.content" :rows="12" /></a-form-item>
        <a-form-item label="是否公开">
          <a-switch v-model:checked="isPublic" checked-children="公开" un-checked-children="私有" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSave">保存</a-button>
            <a-button @click="aiGenerate('note_generate')">AI 生成</a-button>
            <a-button @click="aiGenerate('note_polish')">AI 润色</a-button>
            <a-button @click="aiGenerate('note_summarize')">AI 总结</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { noteApi, aiApi } from '../api'
import { message } from 'ant-design-vue'
export default defineComponent({
  setup() {
    const route = useRoute()
    const router = useRouter()
    const form = ref<any>({ title: '', content: '', isPublic: 0 })
    const isPublic = computed({
      get: () => form.value.isPublic === 1,
      set: (val: boolean) => { form.value.isPublic = val ? 1 : 0 }
    })
    onMounted(async () => {
      const id = route.params.id
      if (id) { const res: any = await noteApi.detail(Number(id)); form.value = res.content }
    })
    const handleSave = async () => { await noteApi.save(form.value); message.success('保存成功'); router.push('/notes') }
    const aiGenerate = async (type: string) => {
      const selectedText = window.getSelection()?.toString() || ''
      const topic = form.value.title || ''
      const res: any = await aiApi.generate({ type, topic, selectedText: selectedText || form.value.content?.substring(0, 500) || '' })
      if (type === 'note_generate') { form.value.content = res.content.text }
      else { form.value.content = res.content.text }
      message.success('AI 生成完成')
    }
    return { form, isPublic, handleSave, aiGenerate }
  }
})
</script>