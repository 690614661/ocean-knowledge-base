<template>
  <div>
    <h2>管理仪表盘</h2>
    <a-row :gutter="16">
      <a-col :span="8"><a-card><a-statistic title="总阅读量" :value="statistic.totalViewCount || 0" /></a-card></a-col>
      <a-col :span="8"><a-card><a-statistic title="总点赞量" :value="statistic.totalVoteCount || 0" /></a-card></a-col>
      <a-col :span="8"><a-card><a-statistic title="点赞率" :value="statistic.voteRate || 0" suffix="%" /></a-card></a-col>
    </a-row>
  </div>
</template>
<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import { snapshotApi } from '../../api'
export default defineComponent({
  setup() {
    const statistic = ref<any>({})
    onMounted(async () => {
      try { const res: any = await snapshotApi.getStatistic(); statistic.value = res.content } catch(e) {}
    })
    return { statistic }
  }
})
</script>