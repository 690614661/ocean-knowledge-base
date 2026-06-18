<template>
  <div class="dashboard-page">
    <div class="page-header">
      <h2>📊 管理仪表盘</h2>
    </div>

    <div class="dashboard-grid">
      <div class="dash-card" v-for="(item, idx) in dashCards" :key="idx" :style="{ '--card-color': item.color }">
        <div class="dash-card-icon" :style="{ background: item.bg }">{{ item.icon }}</div>
        <div class="dash-card-body">
          <div class="dash-card-value">{{ formatNumber(item.animated.value) }}</div>
          <div class="dash-card-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <div class="dashboard-tips">
      <a-alert
        message="管理提示"
        description="从左侧菜单选择管理模块进行内容维护。新增用户后请及时告知其登录信息。"
        type="info"
        show-icon
      />
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted, nextTick } from 'vue'
import { snapshotApi } from '../../api'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'AdminDashboard',
  setup() {
    const statistic = ref<any>({})

    const animated = ref([
      { value: 0 },
      { value: 0 },
      { value: 0 },
      { value: 0 }
    ])

    const dashCards = ref([
      { icon: '📖', label: '总阅读量', value: 0, animated: animated.value[0], color: '#1677ff', bg: '#e6f4ff' },
      { icon: '👍', label: '总点赞量', value: 0, animated: animated.value[1], color: '#eb2f96', bg: '#fff0f6' },
      { icon: '📈', label: '今日阅读', value: 0, animated: animated.value[2], color: '#52c41a', bg: '#f6ffed' },
      { icon: '🔥', label: '今日点赞', value: 0, animated: animated.value[3], color: '#fa8c16', bg: '#fff7e6' }
    ])

    onMounted(async () => {
      try {
        const res: any = await snapshotApi.getStatistic()
        statistic.value = res.content
        const vals = [
          res.content.totalViewCount || 0,
          res.content.totalVoteCount || 0,
          res.content.todayViewCount || 0,
          res.content.todayVoteCount || 0
        ]

        dashCards.value = [
          { icon: '📖', label: '总阅读量', value: vals[0], animated: animated.value[0], color: '#1677ff', bg: '#e6f4ff' },
          { icon: '👍', label: '总点赞量', value: vals[1], animated: animated.value[1], color: '#eb2f96', bg: '#fff0f6' },
          { icon: '📈', label: '今日阅读', value: vals[2], animated: animated.value[2], color: '#52c41a', bg: '#f6ffed' },
          { icon: '🔥', label: '今日点赞', value: vals[3], animated: animated.value[3], color: '#fa8c16', bg: '#fff7e6' }
        ]

        // 数字滚动
        nextTick(() => {
          vals.forEach((target, i) => {
            if (target > 0) {
              animated.value[i].value = 0
              anime({
                targets: animated.value[i],
                value: [0, target],
                round: 1,
                duration: 1200,
                delay: i * 150,
                easing: 'easeOutCubic'
              })
            }
          })
          // 卡片入场
          const cards = document.querySelectorAll('.dash-card')
          anime({
            targets: cards,
            opacity: [0, 1],
            translateY: [20, 0],
            duration: 500,
            delay: anime.stagger(80),
            easing: 'easeOutCubic'
          })
        })
      } catch {}
    })

    const formatNumber = (n: number) => {
      if (n >= 10000) return (n / 10000).toFixed(1) + '万'
      return n.toLocaleString()
    }

    return { statistic, dashCards, animated, formatNumber }
  }
})
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.dash-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
}

.dash-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(22, 119, 255, 0.1);
  border-color: var(--card-color);
}

.dash-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.dash-card-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.dash-card-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

.dashboard-tips {
  margin-top: 16px;
}
</style>