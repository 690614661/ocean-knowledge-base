<template>
  <div class="page-container" style="padding-bottom: 48px">
    <!-- 海洋英雄横幅 -->
    <div class="hero-section">
      <div class="hero-bg-decoration">
        <div class="hero-circle hero-circle-1"></div>
        <div class="hero-circle hero-circle-2"></div>
        <div class="hero-circle hero-circle-3"></div>
      </div>
      <div class="hero-content">
        <div class="hero-text">
          <h1 ref="heroTitleRef" class="hero-title">探索海洋奥秘</h1>
          <p ref="heroSubRef" class="hero-subtitle">汇集海洋生物知识，带你领略蓝色星球的神奇世界</p>
          <div ref="heroSearchRef" class="hero-search">
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索海洋生物、生态、分类..."
              enter-button="🔍 探索"
              size="large"
              class="hero-search-input"
              @search="onSearch"
            />
          </div>
          <div ref="heroTagsRef" class="hero-tags">
            <span class="hero-tag" v-for="tag in hotTags" :key="tag" @click="searchKeyword = tag; onSearch()">
              {{ tag }}
            </span>
          </div>
        </div>
        <div class="hero-visual">
          <div ref="emoji1Ref" class="hero-emoji">🐋</div>
          <div ref="emoji2Ref" class="hero-emoji hero-emoji-2">🐠</div>
          <div ref="emoji3Ref" class="hero-emoji hero-emoji-3">🪸</div>
        </div>
      </div>
      <div class="hero-wave">
        <svg viewBox="0 0 1440 80" preserveAspectRatio="none">
          <path d="M0,40 C360,80 720,0 1080,40 C1260,60 1350,40 1440,40 L1440,80 L0,80 Z" fill="white"/>
        </svg>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card-wrapper" v-for="(stat, idx) in statCards" :key="idx">
        <div class="stat-card" :style="{ '--stat-color': stat.color }">
          <div class="stat-icon" :style="{ background: stat.bg }">{{ stat.icon }}</div>
          <div class="stat-value">{{ formatNumber(stat.animatedValue) }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-trend" v-if="stat.trend !== undefined">
            <span :style="{ color: stat.trend >= 0 ? '#52c41a' : '#ff4d4f' }">
              {{ stat.trend >= 0 ? '↑' : '↓' }} {{ Math.abs(stat.trend) }}%
            </span>
            <span style="color: #999; margin-left: 4px">较昨日</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <div class="filter-label">分类筛选：</div>
      <a-select
        v-model:value="category1Id"
        placeholder="全部一级分类"
        class="filter-select"
        allowClear
        @change="onCategory1Change"
      >
        <a-select-option v-for="c in category1List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
      </a-select>
      <a-select
        v-model:value="category2Id"
        placeholder="全部二级分类"
        class="filter-select"
        allowClear
      >
        <a-select-option v-for="c in category2List" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
      </a-select>
      <span class="filter-count">共 {{ total }} 本电子书</span>
    </div>

    <!-- 电子书列表 -->
    <div v-if="ebooks.length > 0" class="ebook-grid">
      <div v-for="ebook in ebooks" :key="ebook.id" class="ebook-card" @click="$router.push(`/ebook/${ebook.id}`)">
        <div class="cover-wrapper">
          <img v-if="ebook.cover" :src="ebook.cover" :alt="ebook.name" />
          <div v-else class="cover-placeholder">
            <span>🐟</span>
          </div>
          <div class="cover-overlay">
            <span class="read-now">开始阅读 →</span>
          </div>
        </div>
        <div class="ebook-info">
          <div class="ebook-category" v-if="ebook.category1Name">
            <span class="category-tag">{{ ebook.category1Name }}</span>
            <span v-if="ebook.category2Name" class="category-tag category-tag-2">{{ ebook.category2Name }}</span>
          </div>
          <div class="ebook-title">{{ ebook.name }}</div>
          <div class="ebook-desc">{{ ebook.description || '暂无描述' }}</div>
          <div class="ebook-stats">
            <span>📄 {{ ebook.docCount }} 篇文档</span>
            <span>👁 {{ ebook.viewCount }} 阅读</span>
            <span>👍 {{ ebook.voteCount }} 点赞</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <a-empty v-else style="margin: 80px 0" description="暂无电子书">
      <template #image>
        <div style="font-size: 64px">🐙</div>
      </template>
    </a-empty>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > size">
      <a-pagination
        v-model:current="page"
        :total="total"
        :pageSize="size"
        :showTotal="(t) => `共 ${t} 本电子书`"
        @change="loadEbooks"
      />
    </div>

    <!-- 趋势图 -->
    <div class="trend-section" v-if="statistic.trendList?.length">
      <div class="section-header">
        <h3>📈 30 天数据趋势</h3>
      </div>
      <div class="trend-card">
        <div ref="chartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ebookApi, categoryApi, snapshotApi } from '../api'
import * as echarts from 'echarts'
import anime from 'animejs/lib/anime.es.js'

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
    let chartInstance: any = null

    // anime.js 引用
    const heroTitleRef = ref<HTMLElement>()
    const heroSubRef = ref<HTMLElement>()
    const heroSearchRef = ref<HTMLElement>()
    const heroTagsRef = ref<HTMLElement>()
    const emoji1Ref = ref<HTMLElement>()
    const emoji2Ref = ref<HTMLElement>()
    const emoji3Ref = ref<HTMLElement>()

    // 动画数值（用于数字滚动）
    const animatedValues = reactive([
      { value: 0, target: 0 },
      { value: 0, target: 0 },
      { value: 0, target: 0 },
      { value: 0, target: 0 }
    ])

    const statCards = computed(() => [
      { icon: '📖', label: '总阅读量', value: statistic.value.totalViewCount || 0, animatedValue: animatedValues[0].value, color: '#1677ff', bg: '#e6f4ff', trend: statistic.value.viewIncreaseRate },
      { icon: '👍', label: '总点赞量', value: statistic.value.totalVoteCount || 0, animatedValue: animatedValues[1].value, color: '#eb2f96', bg: '#fff0f6', trend: statistic.value.voteIncreaseRate },
      { icon: '📊', label: '今日阅读', value: statistic.value.todayViewCount || 0, animatedValue: animatedValues[2].value, color: '#52c41a', bg: '#f6ffed' },
      { icon: '🔥', label: '今日点赞', value: statistic.value.todayVoteCount || 0, animatedValue: animatedValues[3].value, color: '#fa8c16', bg: '#fff7e6' }
    ])

    const hotTags = ['鲸鱼', '珊瑚礁', '深海', '海龟', '鲨鱼', '海豚']
    let timer: ReturnType<typeof setInterval> | null = null

    const category1List = computed(() => categories.value.filter((c: any) => c.parent === 0))
    const category2List = computed(() => {
      if (!category1Id.value) return []
      return categories.value.filter((c: any) => c.parent === category1Id.value)
    })

    const formatNumber = (n: number) => {
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toLocaleString()
    }

    // ====== anime.js 动画 ======

    /** ① 统计卡片数字滚动 */
    const animateNumberRoll = () => {
      const targets = [0, 1, 2, 3] // 四张卡
      targets.forEach(i => {
        const targetVal = statCards.value[i].value
        if (targetVal === 0) return
        anime({
          targets: animatedValues[i],
          value: [0, targetVal],
          round: 1,
          duration: 1500,
          delay: i * 150,
          easing: 'easeOutCubic'
        })
      })
    }

    /** ② 英雄区 emoji 连续游动 */
    const startEmojiFloat = () => {
      if (!emoji1Ref.value || !emoji2Ref.value || !emoji3Ref.value) return

      // 鲸鱼 — 左右摇摆 + 上下浮动
      anime({
        targets: emoji1Ref.value,
        translateX: [
          { value: [0, 30], duration: 3000, easing: 'easeInOutSine' },
          { value: [30, -20], duration: 4000, easing: 'easeInOutSine' },
          { value: [-20, 0], duration: 3000, easing: 'easeInOutSine' }
        ],
        translateY: [
          { value: [0, -15], duration: 2500, easing: 'easeInOutQuad' },
          { value: [-15, 10], duration: 3500, easing: 'easeInOutQuad' },
          { value: [10, 0], duration: 2500, easing: 'easeInOutQuad' }
        ],
        rotate: [
          { value: [0, 8], duration: 2000, easing: 'easeInOutSine' },
          { value: [8, -6], duration: 3000, easing: 'easeInOutSine' },
          { value: [-6, 0], duration: 2000, easing: 'easeInOutSine' }
        ],
        loop: true
      })

      // 小鱼 — 围绕游动
      anime({
        targets: emoji2Ref.value,
        translateX: [
          { value: [0, -40], duration: 3500, easing: 'easeInOutSine' },
          { value: [-40, 25], duration: 4500, easing: 'easeInOutSine' },
          { value: [25, 0], duration: 3500, easing: 'easeInOutSine' }
        ],
        translateY: [
          { value: [0, 20], duration: 3000, easing: 'easeInOutQuad' },
          { value: [20, -15], duration: 4000, easing: 'easeInOutQuad' },
          { value: [-15, 0], duration: 3000, easing: 'easeInOutQuad' }
        ],
        loop: true
      })

      // 水母 — 缓慢上下漂浮
      anime({
        targets: emoji3Ref.value,
        translateY: [
          { value: [0, -20], duration: 2800, easing: 'easeInOutQuad' },
          { value: [-20, 25], duration: 3800, easing: 'easeInOutQuad' },
          { value: [25, 0], duration: 2800, easing: 'easeInOutQuad' }
        ],
        scale: [
          { value: [1, 1.1], duration: 2000, easing: 'easeInOutQuad' },
          { value: [1.1, 0.95], duration: 3000, easing: 'easeInOutQuad' },
          { value: [0.95, 1], duration: 2000, easing: 'easeInOutQuad' }
        ],
        opacity: [
          { value: [0.5, 0.8], duration: 2500, easing: 'easeInOutSine' },
          { value: [0.8, 0.4], duration: 3500, easing: 'easeInOutSine' },
          { value: [0.4, 0.5], duration: 2500, easing: 'easeInOutSine' }
        ],
        loop: true
      })
    }

    /** ③ 电子书卡片交错弹入 */
    const animateCardStagger = () => {
      const cards = document.querySelectorAll('.ebook-card')
      if (!cards.length) return
      anime({
        targets: cards,
        opacity: [0, 1],
        translateY: [40, 0],
        scale: [0.95, 1],
        duration: 600,
        delay: anime.stagger(80),
        easing: 'easeOutElastic(1, 0.6)'
      })
    }

    /** ④ 英雄区文字淡入 */
    const animateHeroText = () => {
      if (heroTitleRef.value) {
        anime({
          targets: heroTitleRef.value,
          opacity: [0, 1],
          translateY: [30, 0],
          duration: 800,
          easing: 'easeOutCubic'
        })
      }
      if (heroSubRef.value) {
        anime({
          targets: heroSubRef.value,
          opacity: [0, 1],
          translateY: [20, 0],
          duration: 800,
          delay: 200,
          easing: 'easeOutCubic'
        })
      }
      if (heroSearchRef.value) {
        anime({
          targets: heroSearchRef.value,
          opacity: [0, 1],
          translateY: [15, 0],
          duration: 600,
          delay: 400,
          easing: 'easeOutCubic'
        })
      }
      if (heroTagsRef.value) {
        anime({
          targets: heroTagsRef.value.querySelectorAll('.hero-tag'),
          opacity: [0, 1],
          translateX: [-10, 0],
          duration: 500,
          delay: anime.stagger(80, { start: 600 }),
          easing: 'easeOutCubic'
        })
      }
    }

    // ====== 数据加载 ======

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
      // 数据加载后触发卡片动画
      if (res.content.list?.length) {
        nextTick(() => animateCardStagger())
      }
    }

    const loadCategories = async () => {
      try {
        const res: any = await categoryApi.all()
        categories.value = res.content
      } catch {}
    }

    const loadStatistic = async () => {
      try {
        const res: any = await snapshotApi.getStatistic()
        statistic.value = res.content
        // 统计更新后触发数字滚动
        animateNumberRoll()
      } catch {}
    }

    // ====== ECharts ======

    const initChart = (trendList: any[]) => {
      if (!chartRef.value || !trendList.length) return
      if (chartInstance) chartInstance.dispose()
      chartInstance = echarts.init(chartRef.value)
      chartInstance.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.9)',
          borderColor: '#1677ff',
          borderWidth: 2,
          borderRadius: 8,
          padding: [12, 16]
        },
        legend: {
          data: ['阅读增长', '点赞增长'],
          top: 0,
          right: 0,
          textStyle: { fontSize: 13 }
        },
        grid: { left: 50, right: 30, top: 40, bottom: 30 },
        xAxis: {
          type: 'category',
          data: trendList.map((t: any) => t.date?.slice(5)),
          boundaryGap: false,
          axisLine: { lineStyle: { color: '#e8ecf0' } },
          axisLabel: { color: '#999', fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
          axisLabel: { color: '#999', fontSize: 11 }
        },
        series: [
          {
            name: '阅读增长',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 3, color: '#1677ff' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(22, 119, 255, 0.25)' },
                { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
              ])
            },
            itemStyle: { color: '#1677ff' },
            data: trendList.map((t: any) => t.viewIncrease)
          },
          {
            name: '点赞增长',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            lineStyle: { width: 3, color: '#eb2f96' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(235, 47, 150, 0.2)' },
                { offset: 1, color: 'rgba(235, 47, 150, 0.02)' }
              ])
            },
            itemStyle: { color: '#eb2f96' },
            data: trendList.map((t: any) => t.voteIncrease)
          }
        ]
      })
    }

    const onSearch = () => {
      if (searchKeyword.value) {
        router.push(`/search?keyword=${encodeURIComponent(searchKeyword.value)}`)
      }
    }

    const onCategory1Change = () => {
      category2Id.value = undefined
      page.value = 1
      loadEbooks()
    }

    // ====== Watchers ======

    watch(category2Id, () => {
      page.value = 1
      loadEbooks()
    })

    watch(() => statistic.value.trendList, (trendList: any) => {
      if (trendList?.length) {
        setTimeout(() => initChart(trendList), 100)
      }
    }, { deep: true })

    // ====== 生命周期 ======

    onMounted(() => {
      // 页面入场动画
      animateHeroText()

      loadEbooks()
      loadCategories()
      loadStatistic()

      // emoji 连续游动
      startEmojiFloat()

      // 轮询
      timer = setInterval(loadStatistic, 10000)
    })

    onUnmounted(() => {
      if (chartInstance) chartInstance.dispose()
      if (timer) clearInterval(timer)
    })

    return {
      ebooks, total, page, size, searchKeyword, hotTags,
      category1Id, category2Id, category1List, category2List,
      statistic, statCards, chartRef,
      heroTitleRef, heroSubRef, heroSearchRef, heroTagsRef,
      emoji1Ref, emoji2Ref, emoji3Ref,
      formatNumber, loadEbooks, onSearch, onCategory1Change
    }
  }
})
</script>

