<template>
  <section class="card">
    <h2 class="hero-title">个人资料编辑</h2>

    <div v-if="successMessage" class="notice success">{{ successMessage }}</div>
    <div v-if="errorMessage" class="notice error">{{ errorMessage }}</div>

    <div class="profile-section">
      <h3>基本信息</h3>

      <div class="field">
        <label>邮箱</label>
        <div class="email-display">
          <span>{{ currentEmail }}</span>
          <button @click="toggleEmailEdit" class="btn-small">修改</button>
        </div>
      </div>

      <div class="field">
        <label>昵称</label>
        <input v-model="profileForm.nickname" type="text" placeholder="请输入昵称" />
      </div>

      <div class="field">
        <label>头像</label>
        <div class="avatar-upload">
          <div class="avatar-preview">
            <img v-if="profileForm.avatarUrl" :src="profileForm.avatarUrl" alt="avatar" />
            <span v-else>{{ avatarInitials }}</span>
          </div>
          <div class="avatar-actions">
            <input ref="avatarInput" type="file" accept="image/*" @change="handleAvatarSelect" />
            <small class="field-hint">图片会先上传到阿里云 OSS，再保存为头像地址。</small>
          </div>
        </div>
      </div>

      <div class="field">
        <label>个性签名</label>
        <textarea v-model="profileForm.signature" placeholder="输入你的个性签名" rows="3"></textarea>
      </div>

      <div class="field">
        <label>电话（仅显示，不支持修改）</label>
        <input v-model="profileForm.phone" type="tel" placeholder="电话号码" disabled />
      </div>

      <button @click="handleUpdateProfile" :disabled="loading" class="btn btn-primary">
        {{ loading ? '保存中...' : '保存基本信息' }}
      </button>
    </div>

    <div v-if="showEmailEdit" class="profile-section">
      <h3>修改邮箱</h3>
      <p class="section-desc">修改邮箱需要通过新邮箱验证码确认。</p>

      <div class="email-edit-flow">
        <div v-if="emailStep === 1" class="step">
          <div class="field">
            <label>新邮箱地址</label>
            <input v-model="newEmailForm.email" type="email" placeholder="请输入新邮箱" />
          </div>
          <button @click="handleSendEmailCaptcha" :disabled="emailLoading" class="btn btn-secondary">
            {{ emailLoading ? '发送中...' : '发送验证码' }}
          </button>
          <button @click="cancelEmailEdit" class="btn btn-text">取消</button>
        </div>

        <div v-if="emailStep === 2" class="step">
          <p class="step-desc">验证码已发送到 <strong>{{ newEmailForm.email }}</strong></p>
          <div class="field">
            <label>验证码</label>
            <input v-model="newEmailForm.captcha" type="text" placeholder="请输入6位验证码" maxlength="6" />
          </div>
          <button @click="handleVerifyAndUpdateEmail" :disabled="emailLoading" class="btn btn-primary">
            {{ emailLoading ? '验证中...' : '确认修改' }}
          </button>
          <button @click="emailStep = 1" class="btn btn-text">返回</button>
        </div>
      </div>
    </div>

    <div class="back-section">
      <router-link to="/home" class="btn btn-secondary">返回首页</router-link>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getMe, updateProfile, updateEmail, sendCaptcha } from '../api/auth'
import { uploadImage } from '../api/file'

const currentEmail = ref('')
const currentUserId = ref(0)
const avatarInput = ref<HTMLInputElement | null>(null)

const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
  signature: '',
  phone: '',
})

const newEmailForm = reactive({
  email: '',
  captcha: '',
})

const showEmailEdit = ref(false)
const emailStep = ref(1)
const loading = ref(false)
const emailLoading = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const avatarInitials = computed(() => {
  const source = profileForm.nickname || currentEmail.value || 'HC'
  return source.slice(0, 2).toUpperCase()
})

onMounted(async () => {
  try {
    const data = await getMe()
    currentUserId.value = data.userId
    currentEmail.value = data.email
    profileForm.nickname = data.nickname || ''
    profileForm.avatarUrl = data.avatarUrl || ''
    profileForm.signature = data.signature || ''
    profileForm.phone = data.phone || ''
    localStorage.setItem('authData', JSON.stringify(data))
  } catch {
    const authData = localStorage.getItem('authData')
    if (authData) {
      const data = JSON.parse(authData)
      currentUserId.value = data.userId
      currentEmail.value = data.email
      profileForm.nickname = data.nickname || ''
      profileForm.avatarUrl = data.avatarUrl || ''
      profileForm.signature = data.signature || ''
      profileForm.phone = data.phone || ''
    }
  }
})

