<template>
  <section class="card home-card">
    <header class="home-header">
      <div>
        <h2>{{ userNickname || userEmail || 'Hello Chat' }}</h2>
        <p>{{ userEmail }}</p>
      </div>
      <div class="home-actions">
        <router-link to="/profile" class="secondary-btn link-btn">个人资料</router-link>
        <button class="danger-btn" @click="handleLogout">退出登录</button>
      </div>
    </header>

    <h1 class="hero-title">消息工作台</h1>
    <p class="hero-subtitle">从这里进入私聊、好友、群聊、朋友圈和个人资料。</p>

    <div class="module-grid">
      <router-link to="/chats" class="module-card">
        <h3>单聊</h3>
        <p>会话列表、用户搜索、消息发送、撤回、删除和历史记录。</p>
      </router-link>

      <router-link to="/profile" class="module-card">
        <h3>个人资料</h3>
        <p>维护昵称、头像、签名、电话和邮箱验证。</p>
      </router-link>

      <router-link to="/groups" class="module-card">
        <h3>群聊</h3>
        <p>创建群组、管理成员、公告、审批和群消息。</p>
      </router-link>

      <router-link to="/moments" class="module-card">
        <h3>朋友圈</h3>
        <p>发布动态、浏览时间线、评论、点赞、收藏和管理内容。</p>
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
  display: flex;
  max-width: none;
  min-height: 100%;
  flex-direction: column;
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
  flex: 1;
}

.module-card {
  display: block;
  min-height: 220px;
  padding: 28px;
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
  font-size: 22px;
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

