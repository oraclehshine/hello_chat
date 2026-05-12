<template>
  <section class="profile-workspace">
    <header class="profile-topbar">
      <div>
        <p class="eyebrow">Account</p>
        <h2>个人资料</h2>
        <p>维护头像、昵称、签名和登录邮箱。保存后会同步到左侧头像菜单。</p>
      </div>
      <div class="profile-top-actions">
        <router-link to="/home" class="secondary-btn compact link-btn icon-text-btn">
          <SvgIcon name="chat" />
          返回概览
        </router-link>
        <button type="button" class="primary-btn compact icon-text-btn" :disabled="loading" @click="handleUpdateProfile">
          <SvgIcon name="send" />
          {{ loading ? '保存中...' : '保存资料' }}
        </button>
      </div>
    </header>

    <div v-if="successMessage" class="notice success">{{ successMessage }}</div>
    <div v-if="errorMessage" class="notice error">{{ errorMessage }}</div>

    <div class="profile-layout">
      <aside class="profile-preview-panel">
        <button class="hero-avatar-button" type="button" @click="openAvatarDialog">
          <AvatarFrame :src="profileForm.avatarUrl" :name="avatarInitials" size="hero" interactive />
          <span class="hero-avatar-hint">
            <SvgIcon name="image" />
            更换头像
          </span>
        </button>
        <div class="profile-preview-main">
          <h3>{{ profileForm.nickname || currentEmail || 'Hello Chat' }}</h3>
          <p>{{ profileForm.signature || '还没有设置个性签名' }}</p>
        </div>
        <dl class="profile-meta-list">
          <div>
            <dt>邮箱</dt>
            <dd>{{ currentEmail || '-' }}</dd>
          </div>
          <div>
            <dt>电话</dt>
            <dd>{{ profileForm.phone || '未设置' }}</dd>
          </div>
          <div>
            <dt>用户 ID</dt>
            <dd>{{ currentUserId || '-' }}</dd>
          </div>
        </dl>
      </aside>

      <main class="profile-editor-grid">
        <section class="profile-section profile-basic-section">
          <header class="section-title-row">
            <div>
              <h3>基础信息</h3>
              <p>这些信息会展示在聊天、好友和群组中。</p>
            </div>
          </header>

          <div class="field-grid profile-field-grid">
            <div class="field">
              <label for="profile-nickname">昵称</label>
              <input id="profile-nickname" v-model="profileForm.nickname" type="text" placeholder="请输入昵称" />
            </div>

            <div class="field">
              <label for="profile-phone">电话</label>
              <input id="profile-phone" v-model="profileForm.phone" type="tel" placeholder="电话号码" disabled />
              <small class="field-hint">当前版本仅展示电话，不支持修改。</small>
            </div>

            <div class="field signature-field">
              <label for="profile-signature">个性签名</label>
              <textarea id="profile-signature" v-model="profileForm.signature" placeholder="输入你的个性签名" rows="5"></textarea>
            </div>
          </div>
        </section>

        <section class="profile-section account-section">
          <header class="section-title-row">
            <div>
              <h3>账号安全</h3>
              <p>修改登录邮箱需要通过新邮箱验证码确认。</p>
            </div>
            <button type="button" class="secondary-btn compact icon-text-btn" @click="toggleEmailEdit">
              <SvgIcon name="edit" />
              {{ showEmailEdit ? '收起' : '修改邮箱' }}
            </button>
          </header>

          <div class="email-display">
            <span>{{ currentEmail || '暂无邮箱' }}</span>
            <small>当前登录邮箱</small>
          </div>

          <div v-if="showEmailEdit" class="email-edit-panel">
            <div v-if="emailStep === 1" class="step">
              <div class="field">
                <label for="new-email">新邮箱地址</label>
                <input id="new-email" v-model.trim="newEmailForm.email" type="email" placeholder="请输入新邮箱" />
              </div>
              <div class="actions">
                <button type="button" class="primary-btn" :disabled="emailLoading" @click="handleSendEmailCaptcha">
                  {{ emailLoading ? '发送中...' : '发送验证码' }}
                </button>
                <button type="button" class="secondary-btn" @click="cancelEmailEdit">取消</button>
              </div>
            </div>

            <div v-if="emailStep === 2" class="step">
              <p class="step-desc">验证码已发送到 <strong>{{ newEmailForm.email }}</strong></p>
              <div class="field">
                <label for="email-captcha">验证码</label>
                <input id="email-captcha" v-model.trim="newEmailForm.captcha" type="text" placeholder="请输入 6 位验证码" maxlength="6" />
              </div>
              <div class="actions">
                <button type="button" class="primary-btn" :disabled="emailLoading" @click="handleVerifyAndUpdateEmail">
                  {{ emailLoading ? '验证中...' : '确认修改' }}
                </button>
                <button type="button" class="secondary-btn" @click="emailStep = 1">返回</button>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>

    <input ref="avatarInput" class="hidden-file-input" type="file" accept="image/*" @change="handleAvatarSelect" />

    <div v-if="showAvatarDialog" class="avatar-dialog-backdrop" @click.self="closeAvatarDialog">
      <section class="avatar-dialog">
        <header class="avatar-dialog-head">
          <div>
            <h3>更新头像</h3>
            <p>点击下方按钮选择图片，上传后再保存资料。</p>
          </div>
          <button type="button" class="secondary-btn compact icon-only-btn" @click="closeAvatarDialog">
            <SvgIcon name="close" />
          </button>
        </header>

        <div class="avatar-dialog-body">
          <AvatarFrame :src="profileForm.avatarUrl" :name="avatarInitials" size="hero" />
          <button type="button" class="avatar-upload-button" @click="avatarInput?.click()">
            <span class="avatar-upload-icon"><SvgIcon name="image" /></span>
            <span>
              选择头像图片
              <small>支持 JPG / PNG，上传后再保存资料</small>
            </span>
          </button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getMe, sendCaptcha, updateEmail, updateProfile } from '../api/auth'
