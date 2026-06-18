<template>
  <div class="page-container home-page" style="padding-bottom: 48px">
    <!-- 海洋英雄横幅 -->
    <div class="hero-section">
      <!-- Shader 动画背景 -->
      <ShaderAnimation />

      <!-- 气泡粒子 -->
      <div class="bubbles-container">
        <div class="bubble" v-for="n in 10" :key="n" :style="bubbleStyle(n)"></div>
      </div>

      <div class="hero-bg-decoration">
        <div class="hero-circle hero-circle-1"></div>
        <div class="hero-circle hero-circle-2"></div>
        <div class="hero-circle hero-circle-3"></div>
        <div class="hero-circle hero-circle-4"></div>
      </div>

      <div class="hero-content">
        <div class="hero-text">
          <div ref="heroBadgeRef" class="hero-badge">
            <span class="badge-dot"></span>
            广东海洋大学 · 知识探索平台
          </div>
          <h1 ref="heroTitleRef" class="hero-title">
            潜入深海<span class="title-wave">~</span>探索奥秘
          </h1>
          <p ref="heroSubRef" class="hero-subtitle">
            汇集海洋生物知识，带你领略蓝色星球的神奇世界 🌊
          </p>
          <div ref="heroSearchRef" class="hero-search">
            <div class="search-wrapper">
              <span class="search-icon">🔍</span>
              <input
                v-model="searchKeyword"
                class="search-input"
                placeholder="搜索鲸鱼、珊瑚礁、深海生物..."
                @keyup.enter="onSearch"
              />
              <button class="search-btn" @click="onSearch">
                开始探索 🚀
              </button>
            </div>
          </div>
          <div ref="heroTagsRef" class="hero-tags">
            <span
              class="hero-tag"
              v-for="(tag, idx) in hotTags"
              :key="tag"
              :class="`tag-color-${idx % 5}`"
              @click="searchKeyword = tag; onSearch()"
            >
              {{ tag }}
            </span>
          </div>
        </div>
        <div class="hero-visual">
          <div ref="emoji1Ref" class="hero-emoji emoji-whale">🐳</div>
          <div ref="emoji2Ref" class="hero-emoji emoji-fish">🐠</div>
          <div ref="emoji3Ref" class="hero-emoji emoji-octopus">🐙</div>
          <div ref="emoji4Ref" class="hero-emoji emoji-turtle">🐢</div>
          <div ref="emoji5Ref" class="hero-emoji emoji-coral">🪸</div>
          <div ref="emoji6Ref" class="hero-emoji emoji-shark">🦈</div>
        </div>
      </div>

      <div class="hero-wave">
        <svg viewBox="0 0 1440 100" preserveAspectRatio="none">
          <path d="M0,50 C240,90 480,10 720,50 C960,90 1200,10 1440,50 L1440,100 L0,100 Z" fill="white" opacity="0.5"/>
          <path d="M0,60 C360,100 720,20 1080,60 C1260,80 1350,60 1440,60 L1440,100 L0,100 Z" fill="white"/>
        </svg>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-card-wrapper" v-for="(stat, idx) in statCards" :key="idx">
        <div class="stat-card" :class="`stat-card-${idx}`" :style="{ '--stat-color': stat.color, '--stat-bg': stat.bg }">
          <div class="stat-emoji">{{ stat.icon }}</div>
          <div class="stat-value">{{ formatNumber(stat.animatedValue) }}</div>
          <div class="stat-label">{{ stat.label }}</div>
          <div class="stat-unit">{{ stat.unit }}</div>
          <div class="stat-trend" v-if="stat.trend !== undefined">
            <span class="trend-badge" :class="stat.trend >= 0 ? 'trend-up' : 'trend-down'">
              {{ stat.trend >= 0 ? '📈' : '📉' }} {{ Math.abs(stat.trend) }}%
            </span>
          </div>
          <div class="stat-decoration">{{ stat.deco }}</div>
        </div>
      </div>
    </div>

    <!-- 分类筛选 -->
    <div class="filter-bar">
      <span class="filter-emoji">🏷️</span>
      <span class="filter-label">分类筛选</span>
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
      <span class="filter-count">
        <span class="count-num">{{ total }}</span> 本电子书等你探索 📚
      </span>
    </div>

    <!-- 电子书列表 -->
    <div v-if="ebooks.length > 0" class="ebook-grid">
      <div v-for="(ebook, idx) in ebooks" :key="ebook.id" class="ebook-card" @click="$router.push(`/ebook/${ebook.id}`)">
        <div class="cover-wrapper">
          <img v-if="ebook.cover" :src="ebook.cover" :alt="ebook.name" />
          <div v-else class="cover-placeholder">
            <span class="placeholder-emoji">{{ coverEmojis[idx % coverEmojis.length] }}</span>
          </div>
          <div class="cover-overlay">
            <span class="read-now">🐠 开始阅读</span>
          </div>
        </div>
        <div class="ebook-info">
          <div class="ebook-category" v-if="ebook.category1Name">
            <span class="category-tag" :class="`cat-color-${idx % 4}`">{{ ebook.category1Name }}</span>
            <span v-if="ebook.category2Name" class="category-tag cat-secondary">{{ ebook.category2Name }}</span>
          </div>
          <div class="ebook-title">{{ ebook.name }}</div>
          <div class="ebook-desc">{{ ebook.description || '暂无描述' }}</div>
          <div class="ebook-stats">
            <span class="stat-item">📄 {{ ebook.docCount }} 篇</span>
            <span class="stat-item">👀 {{ ebook.viewCount }}</span>
            <span class="stat-item">👍 {{ ebook.voteCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-emoji">🐙</div>
      <div class="empty-text">海洋里还没有电子书呢~</div>
      <div class="empty-sub">快去后台添加第一本吧！</div>
    </div>

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
        <h3>📊 30 天数据趋势</h3>
        <span class="section-sub">看看大家最近在探索什么~</span>
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
import ShaderAnimation from '../components/ShaderAnimation.vue'

export default defineComponent({
  name: 'Home',
  components: { ShaderAnimation },
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
    const heroBadgeRef = ref<HTMLElement>()
    const emoji1Ref = ref<HTMLElement>()
    const emoji2Ref = ref<HTMLElement>()
    const emoji3Ref = ref<HTMLElement>()
    const emoji4Ref = ref<HTMLElement>()
    const emoji5Ref = ref<HTMLElement>()
    const emoji6Ref = ref<HTMLElement>()

    // 动画数值（用于数字滚动）
    const animatedValues = reactive([
      { value: 0, target: 0 },
      { value: 0, target: 0 },
      { value: 0, target: 0 },
      { value: 0, target: 0 }
    ])

    const coverEmojis = ['🐟', '🦐', '🦀', '🐡', '🦑', '🐚', '🦞', '🐬']

    const statCards = computed(() => [
      {
        icon: '📖', label: '总阅读量', unit: '次海底探索',
        value: statistic.value.totalViewCount || 0,
        animatedValue: animatedValues[0].value,
        color: '#1677ff', bg: 'linear-gradient(135deg, #e6f4ff, #bae0ff)',
        trend: statistic.value.viewIncreaseRate, deco: '🐋'
      },
      {
        icon: '💖', label: '总点赞量', unit: '次心动认可',
        value: statistic.value.totalVoteCount || 0,
        animatedValue: animatedValues[1].value,
        color: '#eb2f96', bg: 'linear-gradient(135deg, #fff0f6, #ffd6e7)',
        trend: statistic.value.voteIncreaseRate, deco: '🐠'
      },
      {
        icon: '🌊', label: '今日阅读', unit: '次今日潜水',
        value: statistic.value.todayViewCount || 0,
        animatedValue: animatedValues[2].value,
        color: '#52c41a', bg: 'linear-gradient(135deg, #f6ffed, #d9f7be)',
        deco: '🐢'
      },
      {
        icon: '🔥', label: '今日点赞', unit: '次今日热度',
        value: statistic.value.todayVoteCount || 0,
        animatedValue: animatedValues[3].value,
        color: '#fa8c16', bg: 'linear-gradient(135deg, #fff7e6, #ffe7ba)',
        deco: '🐙'
      }
    ])

    const hotTags = ['鲸鱼', '珊瑚礁', '深海', '海龟', '鲨鱼', '海豚']

    // 气泡样式生成
    const bubbleStyle = (n: number) => {
      const size = 8 + Math.random() * 20
      const left = 5 + (n - 1) * 9 + Math.random() * 5
      const delay = Math.random() * 8
      const duration = 6 + Math.random() * 6
      return {
        width: `${size}px`,
        height: `${size}px`,
        left: `${left}%`,
        animationDelay: `${delay}s`,
        animationDuration: `${duration}s`
      }
    }

    let timer: ReturnType<typeof setInterval> | null = null
    let isFirstLoad = true

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

    /** 统计卡片数字滚动 */
    const animateNumberRoll = () => {
      const targets = [0, 1, 2, 3]
      targets.forEach(i => {
        const targetVal = statCards.value[i].value
        if (targetVal === 0) return
        anime({
          targets: animatedValues[i],
          value: [0, targetVal],
          round: 1,
          duration: 1800,
          delay: i * 200,
          easing: 'easeOutElastic(1, 0.5)'
        })
      })
    }

    const setNumberDirect = () => {
      statCards.value.forEach((card, i) => {
        animatedValues[i].value = card.value
      })
    }

    /** 海洋生物 emoji 连续游动 */
    const startEmojiFloat = () => {
      const emojis = [emoji1Ref, emoji2Ref, emoji3Ref, emoji4Ref, emoji5Ref, emoji6Ref]
      const configs = [
        // 鲸鱼 - 大幅摇摆
        { translateX: [0, 40, -30, 0], translateY: [0, -20, 15, 0], rotate: [0, 10, -8, 0], duration: 9000 },
        // 小鱼 - 快速穿梭
        { translateX: [0, -50, 30, 0], translateY: [0, 25, -20, 0], rotate: [0, -15, 10, 0], duration: 7000 },
        // 章鱼 - 缓慢漂浮
        { translateX: [0, 20, -15, 0], translateY: [0, -25, 20, 0], scale: [1, 1.15, 0.9, 1], duration: 8000 },
        // 海龟 - 稳定游动
        { translateX: [0, -35, 25, 0], translateY: [0, 15, -10, 0], rotate: [0, 5, -5, 0], duration: 10000 },
        // 珊瑚 - 轻微摇曳
        { translateY: [0, -15, 10, 0], scale: [1, 1.08, 0.95, 1], opacity: [0.5, 0.8, 0.4, 0.5], duration: 7500 },
        // 鲨鱼 - 巡逻式
        { translateX: [0, 45, -35, 0], translateY: [0, -10, 8, 0], rotate: [0, 8, -6, 0], duration: 8500 }
      ]

      emojis.forEach((emojiRef, i) => {
        if (!emojiRef.value) return
        const cfg = configs[i]
        anime({
          targets: emojiRef.value,
          ...cfg,
          easing: 'easeInOutSine',
          loop: true
        })
      })
    }

    /** 电子书卡片交错弹入 */
    const animateCardStagger = () => {
      const cards = document.querySelectorAll('.ebook-card')
      if (!cards.length) return
      anime({
        targets: cards,
        opacity: [0, 1],
        translateY: [50, 0],
        scale: [0.9, 1],
        rotate: [3, 0],
        duration: 700,
        delay: anime.stagger(100, { start: 200 }),
        easing: 'easeOutBack(1.2)'
      })
    }

    /** 英雄区文字淡入 */
    const animateHeroText = () => {
      if (heroBadgeRef.value) {
        anime({
          targets: heroBadgeRef.value,
          opacity: [0, 1],
          translateX: [-20, 0],
          duration: 600,
          easing: 'easeOutCubic'
        })
      }
      if (heroTitleRef.value) {
        anime({
          targets: heroTitleRef.value,
          opacity: [0, 1],
          translateY: [40, 0],
          scale: [0.95, 1],
          duration: 900,
          delay: 150,
          easing: 'easeOutElastic(1, 0.6)'
        })
      }
      if (heroSubRef.value) {
        anime({
          targets: heroSubRef.value,
          opacity: [0, 1],
          translateY: [25, 0],
          duration: 700,
          delay: 300,
          easing: 'easeOutCubic'
        })
      }
      if (heroSearchRef.value) {
        anime({
          targets: heroSearchRef.value,
          opacity: [0, 1],
          translateY: [20, 0],
          scale: [0.98, 1],
          duration: 600,
          delay: 450,
          easing: 'easeOutCubic'
        })
      }
      if (heroTagsRef.value) {
        anime({
          targets: heroTagsRef.value.querySelectorAll('.hero-tag'),
          opacity: [0, 1],
          translateX: [-15, 0],
          scale: [0.8, 1],
          duration: 500,
          delay: anime.stagger(80, { start: 600 }),
          easing: 'easeOutBack(1.5)'
        })
      }
    }

    /** 统计卡片入场 */
    const animateStatCards = () => {
      const cards = document.querySelectorAll('.stat-card-wrapper')
      if (!cards.length) return
      anime({
        targets: cards,
        opacity: [0, 1],
        translateY: [30, 0],
        scale: [0.95, 1],
        duration: 600,
        delay: anime.stagger(120, { start: 500 }),
        easing: 'easeOutBack(1.3)'
      })
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
        if (isFirstLoad) {
          isFirstLoad = false
          animateNumberRoll()
        } else {
          setNumberDirect()
        }
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
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#1677ff',
          borderWidth: 2,
          borderRadius: 12,
          padding: [12, 16],
          textStyle: { fontSize: 13 }
        },
        legend: {
          data: ['📖 阅读增长', '💖 点赞增长'],
          top: 0,
          right: 0,
          textStyle: { fontSize: 13 }
        },
        grid: { left: 50, right: 30, top: 45, bottom: 30 },
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
            name: '📖 阅读增长',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: { width: 3, color: '#1677ff' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(22, 119, 255, 0.3)' },
                { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
              ])
            },
            itemStyle: { color: '#1677ff', borderColor: '#fff', borderWidth: 2 },
            data: trendList.map((t: any) => t.viewIncrease)
          },
          {
            name: '💖 点赞增长',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: { width: 3, color: '#eb2f96' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(235, 47, 150, 0.25)' },
                { offset: 1, color: 'rgba(235, 47, 150, 0.02)' }
              ])
            },
            itemStyle: { color: '#eb2f96', borderColor: '#fff', borderWidth: 2 },
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
      animateHeroText()
      animateStatCards()
      loadEbooks()
      loadCategories()
      loadStatistic()
      startEmojiFloat()
      timer = setInterval(loadStatistic, 10000)
    })

    onUnmounted(() => {
      if (chartInstance) chartInstance.dispose()
      if (timer) clearInterval(timer)
    })

    return {
      ebooks, total, page, size, searchKeyword, hotTags, coverEmojis,
      category1Id, category2Id, category1List, category2List,
      statistic, statCards, chartRef, bubbleStyle,
      heroTitleRef, heroSubRef, heroSearchRef, heroTagsRef, heroBadgeRef,
      emoji1Ref, emoji2Ref, emoji3Ref, emoji4Ref, emoji5Ref, emoji6Ref,
      formatNumber, loadEbooks, onSearch, onCategory1Change
    }
  }
})
</script>

