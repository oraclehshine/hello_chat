<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { useAdminStore } from './store'

const route = useRoute()
const router = useRouter()
const admin = useAdminStore()
const publicRoute = computed(() => Boolean(route.meta.public))

const navItems = [
  { path: '/dashboard', label: '首页看板' },
  { path: '/users', label: '用户管理' },
  { path: '/groups', label: '群聊管理' },
  { path: '/sensitive-words', label: '敏感词' },
  { path: '/announcements', label: '公告管理' },
  { path: '/audit-logs', label: '历史记录' },
]

function logout() {
  admin.logout()
  router.push('/login')
}
</script>

<template>
  <RouterView v-if="publicRoute" />

  <div v-else class="admin-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">HC</div>
        <div>
          <strong>Hello Chat</strong>
          <span>Admin Console</span>
        </div>
      </div>

      <nav>
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ admin.displayName || '平台管理员' }}</strong>
          <span>管理端工作台</span>
        </div>
        <button class="ghost-button" type="button" @click="logout">退出</button>
      </header>

      <RouterView />
    </main>
  </div>
</template>

