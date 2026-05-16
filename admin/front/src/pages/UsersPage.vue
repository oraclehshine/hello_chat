<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { freezeUser, listUsers, unfreezeUser, type UserRow } from '../api'

const keyword = ref('')
const rows = ref<UserRow[]>([])
const loadingId = ref<number | null>(null)

async function load() {
  rows.value = (await listUsers(keyword.value)).list
}

async function toggleStatus(item: UserRow) {
  loadingId.value = item.userId
  try {
    if (item.status === 'frozen') {
      await unfreezeUser(item.userId)
    } else {
      await freezeUser(item.userId)
    }
    await load()
  } finally {
    loadingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <section class="page-title">
    <div>
      <h1>用户管理</h1>
      <p>查询用户状态、活跃和风险信息。</p>
    </div>
  </section>

  <section class="panel">
    <div class="toolbar">
      <input v-model="keyword" placeholder="搜索邮箱或昵称" @keyup.enter="load" />
      <button class="primary-button" type="button" @click="load">搜索</button>
    </div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>用户</th>
          <th>状态</th>
          <th>消息数</th>
          <th>群数量</th>
          <th>风险</th>
          <th>最后活跃</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.userId">
          <td>{{ item.userId }}</td>
          <td>{{ item.nickname }}<br /><span>{{ item.email }}</span></td>
          <td><span class="status-pill" :class="item.status">{{ item.status }}</span></td>
          <td>{{ item.messageCount }}</td>
          <td>{{ item.groupCount }}</td>
          <td><span class="status-pill" :class="item.riskLevel">{{ item.riskLevel }}</span></td>
          <td>{{ item.lastActiveAt }}</td>
          <td>
            <button class="table-button" type="button" :disabled="loadingId === item.userId" @click="toggleStatus(item)">
              {{ item.status === 'frozen' ? '解冻' : '冻结' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
