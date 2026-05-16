<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  createAnnouncement,
  deleteAnnouncement,
  listAnnouncements,
  publishAnnouncement,
  withdrawAnnouncement,
  type AnnouncementPayload,
  type AnnouncementRow,
} from '../api'

const rows = ref<AnnouncementRow[]>([])
const form = ref<AnnouncementPayload>({
  title: '',
  content: '',
  scope: 'all',
  status: 'draft',
})
const error = ref('')
const saving = ref(false)

async function load() {
  rows.value = (await listAnnouncements()).list
}

async function submit() {
  if (!form.value.title.trim() || !form.value.content.trim()) return
  saving.value = true
  error.value = ''
  try {
    await createAnnouncement({
      ...form.value,
      title: form.value.title.trim(),
      content: form.value.content.trim(),
    })
    form.value.title = ''
    form.value.content = ''
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function publish(item: AnnouncementRow) {
  await publishAnnouncement(item.announcementId)
  await load()
}

async function withdraw(item: AnnouncementRow) {
  await withdrawAnnouncement(item.announcementId)
  await load()
}

async function remove(item: AnnouncementRow) {
  await deleteAnnouncement(item.announcementId)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-title">
    <div>
      <h1>公告管理</h1>
      <p>发布、撤回和追踪平台公告。</p>
    </div>
  </section>

  <section class="panel">
    <form class="stack-form" @submit.prevent="submit">
      <div class="inline-form">
        <input v-model="form.title" placeholder="公告标题" />
        <select v-model="form.scope">
          <option value="all">全体用户</option>
          <option value="active">活跃用户</option>
          <option value="risk">风险用户</option>
          <option value="group">群聊用户</option>
        </select>
        <select v-model="form.status">
          <option value="draft">草稿</option>
          <option value="published">发布</option>
        </select>
        <button class="primary-button" type="submit" :disabled="saving">创建公告</button>
      </div>
      <textarea v-model="form.content" rows="3" placeholder="公告内容"></textarea>
    </form>
    <p v-if="error" class="form-error">{{ error }}</p>

    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>标题</th>
          <th>范围</th>
          <th>状态</th>
          <th>阅读数</th>
          <th>创建时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.announcementId">
          <td>{{ item.announcementId }}</td>
          <td>{{ item.title }}</td>
          <td>{{ item.scope }}</td>
          <td><span class="status-pill">{{ item.status }}</span></td>
          <td>{{ item.readCount }}</td>
          <td>{{ item.createdAt }}</td>
          <td class="table-actions">
            <button class="table-button" type="button" @click="publish(item)">发布</button>
            <button class="table-button" type="button" @click="withdraw(item)">撤回</button>
            <button class="table-button danger" type="button" @click="remove(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
