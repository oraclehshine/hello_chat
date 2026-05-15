<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { logout } from './api/auth'
import AvatarFrame from './components/AvatarFrame.vue'
import AuthPosterShowcase from './components/AuthPosterShowcase.vue'
import dogLogo from './assets/dog.svg'
import { clearUnreadTitle, prepareMobileNotification } from './utils/mobileNotify'

const route = useRoute()
const router = useRouter()
const showProfileMenu = ref(false)

const isPublicRoute = computed(() => Boolean(route.meta.isPublic))
const authData = computed(() => {
  route.fullPath
  try {
    return JSON.parse(localStorage.getItem('authData') || '{}') as {
      email?: string
      nickname?: string
      avatarUrl?: string | null
    }
  } catch {
    return {}
  }
})
const displayName = computed(() => authData.value.nickname || authData.value.email || 'Hello Chat')
const displayEmail = computed(() => authData.value.email || '在线工作台')
const avatarUrl = computed(() => authData.value.avatarUrl || '')

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    clearUnreadTitle()
  }
}

onMounted(() => {
  prepareMobileNotification()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

async function handleLogout() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (refreshToken) {
    await logout(refreshToken).catch(() => undefined)
  }
  localStorage.removeItem('authToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authData')
  showProfileMenu.value = false
  await router.push('/login')
}
</script>

<template>
  <div v-if="isPublicRoute" class="auth-layout">
    <section class="auth-poster" aria-label="Hello Chat account entrance">
      <div class="brand-ribbon">
        <img class="brand-logo" :src="dogLogo" alt="Hello Chat logo" />
        <div>
          <strong>Hello Chat</strong>
        </div>
      </div>
      <AuthPosterShowcase />
    </section>

    <main class="auth-main">
      <RouterView />
    </main>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="sidebar-brand-copy">
          <img class="sidebar-brand-logo" :src="dogLogo" alt="Hello Chat logo" />
        </div>
        <button class="sidebar-avatar-button" type="button" @click="showProfileMenu = !showProfileMenu">
          <AvatarFrame :src="avatarUrl" :name="displayName" size="lg" />
        </button>

        <section v-if="showProfileMenu" class="profile-popover">
          <div class="profile-popover-head">
            <AvatarFrame :src="avatarUrl" :name="displayName" size="xl" />
            <div>
              <h2>{{ displayName }}</h2>
              <p>{{ displayEmail }}</p>
            </div>
          </div>
          <RouterLink class="secondary-btn compact full" to="/profile" @click="showProfileMenu = false">查看个人资料</RouterLink>
          <button class="danger-btn compact full" type="button" @click="handleLogout">退出登录</button>
        </section>
      </div>

      <nav class="nav-list" aria-label="Main navigation">
        <RouterLink to="/home" aria-label="概览">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-9.5Z" />
          </svg>
          <span>概览</span>
        </RouterLink>
        <RouterLink to="/friends" aria-label="好友">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-3.3 0-6 2.1-6 4.7V20h12v-2.3C15 15.1 12.3 13 9 13Zm8.4-1.4a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4ZM17 13c-.8 0-1.6.1-2.3.4 1.5 1.1 2.3 2.6 2.3 4.3V20h4v-2.1c0-2.7-1.8-4.9-4-4.9Z" />
          </svg>
          <span>好友</span>
        </RouterLink>
        <RouterLink to="/chats" aria-label="单聊">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 4h14a3 3 0 0 1 3 3v7a3 3 0 0 1-3 3h-7.2L6 21v-4H5a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3Zm2.5 5.5h9v-2h-9v2Zm0 4h6.5v-2H7.5v2Z" />
          </svg>
          <span>单聊</span>
        </RouterLink>
        <RouterLink to="/groups" aria-label="群聊">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-3.6 0-6.5 2.2-6.5 5v2h13v-2c0-2.8-2.9-5-6.5-5ZM4.8 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM19.2 12a3 3 0 1 0 0-6 3 3 0 0 0 0 6ZM4.5 14C2.6 14 .9 15.2.5 17.1V20h3v-2c0-1.4.5-2.8 1.4-4H4.5Zm15 0h-.4c.9 1.2 1.4 2.6 1.4 4v2h3v-2.9c-.4-1.9-2.1-3.1-4-3.1Z" />
          </svg>
          <span>群聊</span>
        </RouterLink>
        <RouterLink to="/moments" aria-label="朋友圈">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 2a10 10 0 0 0-8.7 14.9L2 22l5.1-1.3A10 10 0 1 0 12 2Zm0 4.2a3.2 3.2 0 1 1 0 6.4 3.2 3.2 0 0 1 0-6.4Zm-5.6 10.2c1.2-2 3.2-3.1 5.6-3.1s4.4 1.1 5.6 3.1A7.5 7.5 0 0 1 12 18.8a7.5 7.5 0 0 1-5.6-2.4Z" />
          </svg>
          <span>朋友圈</span>
        </RouterLink>
      </nav>
    </aside>

    <main class="content-panel">
      <RouterView />
    </main>
  </div>
</template>