<style scoped>
/* 英雄区 */
.hero-section {
  position: relative;
  background: linear-gradient(135deg, #0a1628 0%, #1a3a5c 30%, #0d4f6b 60%, #1677ff 100%);
  border-radius: 0 0 32px 32px;
  margin: 0 -24px 32px;
  padding: 60px 48px 0;
  overflow: hidden;
  min-height: 360px;
}

.hero-bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.hero-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.08;
}

.hero-circle-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, #69b1ff 0%, transparent 70%);
  top: -150px;
  right: -100px;
}

.hero-circle-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #36cfc9 0%, transparent 70%);
  bottom: -50px;
  left: 10%;
}

.hero-circle-3 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, #1677ff 0%, transparent 70%);
  top: 20%;
  left: 40%;
}

.hero-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1100px;
  margin: 0 auto;
}

.hero-text {
  flex: 1;
  max-width: 600px;
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 12px;
  line-height: 1.2;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 28px;
  line-height: 1.6;
}

.hero-search {
  margin-bottom: 16px;
}

.hero-search-input {
  max-width: 500px;
}

.hero-search-input :deep(.ant-input) {
  border-radius: 12px 0 0 12px !important;
  height: 50px;
  font-size: 15px;
  padding: 0 20px;
  border: 2px solid transparent;
  background: rgba(255, 255, 255, 0.95);
}

