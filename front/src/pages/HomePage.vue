<template>
  <section class="card home-card">
    <header class="home-header">
      <div>
        <h2>{{ userNickname || userEmail || 'Hello Chat' }}</h2>
        <p>{{ userEmail }}</p>
      </div>
      <div class="home-actions">
        <router-link to="/profile" class="secondary-btn link-btn">Profile</router-link>
        <button class="danger-btn" @click="handleLogout">Logout</button>
      </div>
    </header>

    <h1 class="hero-title">Workspace</h1>
    <p class="hero-subtitle">Open a private chat, manage your profile, and continue through the project milestones.</p>

    <div class="module-grid">
      <router-link to="/chats" class="module-card">
        <h3>Private Chat</h3>
        <p>Conversation list, user search, message sending, recall, delete, and message history.</p>
      </router-link>

      <router-link to="/profile" class="module-card">
        <h3>Profile</h3>
        <p>Nickname, avatar, signature, phone, and verified email changes.</p>
      </router-link>

      <router-link to="/groups" class="module-card">
        <h3>Groups</h3>
        <p>Group creation, members, notices, approvals, and real-time group messages.</p>
      </router-link>

      <router-link to="/moments" class="module-card">
        <h3>Moments</h3>
        <p>Publish moments, browse timeline, comment, like, collect, and manage your posts.</p>
      </router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '../api/auth'

const router = useRouter()
const userEmail = ref('')
const userNickname = ref('')

onMounted(() => {
  const authData = localStorage.getItem('authData')
  if (authData) {
    const data = JSON.parse(authData)
    userEmail.value = data.email || ''
    userNickname.value = data.nickname || ''
  }
})

async function handleLogout() {
  const refreshToken = localStorage.getItem('refreshToken')
  if (refreshToken) {
    await logout(refreshToken).catch(() => undefined)
  }
  localStorage.removeItem('authToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('authData')
  router.push('/login')
}
</script>

<style scoped>
.home-card {
  max-width: 1120px;
}

.home-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 28px;
  padding-bottom: 18px;
  border-bottom: 1px solid #d8e0ea;
}

.home-header h2 {
  margin: 0;
  font-size: 20px;
}

.home-header p {
  margin: 6px 0 0;
  color: #53627d;
}

.home-actions {
  display: flex;
  gap: 10px;
}

.danger-btn {
  padding: 14px 18px;
  border: 0;
  border-radius: 14px;
  background: #e44d4d;
  color: #fff;
  cursor: pointer;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.module-card {
  display: block;
  min-height: 150px;
  padding: 20px;
  border: 1px solid #d8e0ea;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.module-card:hover {
  border-color: rgba(79, 140, 255, 0.52);
  transform: translateY(-2px);
}

.module-card h3 {
  margin: 0 0 10px;
}

.module-card p {
  margin: 0;
  color: #53627d;
  line-height: 1.55;
}

.module-card.muted {
  background: #f7f9fc;
}

@media (max-width: 760px) {
  .home-header,
  .home-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
