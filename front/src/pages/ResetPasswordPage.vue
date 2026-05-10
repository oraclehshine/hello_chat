<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { resetPassword, sendCaptcha } from '../api/auth'

const router = useRouter()
const form = reactive({ email: '', captcha: '', newPassword: '' })
const captchaHint = ref('')
const status = ref('')
const error = ref('')
const loadingCaptcha = ref(false)
const submitting = ref(false)

async function requestCaptcha() {
  error.value = ''
  status.value = ''
  captchaHint.value = ''
  if (!form.email) {
    error.value = '请先输入注册邮箱'
    return
  }

  loadingCaptcha.value = true
  try {
    const code = await sendCaptcha(form.email, 'reset_password')
    form.captcha = code
    captchaHint.value = `开发环境验证码：${code}`
    status.value = '验证码已发送，并已自动填入验证码框'
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '发送验证码失败'
  } finally {
    loadingCaptcha.value = false
  }
}

async function submitReset() {
  error.value = ''
  status.value = ''
  if (!form.email || !form.captcha || !form.newPassword) {
    error.value = '请完整填写邮箱、验证码和新密码'
    return
  }

  submitting.value = true
  try {
    await resetPassword(form.email, form.captcha, form.newPassword)
    status.value = '密码已重置，请使用新密码登录'
    await router.push('/login')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '重置密码失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <p class="eyebrow">Account recovery</p>
    <h2 class="hero-title">找回密码</h2>
    <p class="hero-subtitle">重置密码也保留在独立入口里，避免和聊天主工作台混在一起。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label for="reset-email">注册邮箱</label>
        <input id="reset-email" v-model.trim="form.email" type="email" placeholder="user@example.com" autocomplete="email" />
      </div>
      <div class="field">
        <label for="reset-captcha">验证码</label>
        <div class="captcha-row">
          <input id="reset-captcha" v-model.trim="form.captcha" type="text" placeholder="请输入验证码" />
          <button class="secondary-btn captcha-btn" :disabled="loadingCaptcha || !form.email" type="button" @click="requestCaptcha">
            {{ loadingCaptcha ? '发送中...' : '发送验证码' }}
          </button>
        </div>
        <small v-if="captchaHint" class="field-hint">{{ captchaHint }}</small>
      </div>
      <div class="field">
        <label for="reset-password">新密码</label>
        <input id="reset-password" v-model="form.newPassword" type="password" placeholder="请输入新密码" autocomplete="new-password" />
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn full" :disabled="submitting" type="button" @click="submitReset">
        {{ submitting ? '重置中...' : '重置密码' }}
      </button>
    </div>

    <div class="auth-links">
      <span>想起密码了？<RouterLink to="/login">返回登录</RouterLink></span>
      <span>没有账号？<RouterLink to="/register">创建账号</RouterLink></span>
    </div>
  </section>
</template>

<style scoped>
.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-row input {
  min-width: 0;
}

.captcha-btn {
  flex: 0 0 132px;
  padding: 0 14px;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .captcha-row {
    flex-direction: column;
  }

  .captcha-btn {
    flex-basis: auto;
  }
}
</style>
