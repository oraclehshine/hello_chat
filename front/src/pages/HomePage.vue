<template>
  <section class="card home-card">
    <header class="home-header">
      <div>
        <h2>{{ userNickname || userEmail || 'Hello Chat' }}</h2>
        <p>{{ userEmail }}</p>
      </div>
      <div class="home-actions">
        <router-link to="/profile" class="secondary-btn link-btn icon-text-btn">
          <SvgIcon name="profile" />
          <span>个人资料</span>
        </router-link>
        <button class="danger-btn icon-text-btn" @click="handleLogout">
          <SvgIcon name="close" />
          <span>退出登录</span>
        </button>
      </div>
    </header>

    <h1 class="hero-title">消息工作台</h1>
    <p class="hero-subtitle">从这里进入私聊、好友、群聊、朋友圈和个人资料。</p>

    <div class="module-grid">
      <router-link to="/chats" class="module-card">
        <span class="module-icon"><SvgIcon name="chat" /></span>
        <h3>单聊</h3>
        <p>会话列表、用户搜索、消息发送、撤回、删除和历史记录。</p>
      </router-link>

      <router-link to="/profile" class="module-card">
        <span class="module-icon"><SvgIcon name="profile" /></span>
        <h3>个人资料</h3>
        <p>维护昵称、头像、签名、电话和邮箱验证。</p>
      </router-link>

      <router-link to="/groups" class="module-card">
        <span class="module-icon"><SvgIcon name="group" /></span>
        <h3>群聊</h3>
        <p>创建群组、管理成员、公告、审批和群消息。</p>
      </router-link>

      <router-link to="/moments" class="module-card">
        <span class="module-icon"><SvgIcon name="moment" /></span>
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
import SvgIcon from '../components/SvgIcon.vue'

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
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(47, 126, 255, 0.1), transparent 34%),
    #fff;
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
  position: relative;
  display: flex;
  min-height: 220px;
  padding: 28px;
  overflow: hidden;
  border: 1px solid rgba(147, 165, 193, 0.32);
  border-radius: 28px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96)),
    radial-gradient(circle at 92% 4%, rgba(47, 126, 255, 0.18), transparent 34%);
  color: inherit;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: 0 18px 46px rgba(36, 72, 128, 0.08);
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
}

.module-card::before {
  position: absolute;
  inset: 0;
  background: linear-gradient(115deg, transparent 20%, rgba(255, 255, 255, 0.58) 48%, transparent 76%);
  opacity: 0;
  transform: translateX(-135%);
  transition: transform 0.55s ease, opacity 0.26s ease;
  content: '';
  pointer-events: none;
}

.module-card:hover {
  border-color: rgba(79, 140, 255, 0.52);
  box-shadow: 0 24px 60px rgba(47, 126, 255, 0.15);
  transform: translateY(-4px);
}

.module-card:hover::before {
  opacity: 1;
  transform: translateX(135%);
}

.module-card::after {
  position: absolute;
  right: -26px;
  bottom: -34px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: rgba(47, 126, 255, 0.06);
  content: '';
}

.module-icon {
  display: grid;
  width: 54px;
  height: 54px;
  border-radius: 19px;
  place-items: center;
  background: linear-gradient(135deg, #2f7eff, #725cff);
  color: #fff;
  box-shadow: 0 16px 34px rgba(47, 126, 255, 0.22);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.module-card:hover .module-icon {
  transform: translateY(-2px) rotate(-6deg) scale(1.04);
  box-shadow: 0 20px 40px rgba(47, 126, 255, 0.28);
}

.module-icon :deep(.ui-icon) {
  width: 26px;
  height: 26px;
}

.module-card h3 {
  margin: auto 0 10px;
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

