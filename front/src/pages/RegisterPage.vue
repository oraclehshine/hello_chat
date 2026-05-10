<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register, sendCaptcha } from '../api/auth'

const router = useRouter()
const form = reactive({ email: '', password: '', captcha: '' })
const captchaHint = ref('')
const status = ref('')
const error = ref('')
const loadingCaptcha = ref(false)
const submitting = ref(false)

const passwordInvalid = computed(() => !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,64}$/.test(form.password))
const isBusy = computed(() => loadingCaptcha.value || submitting.value)

async function requestCaptcha() {
  error.value = ''
  status.value = ''
  captchaHint.value = ''
  if (!form.email) {
    error.value = '请先输入邮箱'
    return
  }

  loadingCaptcha.value = true
  try {
    const code = await sendCaptcha(form.email, 'register')
    form.captcha = code
    captchaHint.value = `开发环境验证码：${code}`
    status.value = '验证码已获取，并已自动填入输入框'
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '获取验证码失败'
  } finally {
    loadingCaptcha.value = false
  }
}

async function submitRegister() {
  error.value = ''
  status.value = ''
  if (passwordInvalid.value) {
    error.value = '密码至少 8 位，并且必须包含大写字母、小写字母和数字'
    return
  }
  if (!form.captcha) {
    error.value = '请先获取验证码'
    return
  }

  submitting.value = true
  try {
    const data = await register(form.email, form.password, form.captcha)
    localStorage.setItem('authToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('authData', JSON.stringify({
      userId: data.userId,
      email: data.email,
      nickname: data.nickname,
      avatarUrl: data.avatarUrl,
    }))
    status.value = `注册成功，欢迎：${data.nickname || data.email}`
    await router.push('/home')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="auth-card">
    <p class="eyebrow">New account</p>
    <h2 class="hero-title">创建账号</h2>
    <p class="hero-subtitle">注册入口已和主功能区分开。完成邮箱验证后，会自动进入 Hello Chat 工作台。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label for="register-email">邮箱</label>
        <input id="register-email" v-model.trim="form.email" type="email" placeholder="user@example.com" autocomplete="email" />
      </div>
      <div class="field">
        <label for="register-password">密码</label>
        <input id="register-password" v-model="form.password" type="password" placeholder="Password123" autocomplete="new-password" />
        <small class="field-hint">至少 8 位，包含大写字母、小写字母和数字，例如 Password123</small>
      </div>
      <div class="field">
        <label for="register-captcha">验证码</label>
        <div class="captcha-row">
          <input id="register-captcha" v-model.trim="form.captcha" type="text" placeholder="请输入邮箱验证码" />
          <button class="secondary-btn captcha-btn" :disabled="loadingCaptcha || !form.email" type="button" @click="requestCaptcha">
            {{ loadingCaptcha ? '获取中...' : '获取验证码' }}
          </button>
        </div>
        <small v-if="captchaHint" class="field-hint">{{ captchaHint }}</small>
      </div>
    </div>

    <div class="actions">
      <button class="primary-btn full" :disabled="isBusy" type="button" @click="submitRegister">
        {{ submitting ? '创建中...' : '注册并进入' }}
      </button>
    </div>

    <div class="auth-links">
      <span>已有账号？<RouterLink to="/login">返回登录</RouterLink></span>
      <span>忘记密码？<RouterLink to="/reset-password">重置密码</RouterLink></span>
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
