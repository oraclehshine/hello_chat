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
      avatarUrl: data.avatarUrl,
    }))
    status.value = `登录成功，欢迎回来：${data.nickname || data.email}`
    await router.push('/home')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '登录失败，请检查邮箱和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <p class="eyebrow">Welcome back</p>
    <h2 class="hero-title">登录 Hello Chat</h2>
    <p class="hero-subtitle">输入账号信息后即可进入聊天、群组、朋友圈与个人资料。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label for="login-email">邮箱</label>
        <input id="login-email" v-model.trim="form.email" type="email" placeholder="user@example.com" autocomplete="email" />
      </div>
      <div class="field">
        <label for="login-password">密码</label>
        <input id="login-password" v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn full" :disabled="loading" type="button" @click="submitLogin">
        {{ loading ? '登录中...' : '进入工作台' }}
      </button>
    </div>

    <div class="auth-links">
      <span>还没有账号？<RouterLink to="/register">创建账号</RouterLink></span>
      <span>忘记密码？<RouterLink to="/reset-password">重置密码</RouterLink></span>
    </div>
  </section>
</template>