import { uploadImage } from '../api/file'
import AvatarFrame from '../components/AvatarFrame.vue'
import SvgIcon from '../components/SvgIcon.vue'

const currentEmail = ref('')
const currentUserId = ref(0)
const avatarInput = ref<HTMLInputElement | null>(null)
const showAvatarDialog = ref(false)

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
    applyProfile(data)
    localStorage.setItem('authData', JSON.stringify(data))
  } catch {
    const authData = localStorage.getItem('authData')
    if (authData) {
      applyProfile(JSON.parse(authData))
    }
  }
})

function applyProfile(data: {
  userId?: number
  email?: string
  nickname?: string
  avatarUrl?: string | null
  signature?: string | null
  phone?: string | null
}) {
  currentUserId.value = data.userId || 0
  currentEmail.value = data.email || ''
  profileForm.nickname = data.nickname || ''
  profileForm.avatarUrl = data.avatarUrl || ''
  profileForm.signature = data.signature || ''
  profileForm.phone = data.phone || ''
}

function openAvatarDialog() {
  showAvatarDialog.value = true
}

function closeAvatarDialog() {
  showAvatarDialog.value = false
}

async function handleAvatarSelect(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  errorMessage.value = ''
  successMessage.value = ''
  try {
    const result = await uploadImage(file, 'avatar')
    profileForm.avatarUrl = result.fileUrl
    successMessage.value = '头像已上传，保存资料后即可生效'
    showAvatarDialog.value = false
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '头像上传失败'
  } finally {
    if (avatarInput.value) avatarInput.value.value = ''
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
    const data = authData ? JSON.parse(authData) : {}
    data.nickname = result.nickname
    data.avatarUrl = result.avatarUrl
    data.signature = result.signature
    data.phone = result.phone
    localStorage.setItem('authData', JSON.stringify(data))

    successMessage.value = '基础信息更新成功'
    window.setTimeout(() => {
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
  if (!showEmailEdit.value) cancelEmailEdit()
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
    const data = authData ? JSON.parse(authData) : {}
    data.email = result.email
    localStorage.setItem('authData', JSON.stringify(data))

    currentEmail.value = result.email
    successMessage.value = '邮箱修改成功'
    cancelEmailEdit()
    window.setTimeout(() => {
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
.profile-workspace {
  display: flex;
  width: 100%;
  height: calc(100vh - 32px);
  min-height: calc(100vh - 32px);
  overflow: hidden;
  border: 1px solid rgba(85, 131, 255, 0.18);
  border-radius: 28px;
  background:
    radial-gradient(circle at 12% 0%, rgba(79, 140, 255, 0.18), transparent 32%),
    linear-gradient(135deg, #f8fbff 0%, #eef5ff 45%, #ffffff 100%);
  box-shadow: 0 24px 70px rgba(37, 87, 197, 0.12);
  flex-direction: column;
}

.profile-topbar,
.profile-top-actions,
.section-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile-topbar,
.section-title-row {
  justify-content: space-between;
}

.profile-topbar {
  min-height: 86px;
  padding: 18px 22px;
  border-bottom: 1px solid rgba(85, 131, 255, 0.16);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
}

.profile-topbar h2,
.profile-preview-panel h3,
.profile-section h3,
.profile-topbar p,
.profile-preview-panel p {
  margin: 0;
}

.profile-topbar h2 {
  font-size: 26px;
}

.profile-topbar p,
.section-title-row p,
.profile-preview-panel p,
.profile-meta-list dt,
.step-desc {
  color: var(--muted);
}

.profile-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  min-height: 0;
  flex: 1;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.profile-preview-panel,
.profile-section {
  border: 1px solid rgba(85, 131, 255, 0.16);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 44px rgba(37, 87, 197, 0.08);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease, background 0.22s ease;
  animation: profile-rise-in 0.48s ease both;
}

.profile-preview-panel {
  display: flex;
  min-height: 0;
  padding: 24px;
  text-align: center;
  align-items: center;
  flex-direction: column;
  gap: 18px;
  overflow: auto;
  animation-delay: 0.03s;
}

.profile-preview-panel:hover,
.profile-section:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.24);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 56px rgba(37, 87, 197, 0.12);
}

.profile-preview-main {
  display: grid;
  gap: 8px;
}

.profile-preview-main p {
  line-height: 1.7;
  overflow-wrap: anywhere;
}

.profile-meta-list {
  display: grid;
  width: 100%;
  gap: 10px;
  margin: 8px 0 0;
}

.profile-meta-list div {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid rgba(85, 131, 255, 0.12);
  border-radius: 16px;
  background: linear-gradient(135deg, #f7fbff, #eef5ff);
  text-align: left;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.profile-meta-list div:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.22);
  box-shadow: 0 16px 32px rgba(37, 87, 197, 0.1);
}

.profile-meta-list dt,
.profile-meta-list dd {
  margin: 0;
}

.profile-meta-list dd {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 0.8fr);
  min-height: 0;
  gap: 16px;
  overflow: hidden;
}

.profile-section {
  display: grid;
  align-content: start;
  gap: 18px;
  min-height: 0;
  padding: 20px;
  overflow: auto;
}

.profile-basic-section {
  animation-delay: 0.08s;
}

.account-section {
  animation-delay: 0.13s;
}

.profile-field-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.signature-field {
  grid-column: 1 / -1;
}

.email-display {
  display: grid;
  gap: 4px;
  min-height: 64px;
  padding: 14px;
  border: 1px solid var(--line-strong);
  border-radius: 12px;
  background: var(--surface-soft);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.email-display:hover {
  transform: translateY(-1px);
  border-color: rgba(85, 131, 255, 0.2);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}

.email-display span {
  min-width: 0;
  overflow: hidden;
  color: var(--text);
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.email-display small,
.field-hint {
  color: var(--muted);
}

.email-edit-panel,
.step,
.avatar-actions {
  display: grid;
  gap: 14px;
}

.avatar-preview {
  display: grid;
  width: 64px;
  height: 64px;
  overflow: hidden;
  border-radius: 50%;
  place-items: center;
  background: linear-gradient(135deg, #2563ff, #63b3ff);
  color: #fff;
  font-weight: 900;
  box-shadow: 0 16px 36px rgba(37, 99, 255, 0.28);
}

.hero-avatar {
  width: 128px;
  height: 128px;
  font-size: 34px;
  border: 8px solid #fff;
}

.hero-avatar-button {
  position: relative;
  border: 0;
  cursor: pointer;
  padding: 0;
  transition: transform 0.22s ease, filter 0.22s ease;
}

.hero-avatar-button:hover {
  transform: translateY(-3px) scale(1.015);
  filter: saturate(1.04);
}

.hero-avatar-hint {
  position: absolute;
  right: -10px;
  bottom: -10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 999px;
  background: #172033;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  padding: 8px 12px;
  white-space: nowrap;
  opacity: 0.92;
  transition: transform 0.18s ease, opacity 0.18s ease, box-shadow 0.18s ease;
}

.hero-avatar-button:hover .hero-avatar-hint {
  opacity: 1;
  transform: translateY(-2px);
  box-shadow: 0 14px 26px rgba(15, 23, 42, 0.18);
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hidden-file-input {
  display: none;
}

.avatar-upload-button {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(85, 131, 255, 0.22);
  border-radius: 18px;
  background: linear-gradient(135deg, #f7fbff, #edf5ff);
  color: #1f4fc4;
  cursor: pointer;
  padding: 14px;
  text-align: left;
  font-weight: 900;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.avatar-upload-button:hover {
  transform: translateY(-2px);
  border-color: rgba(85, 131, 255, 0.3);
  box-shadow: 0 18px 34px rgba(37, 87, 197, 0.12);
}

.avatar-upload-button small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  font-weight: 600;
}

.avatar-upload-icon {
  display: grid;
  width: 42px;
  height: 42px;
  border-radius: 16px;
  background: #fff;
  color: #2563ff;
  place-items: center;
  box-shadow: 0 12px 26px rgba(37, 99, 255, 0.14);
}

.avatar-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
  background: rgba(15, 23, 42, 0.3);
  backdrop-filter: blur(8px);
  padding: 20px;
}

.avatar-dialog {
  display: grid;
  gap: 18px;
  width: min(460px, 96vw);
  border: 1px solid rgba(85, 131, 255, 0.16);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24px 70px rgba(37, 87, 197, 0.16);
  padding: 22px;
  animation: profile-dialog-in 0.24s ease both;
}

.avatar-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.avatar-dialog-head h3,
.avatar-dialog-head p {
  margin: 0;
}

.avatar-dialog-head p {
  color: var(--muted);
  margin-top: 6px;
}

.avatar-dialog-body {
  display: grid;
  gap: 16px;
  justify-items: center;
}

.dialog-avatar {
  width: 120px;
  height: 120px;
  font-size: 30px;
}

@keyframes profile-rise-in {
  from {
    opacity: 0;
    transform: translateY(16px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes profile-dialog-in {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 1100px) {
  .profile-layout,
  .profile-editor-grid {
    grid-template-columns: 1fr;
    overflow: auto;
  }

  .profile-preview-panel,
  .profile-section {
    overflow: visible;
  }
}

@media (max-width: 720px) {
  .profile-topbar,
  .profile-top-actions,
  .section-title-row {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
