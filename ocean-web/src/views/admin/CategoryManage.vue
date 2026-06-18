<template>
  <div>
    <div class="page-header">
      <h2>📂 分类管理</h2>
      <a-button type="primary" class="add-btn" @click="showModal()">+ 新增分类</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="treeData"
      row-key="id"
      :pagination="false"
      default-expand-all-rows
      class="ocean-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="showModal(record)">编辑</a-button>
          <a-popconfirm title="确定删除？如有子分类将无法删除" @confirm="handleDelete(record.id)">
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal
            :open="modalVisible"
      @update:open="(v) => modalVisible = v"
      :title="form.id ? '编辑分类' : '新增分类'"
      @ok="handleSave"
      class="ocean-modal"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="父分类">
          <a-tree-select
            v-model:value="form.parent"
            :tree-data="treeSelectData"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="顶级分类选 0"
            tree-default-expand-all
            allow-clear
          />
        </a-form-item>
        <a-form-item label="名称" :rules="[{ required: true, message: '请输入分类名称' }]">
          <a-input v-model:value="form.name" placeholder="分类名称" />
        </a-form-item>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { categoryApi } from '../../api'

export default defineComponent({
  setup() {
    const treeData = ref<any[]>([])
    const modalVisible = ref(false)
    const form = ref<any>({ parent: 0, sort: 0 })
    const columns = [
      { title: '名称', dataIndex: 'name', key: 'name' },
      { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
      { title: '操作', key: 'action', width: 140 }
    ]

    const treeSelectData = computed(() => [
      { id: 0, name: '顶级分类', children: treeData.value }
    ])

    const loadTree = async () => {
      const res: any = await categoryApi.tree()
      treeData.value = res.content
    }

    const showModal = (record?: any) => {
      form.value = record ? { ...record } : { parent: 0, sort: 0 }
      modalVisible.value = true
    }

    const handleSave = async () => {
      await categoryApi.save(form.value)
      message.success('保存成功')
      modalVisible.value = false
      loadTree()
    }

    const handleDelete = async (id: number) => {
      await categoryApi.delete(id)
      message.success('删除成功')
      loadTree()
    }

    onMounted(loadTree)

    return { treeData, treeSelectData, columns, modalVisible, form, showModal, handleSave, handleDelete }
  }
})
</script>

<style scoped>
.add-btn {
  border-radius: 10px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 500;
}
</style>