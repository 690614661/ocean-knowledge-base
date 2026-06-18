<template>
  <div class="login-page">
    <!-- Left Content Section -->
    <div class="left-section">
      <div class="brand">
        <div class="brand-icon">🌊</div>
        <span>海洋知识库</span>
      </div>

      <!-- Cartoon Characters -->
      <div class="characters-wrapper">
        <div class="characters-container">
          <!-- Purple tall character - Back layer -->
          <div
            ref="purpleRef"
            class="character character-purple"
            :style="purpleStyle"
          >
            <div class="character-eyes" :style="purpleEyesPos">
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="purpleLookX"
                :force-look-y="purpleLookY"
              />
              <EyeBall
                :size="18"
                :pupil-size="7"
                :max-distance="5"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isPurpleBlinking"
                :force-look-x="purpleLookX"
                :force-look-y="purpleLookY"
              />
            </div>
          </div>

          <!-- Black tall character - Middle layer -->
          <div
            ref="blackRef"
            class="character character-black"
            :style="blackStyle"
          >
            <div class="character-eyes" :style="blackEyesPos">
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="blackLookX"
                :force-look-y="blackLookY"
              />
              <EyeBall
                :size="16"
                :pupil-size="6"
                :max-distance="4"
                eye-color="white"
                pupil-color="#2D2D2D"
                :is-blinking="isBlackBlinking"
                :force-look-x="blackLookX"
                :force-look-y="blackLookY"
              />
            </div>
          </div>

          <!-- Orange semi-circle character - Front left -->
          <div
            ref="orangeRef"
            class="character character-orange"
            :style="orangeStyle"
          >
            <div class="character-eyes" :style="orangeEyesPos">
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="orangeLookX" :force-look-y="orangeLookY" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="orangeLookX" :force-look-y="orangeLookY" />
            </div>
          </div>

          <!-- Yellow tall character - Front right -->
          <div
            ref="yellowRef"
            class="character character-yellow"
            :style="yellowStyle"
          >
            <div class="character-eyes" :style="yellowEyesPos">
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="yellowLookX" :force-look-y="yellowLookY" />
              <Pupil :size="12" :max-distance="5" pupil-color="#2D2D2D" :force-look-x="yellowLookX" :force-look-y="yellowLookY" />
            </div>
            <!-- Yellow mouth -->
            <div class="yellow-mouth" :style="yellowMouthPos" />
          </div>
        </div>
      </div>

      <div class="footer-links">
        <a href="#">广东海洋大学</a>
        <a href="#">软件工程实训</a>
        <a href="#">2026</a>
      </div>

      <!-- Decorative elements -->
      <div class="bg-grid" />
      <div class="bg-blob bg-blob-1" />
      <div class="bg-blob bg-blob-2" />
    </div>

    <!-- Right Login Section -->
    <div class="right-section">
      <div class="login-container">
        <!-- Mobile Logo -->
        <div class="mobile-brand">
          <div class="brand-icon">🌊</div>
          <span>海洋知识库</span>
        </div>

        <!-- Header -->
        <div class="login-header">
          <h1>欢迎回来</h1>
          <p>海洋生物知识库</p>
        </div>

        <!-- Login Form -->
        <form class="login-form" @submit.prevent="handleSubmit">
          <div class="form-group">
            <label for="loginName">用户名</label>
            <a-input
              id="loginName"
              v-model:value="loginName"
              placeholder="请输入用户名"
              size="large"
              :disabled="isLoading"
              @focus="isTyping = true"
              @blur="isTyping = false"
            />
          </div>

          <div class="form-group">
            <label for="password">密码</label>
            <a-input-password
              id="password"
              v-model:value="password"
              placeholder="••••••••"
              size="large"
              :disabled="isLoading"
              :visibility-toggle="true"
            />
          </div>

          <div class="form-options">
            <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            <a class="forgot-link" href="#">忘记密码？</a>
          </div>

          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="isLoading"
            class="login-btn"
          >
            {{ isLoading ? '登录中...' : '登录' }}
          </a-button>
        </form>

        <!-- Footer -->
        <div class="login-footer">
          默认账号: admin / admin123
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userApi } from '../../api'
import EyeBall from './EyeBall.vue'
import Pupil from './Pupil.vue'

const router = useRouter()

// ====== State ======
const loginName = ref('')
const password = ref('')
const error = ref('')
const isLoading = ref(false)
const rememberMe = ref(false)

const mouseX = ref(0)
const mouseY = ref(0)
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
const isTyping = ref(false)
const isLookingAtEachOther = ref(false)
const isPurplePeeking = ref(false)

// Refs for character elements
const purpleRef = ref<HTMLDivElement | null>(null)
const blackRef = ref<HTMLDivElement | null>(null)
const yellowRef = ref<HTMLDivElement | null>(null)
const orangeRef = ref<HTMLDivElement | null>(null)

// ====== Mouse tracking ======
const handleMouseMove = (e: MouseEvent) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

onMounted(() => window.addEventListener('mousemove', handleMouseMove))
onUnmounted(() => window.removeEventListener('mousemove', handleMouseMove))