async function handleAvatarSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await uploadImage(file, 'avatar')
    profileForm.avatarUrl = result.fileUrl
    successMessage.value = '头像已上传到 OSS，保存资料后即可生效。'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '头像上传失败'
  } finally {
    if (avatarInput.value) {
      avatarInput.value.value = ''
    }
  }
}

async function handleUpdateProfile() {
  errorMessage.value = ''
  successMessage.value = ''
  loading.value = true

  try {
    const result = await updateProfile({
      nickname: profileForm.nickname,
      avatarUrl: profileForm.avatarUrl,
      signature: profileForm.signature,
      phone: profileForm.phone,
    })

    const authData = localStorage.getItem('authData')
    if (authData) {
      const data = JSON.parse(authData)
      data.nickname = result.nickname
      data.avatarUrl = result.avatarUrl
      localStorage.setItem('authData', JSON.stringify(data))
    }

    successMessage.value = '基本信息更新成功'
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '更新失败'
  } finally {
    loading.value = false
  }
}

function toggleEmailEdit() {
  showEmailEdit.value = !showEmailEdit.value
  if (!showEmailEdit.value) {
    cancelEmailEdit()
  }
}

function cancelEmailEdit() {
  showEmailEdit.value = false
  emailStep.value = 1
  newEmailForm.email = ''
  newEmailForm.captcha = ''
  errorMessage.value = ''
}

async function handleSendEmailCaptcha() {
  if (!newEmailForm.email) {
    errorMessage.value = '请输入新邮箱地址'
    return
  }

  if (newEmailForm.email === currentEmail.value) {
    errorMessage.value = '新邮箱不能与当前邮箱相同'
    return
  }

  errorMessage.value = ''
  emailLoading.value = true

  try {
    await sendCaptcha(newEmailForm.email, 'modify_email')
    emailStep.value = 2
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '发送验证码失败'
  } finally {
    emailLoading.value = false
  }
}

async function handleVerifyAndUpdateEmail() {
  if (!newEmailForm.captcha) {
    errorMessage.value = '请输入验证码'
    return
  }

  errorMessage.value = ''
  emailLoading.value = true

  try {
    const result = await updateEmail(newEmailForm.email, newEmailForm.captcha)

    const authData = localStorage.getItem('authData')
    if (authData) {
      const data = JSON.parse(authData)
      data.email = result.email
      localStorage.setItem('authData', JSON.stringify(data))
    }

    currentEmail.value = result.email
    successMessage.value = '邮箱修改成功'
    cancelEmailEdit()

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '邮箱修改失败'
  } finally {
    emailLoading.value = false
  }
}
</script>

<style scoped>
.profile-section {
  margin: 24px 0;
  padding: 20px;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(139, 92, 246, 0.05) 100%);
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.1);
}

.profile-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-size: 18px;
  color: #333;
}

.section-desc {
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.field {
  margin-bottom: 16px;
}

.field label {
  display: block;
  margin-bottom: 6px;
  font-weight: 500;
  color: #333;
  font-size: 14px;
}

.field input,
.field textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.field input:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.avatar-upload {
  display: flex;
  gap: 16px;
  align-items: center;
}

.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #e9eef6;
  color: #10233f;
  font-weight: 700;
  flex: 0 0 auto;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  display: grid;
  gap: 8px;
}

.email-display {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.email-display span {
  flex: 1;
}

.btn-small {
  padding: 6px 12px;
  background-color: #6366f1;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}

.btn {
  padding: 10px 16px;
  border: none;
  border-radius: 6px;
  font-weight: 500;
  cursor: pointer;
  font-size: 14px;
  margin-right: 8px;
  margin-bottom: 8px;
}

.btn-primary {
  background-color: #6366f1;
  color: white;
}

.btn-secondary {
  background-color: #e5e7eb;
  color: #333;
}

.btn-text {
  background-color: transparent;
  color: #6366f1;
  text-decoration: underline;
}

.email-edit-flow {
  margin-top: 16px;
}

.step {
  padding: 16px;
  background-color: #fafafa;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.step-desc {
  margin-bottom: 16px;
  color: #666;
  font-size: 14px;
}

.notice {
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 14px;
}

.notice.success {
  background-color: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.notice.error {
  background-color: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.back-section {
  margin-top: 24px;
  text-align: center;
}

@media (max-width: 640px) {
  .avatar-upload {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
