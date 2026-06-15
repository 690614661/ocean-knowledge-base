<template>
  <div class="home">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin: 24px 0">
      <a-col :span="6">
        <a-card>
          <a-statistic title="总阅读量" :value="statistic.totalViewCount || 0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="总点赞量" :value="statistic.totalVoteCount || 0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日阅读" :value="statistic.todayViewCount || 0" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日点赞" :value="statistic.todayVoteCount || 0" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <div style="margin-bottom: 24px; display: flex; gap: 12px">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索文档内容..."
        enter-button="搜索"
        style="max-width: 400px"
        @search="onSearch"
      />
    </div>

    <!-- 分类筛选 -->
    <div style="margin-bottom: 24px">
      <a-select v-model:value="category1Id" placeholder="一级分类" style="width: 160px; margin-right: 12px" allowClear @change="onCategory1Change">
        <a-select-option v-for="c in category1List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
      </a-select>
      <a-select v-model:value="category2Id" placeholder="二级分类" style="width: 160px" allowClear>
        <a-select-option v-for="c in category2List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
      </a-select>
    </div>

    <!-- 电子书列表 -->
    <a-row :gutter="[16, 16]">
      <a-col v-for="ebook in ebooks" :key="ebook.id" :span="6">
        <a-card hoverable @click="$router.push(`/ebook/${ebook.id}`)">
          <template #cover>
            <img v-if="ebook.cover" :src="ebook.cover" style="height: 200px; object-fit: cover" />
            <div v-else style="height: 200px; background: #e8f4f8; display: flex; align-items: center; justify-content: center; font-size: 48px">🐠</div>
          </template>
          <a-card-meta :title="ebook.name" :description="ebook.description || '暂无描述'" />
          <div style="margin-top: 8px; color: #999; font-size: 12px">
            <span>📄 {{ ebook.docCount }} 篇</span>
            <span style="margin-left: 16px">👁 {{ ebook.viewCount }}</span>
            <span style="margin-left: 16px">👍 {{ ebook.voteCount }}</span>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 分页 -->
    <div style="margin-top: 24px; text-align: center">
      <a-pagination v-model:current="page" :total="total" :pageSize="size" @change="loadEbooks" />
    </div>

    <!-- 趋势图 -->
    <a-card title="30天趋势" style="margin-top: 24px">
      <div ref="chartRef" style="height: 300px"></div>
    </a-card>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ebookApi, categoryApi, snapshotApi } from '../api'
import * as echarts from 'echarts'

export default defineComponent({
  name: 'Home',
  setup() {
    const router = useRouter()
    const ebooks = ref<any[]>([])
    const total = ref(0)
    const page = ref(1)
    const size = ref(8)
    const searchKeyword = ref('')
    const category1Id = ref<number | undefined>(undefined)
    const category2Id = ref<number | undefined>(undefined)
    const categories = ref<any[]>([])
    const statistic = ref<any>({})
    const chartRef = ref<HTMLElement>()

    const category1List = computed(() => categories.value.filter(c => c.parent === 0))
    const category2List = computed(() => {
      if (!category1Id.value) return []
      return categories.value.filter(c => c.parent === category1Id.value)
    })

    const loadEbooks = async () => {
      const res: any = await ebookApi.list({
        page: page.value,
        size: size.value,
        name: searchKeyword.value || undefined,
        category1Id: category1Id.value,
        category2Id: category2Id.value
      })
      ebooks.value = res.content.list
      total.value = res.content.total
    }

    const loadCategories = async () => {
      const res: any = await categoryApi.all()
      categories.value = res.content
    }

    const loadStatistic = async () => {
      try {
        const res: any = await snapshotApi.getStatistic()
        statistic.value = res.content
        initChart(res.content.trendList || [])
      } catch (e) {
        // 统计数据加载失败不影响主流程
      }
    }

    const initChart = (trendList: any[]) => {
      if (!chartRef.value) return
      const chart = echarts.init(chartRef.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['阅读增长', '点赞增长'] },
        xAxis: { type: 'category', data: trendList.map((t: any) => t.date) },
        yAxis: { type: 'value' },
        series: [
          { name: '阅读增长', type: 'line', data: trendList.map((t: any) => t.viewIncrease), smooth: true },
          { name: '点赞增长', type: 'line', data: trendList.map((t: any) => t.voteIncrease), smooth: true }
        ]
      })
    }

    const onSearch = () => {
      if (searchKeyword.value) {
        router.push(`/search?keyword=${searchKeyword.value}`)
      }
    }

    const onCategory1Change = () => {
      category2Id.value = undefined
      loadEbooks()
    }

    onMounted(() => {
      loadEbooks()
      loadCategories()
      loadStatistic()
    })

    return { ebooks, total, page, size, searchKeyword, category1Id, category2Id, category1List, category2List, statistic, chartRef, loadEbooks, onSearch, onCategory1Change }
  }
})
</script>