.hero-search-input :deep(.ant-btn) {
  border-radius: 0 12px 12px 0 !important;
  height: 50px;
  padding: 0 24px;
  font-size: 15px;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border: none;
  font-weight: 600;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-tag {
  padding: 4px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.hero-tag:hover {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  transform: translateY(-1px);
}

.hero-visual {
  position: relative;
  width: 220px;
  height: 220px;
  flex-shrink: 0;
}

.hero-emoji {
  font-size: 80px;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2));
}

.hero-emoji-2 {
  font-size: 50px;
  top: 10%;
  left: 10%;
  opacity: 0.6;
}

.hero-emoji-3 {
  font-size: 40px;
  top: 70%;
  left: 70%;
  opacity: 0.5;
}

.hero-wave {
  position: relative;
  z-index: 2;
  margin-top: 32px;
  height: 80px;
}

.hero-wave svg {
  width: 100%;
  height: 80px;
}

/* 统计卡片 */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-card-wrapper {
  --stat-color: #1677ff;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--stat-color);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.12);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-bottom: 12px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #5a6a7a;
  margin-top: 4px;
}

.stat-trend {
  font-size: 12px;
  margin-top: 8px;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-label {
  font-size: 14px;
  color: #666;
  white-space: nowrap;
}

.filter-select {
  width: 160px;
}

.filter-count {
  margin-left: auto;
  font-size: 13px;
  color: #999;
}

/* 电子书网格 */
.ebook-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 32px;
}

