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
    error.value = '请先输入邮箱。'
    return
  }

  loadingCaptcha.value = true
  try {
    const code = await sendCaptcha(form.email, 'register')
    form.captcha = code
    captchaHint.value = `开发环境验证码：${code}`
    status.value = '验证码已获取，并已自动填入输入框。'
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
    error.value = '密码至少 8 位，并且必须包含大写字母、小写字母和数字。'
    return
  }
  if (!form.captcha) {
    error.value = '请先获取验证码。'
    return
  }

  submitting.value = true
  try {
    const data = await register(form.email, form.password, form.captcha)
    localStorage.setItem('authToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem(
      'authData',
      JSON.stringify({
        userId: data.userId,
        email: data.email,
        nickname: data.nickname,
        avatarUrl: data.avatarUrl,
      }),
    )
    status.value = `注册成功：${data.nickname}`
    await router.push('/home')
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="card auth-card">
    <h2 class="hero-title">创建账号</h2>
    <p class="hero-subtitle">使用邮箱注册。密码至少 8 位，并且包含大写字母、小写字母和数字。</p>

    <div v-if="status" class="notice success">{{ status }}</div>
    <div v-if="error" class="notice error">{{ error }}</div>

    <div class="field-grid">
      <div class="field">
        <label>邮箱</label>
        <input v-model.trim="form.email" type="email" placeholder="user@example.com" autocomplete="email" />
      </div>
      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="Password123" autocomplete="new-password" />
        <small class="field-hint">示例：Password123 或 Wang5874579</small>
      </div>
      <div class="field">
        <label>验证码</label>
        <div class="captcha-row">
          <input v-model.trim="form.captcha" type="text" placeholder="请输入邮箱验证码" />
          <button class="primary-btn captcha-btn" :disabled="loadingCaptcha || !form.email" @click="requestCaptcha">
            {{ loadingCaptcha ? '获取中...' : '获取验证码' }}
          </button>
        </div>
        <small v-if="captchaHint" class="field-hint">{{ captchaHint }}</small>
      </div>
    </div>

    <div class="actions">
      <button class="secondary-btn" :disabled="isBusy" @click="submitRegister">注册并登录</button>
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
  border-radius: 14px;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .captcha-row {
    flex-direction: column;
  }

  .captcha-btn {
    flex-basis: auto;
    min-height: 46px;
  }
}
</style>
