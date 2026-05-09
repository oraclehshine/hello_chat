<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword, sendCaptcha } from '../api/auth'

const router = useRouter()
const form = reactive({ email: '', captcha: '', newPassword: '' })
const captchaHint = ref('')
const status = ref('')
const error = ref('')
const loading = ref(false)

async function requestCaptcha() {
  error.value = ''
  status.value = ''
  loading.value = true
  try {
    const code = await sendCaptcha(form.email, 'reset_password')
    form.captcha = code
    captchaHint.value = `开发模式验证码：${code}`
    status.value = '验证码已发送，已自动填入验证码框。'
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '发送验证码失败'
  } finally {
    loading.value = false
  }
}

async function submitReset() {
  error.value = ''
  status.value = ''
  loading.value = true
  try {
    await resetPassword(form.email, form.captcha, form.newPassword)
    status.value = '密码已重置，请使用新密码登录。'
    await router.push('/login')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '重置密码失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card auth-card">
    <h2 class="hero-title">找回密码</h2>
    <p class="hero-subtitle">输入注册邮箱，发送验证码后即可重置密码。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label>注册邮箱</label>
        <input v-model="form.email" type="email" placeholder="user@example.com" />
      </div>
      <div class="field">
        <label>验证码</label>
        <input v-model="form.captcha" type="text" placeholder="请输入验证码" />
        <small v-if="captchaHint" class="field-hint">{{ captchaHint }}</small>
      </div>
      <div class="field">
        <label>新密码</label>
        <input v-model="form.newPassword" type="password" placeholder="请输入新密码" />
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn" :disabled="loading" @click="requestCaptcha">
        {{ loading ? '处理中...' : '发送验证码' }}
      </button>
      <button class="secondary-btn" :disabled="loading" @click="submitReset">重置密码</button>
    </div>
  </section>
</template>
