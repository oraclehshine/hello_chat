<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
  createSensitiveWord,
  deleteSensitiveWord,
  disableSensitiveWord,
  enableSensitiveWord,
  listSensitiveWords,
  type SensitiveWordPayload,
  type SensitiveWordRow,
} from '../api'

const rows = ref<SensitiveWordRow[]>([])
const form = ref<SensitiveWordPayload>({
  word: '',
  scene: 'chat',
  level: 'medium',
  action: 'warn',
  enabled: true,
})
const error = ref('')
const saving = ref(false)

async function load() {
  rows.value = (await listSensitiveWords()).list
}

async function submit() {
  if (!form.value.word.trim()) return
  saving.value = true
  error.value = ''
  try {
    await createSensitiveWord({ ...form.value, word: form.value.word.trim() })
    form.value.word = ''
    await load()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function toggle(item: SensitiveWordRow) {
  if (item.enabled) {
    await disableSensitiveWord(item.wordId)
  } else {
    await enableSensitiveWord(item.wordId)
  }
  await load()
}

async function remove(item: SensitiveWordRow) {
  await deleteSensitiveWord(item.wordId)
  await load()
}

onMounted(load)
</script>

<template>
  <section class="page-title">
    <div>
      <h1>敏感词管理</h1>
      <p>维护聊天、朋友圈、公告等场景的敏感词规则。</p>
    </div>
  </section>

  <section class="panel">
    <form class="inline-form" @submit.prevent="submit">
      <input v-model="form.word" placeholder="输入词条" />
      <select v-model="form.scene">
        <option value="chat">聊天</option>
        <option value="group">群聊</option>
        <option value="moment">朋友圈</option>
        <option value="announcement">公告</option>
        <option value="all">全局</option>
      </select>
      <select v-model="form.level">
        <option value="low">低</option>
        <option value="medium">中</option>
        <option value="high">高</option>
      </select>
      <select v-model="form.action">
        <option value="warn">提醒</option>
        <option value="review">审核</option>
        <option value="block">拦截</option>
      </select>
      <label class="check-label">
        <input v-model="form.enabled" type="checkbox" />
        启用
      </label>
      <button class="primary-button" type="submit" :disabled="saving">新增词条</button>
    </form>
    <p v-if="error" class="form-error">{{ error }}</p>

    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>词条</th>
          <th>场景</th>
          <th>等级</th>
          <th>动作</th>
          <th>启用</th>
          <th>命中次数</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.wordId">
          <td>{{ item.wordId }}</td>
          <td>{{ item.word }}</td>
          <td>{{ item.scene }}</td>
          <td><span class="status-pill" :class="item.level">{{ item.level }}</span></td>
          <td>{{ item.action }}</td>
          <td>{{ item.enabled ? '是' : '否' }}</td>
          <td>{{ item.hitCount }}</td>
          <td class="table-actions">
            <button class="table-button" type="button" @click="toggle(item)">
              {{ item.enabled ? '停用' : '启用' }}
            </button>
            <button class="table-button danger" type="button" @click="remove(item)">删除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
