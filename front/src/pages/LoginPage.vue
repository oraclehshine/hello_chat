<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'

const router = useRouter()
const form = reactive({ email: '', password: '' })
const status = ref('')
const error = ref('')
const loading = ref(false)

async function submitLogin() {
  error.value = ''
  status.value = ''
  loading.value = true
  try {
    const data = await login(form.email, form.password)
    localStorage.setItem('authToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('authData', JSON.stringify({
      userId: data.userId,
      email: data.email,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl
    }))
    status.value = `登录成功，当前用户：${data.nickname}`
    await router.push('/home')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card auth-card">
    <h2 class="hero-title">登录 Hello Chat</h2>
    <p class="hero-subtitle">使用注册邮箱登录，进入单聊、群聊和朋友圈主界面。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label>邮箱</label>
        <input v-model="form.email" type="email" placeholder="user@example.com" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn" :disabled="loading" @click="submitLogin">
        {{ loading ? '登录中...' : '登录' }}
      </button>
      <RouterLink class="secondary-btn link-btn" to="/reset-password">忘记密码</RouterLink>
    </div>
  </section>
</template>
