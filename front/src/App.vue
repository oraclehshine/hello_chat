<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { logout } from './api/auth'

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
const initials = computed(() => displayName.value.slice(0, 2).toUpperCase())
const avatarUrl = computed(() => authData.value.avatarUrl || '')

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
        <span class="brand-mark">HC</span>
        <div>
          <strong>Hello Chat</strong>
          <small>实时聊天工作台</small>
        </div>
      </div>
      <div class="poster-copy">
        <p class="eyebrow">Secure messaging</p>
        <h1>登录后进入你的聊天工作台。</h1>
        <p>账号入口独立展示，主功能区只保留聊天、好友、群组、朋友圈和个人资料，让界面更接近主流 IM 产品。</p>
      </div>
      <div>
        <div class="floating-card card-one">私聊消息</div>
        <div class="floating-card card-two">群聊协作</div>
        <div class="floating-card card-three">朋友圈动态</div>
      </div>
    </section>

    <main class="auth-main">
      <RouterView />
    </main>
  </div>

  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <button class="sidebar-avatar-button" type="button" @click="showProfileMenu = !showProfileMenu">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ initials }}</span>
        </button>
        <div>
          <h1>Hello Chat</h1>
          <p>消息工作台</p>
        </div>

        <section v-if="showProfileMenu" class="profile-popover">
          <div class="profile-popover-head">
            <div class="avatar xl">
              <img v-if="avatarUrl" :src="avatarUrl" alt="" />
              <span v-else>{{ initials }}</span>
            </div>
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
        <RouterLink to="/home">概览</RouterLink>
        <RouterLink to="/friends">好友</RouterLink>
        <RouterLink to="/chats">单聊</RouterLink>
        <RouterLink to="/groups">群聊</RouterLink>
        <RouterLink to="/moments">朋友圈</RouterLink>
      </nav>
    </aside>

    <main class="content-panel">
      <RouterView />
    </main>
  </div>
</template>
