<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'
import { useAdminStore } from '../store'

const router = useRouter()
const admin = useAdminStore()
const username = ref('admin')
const password = ref('admin123')
const loading = ref(false)
const error = ref('')

async function submit() {
  loading.value = true
  error.value = ''
  try {
    const session = await login(username.value, password.value)
    admin.setSession(session.accessToken, session.admin.displayName)
    await router.push('/dashboard')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h1>Hello Chat 管理端</h1>
      <p>平台运营、内容治理和实时数据看板</p>

      <form @submit.prevent="submit">
        <input v-model="username" placeholder="管理员账号" />
        <input v-model="password" placeholder="密码" type="password" />
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <span v-if="error" class="status-pill high">{{ error }}</span>
      </form>
    </section>
  </main>
</template>