// ====== Blink timers ======
let purpleBlinkTimer: ReturnType<typeof setTimeout> | null = null
let blackBlinkTimer: ReturnType<typeof setTimeout> | null = null

const randomBlinkInterval = () => Math.random() * 4000 + 3000

const schedulePurpleBlink = () => {
  purpleBlinkTimer = setTimeout(() => {
    isPurpleBlinking.value = true
    setTimeout(() => {
      isPurpleBlinking.value = false
      schedulePurpleBlink()
    }, 150)
  }, randomBlinkInterval())
}

const scheduleBlackBlink = () => {
  blackBlinkTimer = setTimeout(() => {
    isBlackBlinking.value = true
    setTimeout(() => {
      isBlackBlinking.value = false
      scheduleBlackBlink()
    }, 150)
  }, randomBlinkInterval())
}

onMounted(() => {
  schedulePurpleBlink()
  scheduleBlackBlink()
})

onUnmounted(() => {
  if (purpleBlinkTimer) clearTimeout(purpleBlinkTimer)
  if (blackBlinkTimer) clearTimeout(blackBlinkTimer)
})

// ====== Look at each other when typing ======
let lookTimer: ReturnType<typeof setTimeout> | null = null

watch(isTyping, (typing) => {
  if (typing) {
    isLookingAtEachOther.value = true
    lookTimer = setTimeout(() => {
      isLookingAtEachOther.value = false
    }, 800)
  } else {
    isLookingAtEachOther.value = false
  }
})

onUnmounted(() => {
  if (lookTimer) clearTimeout(lookTimer)
})

// ====== Purple peeking when password visible ======
let peekTimer: ReturnType<typeof setTimeout> | null = null

const schedulePeek = () => {
  peekTimer = setTimeout(() => {
    isPurplePeeking.value = true
    setTimeout(() => {
      isPurplePeeking.value = false
    }, 800)
  }, Math.random() * 3000 + 2000)
}

watch(() => password.value, (pw) => {
  if (peekTimer) clearTimeout(peekTimer)
  if (pw.length > 0) {
    schedulePeek()
  } else {
    isPurplePeeking.value = false
  }
})

// ====== Position calculations ======
interface Pos {
  faceX: number
  faceY: number
  bodySkew: number
}

const calculatePosition = (el: HTMLDivElement | null): Pos => {
  if (!el) return { faceX: 0, faceY: 0, bodySkew: 0 }
  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 3
  const dx = mouseX.value - cx
  const dy = mouseY.value - cy
  return {
    faceX: Math.max(-15, Math.min(15, dx / 20)),
    faceY: Math.max(-10, Math.min(10, dy / 30)),
    bodySkew: Math.max(-6, Math.min(6, -dx / 120)),
  }
}

const purplePos = computed(() => calculatePosition(purpleRef.value))
const blackPos = computed(() => calculatePosition(blackRef.value))
const yellowPos = computed(() => calculatePosition(yellowRef.value))
const orangePos = computed(() => calculatePosition(orangeRef.value))

// ====== Computed styles ======
const typingOrHidden = computed(() => isTyping.value || password.value.length > 0)

// Purple
const purpleStyle = computed(() => ({
  height: typingOrHidden.value ? '440px' : '400px',
  transform: false
    ? 'skewX(0deg)'
    : typingOrHidden.value
      ? `skewX(${(purplePos.value.bodySkew || 0) - 12}deg) translateX(40px)`
      : `skewX(${purplePos.value.bodySkew || 0}deg)`,
}))

const purpleEyesPos = computed(() => ({
  left: false ? '20px' : isLookingAtEachOther.value ? '55px' : `${45 + purplePos.value.faceX}px`,
  top: false ? '35px' : isLookingAtEachOther.value ? '65px' : `${40 + purplePos.value.faceY}px`,
}))

const purpleLookX = computed(() =>
  false ? (isPurplePeeking.value ? 4 : -4) : isLookingAtEachOther.value ? 3 : undefined
)
const purpleLookY = computed(() =>
  false ? (isPurplePeeking.value ? 5 : -4) : isLookingAtEachOther.value ? 4 : undefined
)

// Black
const blackStyle = computed(() => ({
  transform: false
    ? 'skewX(0deg)'
    : isLookingAtEachOther.value
      ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
      : typingOrHidden.value
        ? `skewX(${(blackPos.value.bodySkew || 0) * 1.5}deg)`
        : `skewX(${blackPos.value.bodySkew || 0}deg)`,
}))

const blackEyesPos = computed(() => ({
  left: false ? '10px' : isLookingAtEachOther.value ? '32px' : `${26 + blackPos.value.faceX}px`,
  top: false ? '28px' : isLookingAtEachOther.value ? '12px' : `${32 + blackPos.value.faceY}px`,
}))

const blackLookX = computed(() =>
  false ? -4 : isLookingAtEachOther.value ? 0 : undefined
)
const blackLookY = computed(() =>
  false ? -4 : isLookingAtEachOther.value ? -4 : undefined
)

// Orange
const orangeStyle = computed(() => ({
  transform: false ? 'skewX(0deg)' : `skewX(${orangePos.value.bodySkew || 0}deg)`,
}))