<style scoped>
/* ====== 页面全局 ====== */
.home-page {
  position: relative;
}

/* ====== 英雄区 ====== */
.hero-section {
  position: relative;
  border-radius: 0 0 32px 32px;
  margin: 0 -24px 32px;
  padding: 56px 48px 0;
  overflow: hidden;
  min-height: 380px;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, rgba(10, 22, 40, 0.85) 0%, rgba(13, 59, 102, 0.7) 25%, rgba(13, 110, 110, 0.6) 50%, rgba(22, 119, 255, 0.7) 80%, rgba(64, 169, 255, 0.8) 100%);
  z-index: 1;
  pointer-events: none;
}

/* 气泡粒子 */
.bubbles-container {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 2;
}

.bubble {
  position: absolute;
  bottom: -20px;
  background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.4), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: bubbleRise linear infinite;
}

@keyframes bubbleRise {
  0% {
    transform: translateY(0) scale(1);
    opacity: 0;
  }
  10% {
    opacity: 0.6;
  }
  90% {
    opacity: 0.3;
  }
  100% {
    transform: translateY(-420px) scale(0.4);
    opacity: 0;
  }
}

.hero-bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 2;
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

.hero-circle-4 {
  width: 150px;
  height: 150px;
  background: radial-gradient(circle, #eb2f96 0%, transparent 70%);
  top: 60%;
  right: 20%;
  opacity: 0.05;
}

.hero-content {
  position: relative;
  z-index: 3;
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

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  margin-bottom: 20px;
  backdrop-filter: blur(8px);
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.3); }
}

