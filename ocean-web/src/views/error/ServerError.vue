<template>
  <div style="text-align: center; padding: 100px 0">
    <div ref="emojiRef" style="font-size: 72px; margin-bottom: 16px">💥</div>
    <h1 style="font-size: 72px; color: #ff4d4f; margin: 0 0 8px">500</h1>
    <p style="font-size: 18px; color: #666; margin: 0 0 24px">服务器遇到了一些问题</p>
    <a-button type="primary" @click="$router.push('/')">返回首页</a-button>
  </div>
</template>

<script lang="ts">
import { defineComponent, ref, onMounted } from 'vue'
import anime from 'animejs/lib/anime.es.js'

export default defineComponent({
  name: 'ServerError',
  setup() {
    const emojiRef = ref<HTMLElement>()

    onMounted(() => {
      if (emojiRef.value) {
        anime({
          targets: emojiRef.value,
          scale: [1, 1.15, 1],
          rotate: [0, -5, 5, 0],
          duration: 2000,
          loop: true,
          easing: 'easeInOutSine'
        })
      }
      const h1 = document.querySelector('h1')
      if (h1) {
        const digits = [{ value: 0 }]
        anime({
          targets: digits,
          value: [0, 500],
          round: 1,
          duration: 1200,
          easing: 'easeOutElastic(1, 0.5)',
          update: () => { h1.textContent = String(digits[0].value) }
        })
      }
      anime({
        targets: 'p, .ant-btn',
        opacity: [0, 1],
        translateY: [15, 0],
        duration: 500,
        delay: anime.stagger(150, { start: 1400 }),
        easing: 'easeOutCubic'
      })
    })

    return { emojiRef }
  }
})
</script>