.ebook-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: white;
  border: 1px solid #e8ecf0;
  cursor: pointer;
}

.ebook-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(22, 119, 255, 0.15);
  border-color: #4096ff;
}

.cover-wrapper {
  height: 200px;
  overflow: hidden;
  position: relative;
}

.cover-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}

.ebook-card:hover .cover-wrapper img {
  transform: scale(1.08);
}

.cover-placeholder {
  height: 100%;
  background: linear-gradient(135deg, #e8f4fd 0%, #d6eaf8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 64px;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.ebook-card:hover .cover-overlay {
  opacity: 1;
}

.read-now {
  padding: 8px 24px;
  border: 2px solid white;
  border-radius: 24px;
  color: white;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.read-now:hover {
  background: white;
  color: #1677ff;
}

.ebook-info {
  padding: 16px;
}

.ebook-category {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
}

.category-tag {
  padding: 2px 10px;
  border-radius: 4px;
  background: #e6f4ff;
  color: #1677ff;
  font-size: 11px;
  font-weight: 500;
}

.category-tag-2 {
  background: #f0f5ff;
  color: #2f54eb;
}

.ebook-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ebook-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ebook-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #999;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

/* 分页 */
.pagination-wrapper {
  text-align: center;
  margin: 32px 0;
}

/* 趋势图 */
.trend-section {
  margin-top: 32px;
}

.section-header {
  margin-bottom: 16px;
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.trend-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.chart-container {
  height: 320px;
}
</style>