.hero-title {
  font-size: 44px;
  font-weight: 900;
  color: #fff;
  margin: 0 0 12px;
  line-height: 1.2;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
  letter-spacing: -1px;
}

.title-wave {
  display: inline-block;
  color: #36cfc9;
  animation: waveText 2s ease-in-out infinite;
}

@keyframes waveText {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.hero-subtitle {
  font-size: 17px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 28px;
  line-height: 1.6;
}

.hero-search {
  margin-bottom: 18px;
}

.search-wrapper {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 28px;
  padding: 4px 4px 4px 18px;
  max-width: 520px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.search-wrapper:focus-within {
  box-shadow: 0 8px 40px rgba(22, 119, 255, 0.3);
  transform: translateY(-2px);
}

.search-icon {
  font-size: 18px;
  margin-right: 10px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  background: transparent;
  color: #1a1a2e;
  padding: 10px 0;
  min-width: 0;
}

.search-input::placeholder {
  color: #999;
}

.search-btn {
  flex-shrink: 0;
  padding: 10px 22px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #1677ff, #36cfc9);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.search-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(22, 119, 255, 0.4);
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.hero-tag {
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.tag-color-0 { background: rgba(22, 119, 255, 0.2); color: #91caff; border: 1px solid rgba(22, 119, 255, 0.3); }
.tag-color-1 { background: rgba(54, 207, 201, 0.2); color: #87e8de; border: 1px solid rgba(54, 207, 201, 0.3); }
.tag-color-2 { background: rgba(235, 47, 150, 0.15); color: #ff85c0; border: 1px solid rgba(235, 47, 150, 0.25); }
.tag-color-3 { background: rgba(250, 140, 22, 0.15); color: #ffc069; border: 1px solid rgba(250, 140, 22, 0.25); }
.tag-color-4 { background: rgba(82, 196, 26, 0.15); color: #95de64; border: 1px solid rgba(82, 196, 26, 0.25); }

.hero-tag:hover {
  transform: translateY(-2px) scale(1.05);
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

/* 海洋生物 emoji */
.hero-visual {
  position: relative;
  width: 260px;
  height: 260px;
  flex-shrink: 0;
}

.hero-emoji {
  position: absolute;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2));
}

.emoji-whale {
  font-size: 80px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.emoji-fish {
  font-size: 45px;
  top: 5%;
  left: 5%;
  opacity: 0.7;
}

.emoji-octopus {
  font-size: 50px;
  top: 10%;
  right: 5%;
  opacity: 0.65;
}

.emoji-turtle {
  font-size: 40px;
  bottom: 10%;
  left: 5%;
  opacity: 0.6;
}

.emoji-coral {
  font-size: 35px;
  bottom: 5%;
  right: 10%;
  opacity: 0.5;
}

.emoji-shark {
  font-size: 42px;
  top: 55%;
  right: 0%;
  opacity: 0.55;
}

.hero-wave {
  position: relative;
  z-index: 3;
  margin-top: 24px;
  height: 100px;
}

.hero-wave svg {
  width: 100%;
  height: 100px;
}

/* ====== 统计卡片 ====== */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card-wrapper {
  opacity: 0;
}

.stat-card {
  border-radius: 20px;
  padding: 22px 20px;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
  background: var(--stat-bg);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.stat-card:hover {
  transform: translateY(-6px) rotate(-1deg);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1);
}

.stat-card-0:hover { transform: translateY(-6px) rotate(-1deg); }
.stat-card-1:hover { transform: translateY(-6px) rotate(1deg); }
.stat-card-2:hover { transform: translateY(-6px) rotate(-0.5deg); }
.stat-card-3:hover { transform: translateY(-6px) rotate(0.5deg); }

.stat-emoji {
  font-size: 36px;
  margin-bottom: 10px;
  display: inline-block;
  animation: gentleBounce 3s ease-in-out infinite;
}

@keyframes gentleBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--stat-color);
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #5a6a7a;
  margin-top: 2px;
  font-weight: 500;
}

.stat-unit {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.stat-trend {
  margin-top: 8px;
}

.trend-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  font-weight: 600;
}

.trend-up {
  background: rgba(82, 196, 26, 0.12);
  color: #52c41a;
}

.trend-down {
  background: rgba(255, 77, 79, 0.12);
  color: #ff4d4f;
}

.stat-decoration {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 40px;
  opacity: 0.08;
  pointer-events: none;
}

/* ====== 筛选栏 ====== */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #e6f4ff 0%, #f0f9ff 50%, #e6fffb 100%);
  border-radius: 20px;
  border: 1px solid rgba(22, 119, 255, 0.08);
}

.filter-emoji {
  font-size: 20px;
}

.filter-label {
  font-size: 14px;
  color: #1677ff;
  font-weight: 600;
  white-space: nowrap;
}

.filter-select {
  width: 150px;
}

.filter-select :deep(.ant-select-selector) {
  border-radius: 12px !important;
  border-color: rgba(22, 119, 255, 0.15) !important;
}

.filter-count {
  margin-left: auto;
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.count-num {
  font-weight: 700;
  color: #1677ff;
  font-size: 16px;
}

/* ====== 电子书网格 ====== */
.ebook-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18px;
  margin-bottom: 32px;
}

.ebook-card {
  border-radius: 20px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  background: white;
  border: 1px solid #e8ecf0;
  cursor: pointer;
  opacity: 0;
}

.ebook-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 48px rgba(22, 119, 255, 0.18);
  border-color: #91caff;
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
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.ebook-card:hover .cover-wrapper img {
  transform: scale(1.1);
}

.cover-placeholder {
  height: 100%;
  background: linear-gradient(135deg, #e6f4ff 0%, #d6eaf8 50%, #b7e3fa 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-emoji {
  font-size: 64px;
  animation: gentleBounce 3s ease-in-out infinite;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(22, 119, 255, 0.1) 0%, rgba(22, 119, 255, 0.5) 100%);
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
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  color: #1677ff;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
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
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.cat-color-0 { background: #e6f4ff; color: #1677ff; }
.cat-color-1 { background: #f0f9ff; color: #36cfc9; }
.cat-color-2 { background: #fff0f6; color: #eb2f96; }
.cat-color-3 { background: #fff7e6; color: #fa8c16; }
.cat-secondary { background: #f5f5f5; color: #666; }

.ebook-title {
  font-size: 15px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ebook-desc {
  font-size: 13px;
  color: #888;
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ebook-stats {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #999;
  padding-top: 12px;
  border-top: 1px dashed #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ====== 空状态 ====== */
.empty-state {
  text-align: center;
  padding: 80px 0;
}

.empty-emoji {
  font-size: 80px;
  animation: gentleBounce 3s ease-in-out infinite;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 18px;
  color: #1a1a2e;
  font-weight: 600;
  margin-bottom: 8px;
}

.empty-sub {
  font-size: 14px;
  color: #999;
}

/* ====== 分页 ====== */
.pagination-wrapper {
  text-align: center;
  margin: 32px 0;
}

.pagination-wrapper :deep(.ant-pagination-item) {
  border-radius: 12px;
}

.pagination-wrapper :deep(.ant-pagination-item-active) {
  background: linear-gradient(135deg, #1677ff, #4096ff);
  border-color: transparent;
}

.pagination-wrapper :deep(.ant-pagination-item-active a) {
  color: white;
}

/* ====== 趋势图 ====== */
.trend-section {
  margin-top: 36px;
}

.section-header {
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.section-header h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.section-sub {
  font-size: 13px;
  color: #999;
}

.trend-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.chart-container {
  height: 320px;
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .ebook-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .hero-section {
    padding: 40px 20px 0;
  }
  .hero-content {
    flex-direction: column;
    text-align: center;
  }
  .hero-title {
    font-size: 32px;
  }
  .hero-visual {
    width: 200px;
    height: 200px;
    margin-top: 20px;
  }
  .hero-tags {
    justify-content: center;
  }
  .search-wrapper {
    max-width: 100%;
  }
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .ebook-grid {
    grid-template-columns: 1fr;
  }
  .filter-bar {
    flex-wrap: wrap;
  }
}
</style>