const orangeEyesPos = computed(() => ({
  left: false ? '50px' : `${82 + (orangePos.value.faceX || 0)}px`,
  top: false ? '85px' : `${90 + (orangePos.value.faceY || 0)}px`,
}))

const orangeLookX = computed(() => false ? -5 : undefined)
const orangeLookY = computed(() => false ? -4 : undefined)

// Yellow
const yellowStyle = computed(() => ({
  transform: false ? 'skewX(0deg)' : `skewX(${yellowPos.value.bodySkew || 0}deg)`,
}))

const yellowEyesPos = computed(() => ({
  left: false ? '20px' : `${52 + (yellowPos.value.faceX || 0)}px`,
  top: false ? '35px' : `${40 + (yellowPos.value.faceY || 0)}px`,
}))

const yellowMouthPos = computed(() => ({
  left: false ? '10px' : `${40 + (yellowPos.value.faceX || 0)}px`,
  top: false ? '88px' : `${88 + (yellowPos.value.faceY || 0)}px`,
}))

const yellowLookX = computed(() => false ? -5 : undefined)
const yellowLookY = computed(() => false ? -4 : undefined)

// ====== Submit ======
const handleSubmit = async () => {
  error.value = ''
  isLoading.value = true

  try {
    const res: any = await userApi.login({
      loginName: loginName.value,
      password: password.value
    })
    const user = res.content
    sessionStorage.setItem('user', JSON.stringify(user))
    message.success('登录成功')
    router.push('/')
  } catch (e: any) {
    error.value = e?.message || '登录失败，请重试'
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr;
  overflow: hidden;
}

@media (min-width: 1024px) {
  .login-page {
    grid-template-columns: 1fr 1fr;
  }
}

/* ====== Left Section ====== */
.left-section {
  position: relative;
  display: none;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.9), #6366f1, rgba(99, 102, 241, 0.8));
  padding: 48px;
  color: white;
  overflow: hidden;
}

@media (min-width: 1024px) {
  .left-section {
    display: flex;
  }
}

.brand {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.brand-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.characters-wrapper {
  position: relative;
  z-index: 20;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  height: 500px;
}

.characters-container {
  position: relative;
  width: 550px;
  height: 400px;
}

.character {
  position: absolute;
  bottom: 0;
  transition: all 0.7s ease-in-out;
}

.character-purple {
  left: 70px;
  width: 180px;
  height: 400px;
  background-color: #6C3FF5;
  border-radius: 10px 10px 0 0;
  z-index: 1;
  transform-origin: bottom center;
}

.character-black {
  left: 240px;
  width: 120px;
  height: 310px;
  background-color: #2D2D2D;
  border-radius: 8px 8px 0 0;
  z-index: 2;
  transform-origin: bottom center;
}

.character-orange {
  left: 0;
  width: 240px;
  height: 200px;
  background-color: #FF9B6B;
  border-radius: 120px 120px 0 0;
  z-index: 3;
  transform-origin: bottom center;
}

.character-yellow {
  left: 310px;
  width: 140px;
  height: 230px;
  background-color: #E8D754;
  border-radius: 70px 70px 0 0;
  z-index: 4;
  transform-origin: bottom center;
}

.character-eyes {
  position: absolute;
  display: flex;
  gap: 24px;
  transition: all 0.7s ease-in-out;
}

.character-black .character-eyes {
  gap: 16px;
}

.yellow-mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  background: #2D2D2D;
  border-radius: 999px;
  transition: all 0.2s ease-out;
}

.footer-links {
  position: relative;
  z-index: 20;
  display: flex;
  gap: 32px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
}

.footer-links a {
  color: inherit;
  text-decoration: none;
  transition: color 0.2s;
}

.footer-links a:hover {
  color: white;
}

/* Decorative */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 20px 20px;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
}

.bg-blob-1 {
  top: 25%;
  right: 25%;
  width: 256px;
  height: 256px;
  background: rgba(255, 255, 255, 0.1);
  filter: blur(64px);
}

.bg-blob-2 {
  bottom: 25%;
  left: 25%;
  width: 384px;
  height: 384px;
  background: rgba(255, 255, 255, 0.05);
  filter: blur(64px);
}

/* ====== Right Section ====== */
.right-section {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: #fff;
}

.login-container {
  width: 100%;
  max-width: 420px;
}

.mobile-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 48px;
}

@media (min-width: 1024px) {
  .mobile-brand {
    display: none;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-header h1 {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.025em;
  margin: 0 0 8px;
}

.login-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.forgot-link {
  font-size: 14px;
  color: #6366f1;
  text-decoration: none;
  font-weight: 500;
}

.forgot-link:hover {
  text-decoration: underline;
}

.error-message {
  padding: 12px;
  font-size: 14px;
  color: #f87171;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 8px;
}

.login-btn {
  height: 48px !important;
  font-size: 16px !important;
  font-weight: 500 !important;
}

.login-footer {
  text-align: center;
  font-size: 13px;
  color: #9ca3af;
  margin-top: 32px;
}
</style>
