<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { disableGroup, enableGroup, listGroups, type GroupRow } from '../api'

const keyword = ref('')
const rows = ref<GroupRow[]>([])
const loadingId = ref<number | null>(null)

async function load() {
  rows.value = (await listGroups(keyword.value)).list
}

async function toggleStatus(item: GroupRow) {
  loadingId.value = item.groupId
  try {
    if (item.status === 'active') {
      await disableGroup(item.groupId)
    } else {
      await enableGroup(item.groupId)
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
      <h1>群聊管理</h1>
      <p>查看群规模、消息活跃和风险命中。</p>
    </div>
  </section>

  <section class="panel">
    <div class="toolbar">
      <input v-model="keyword" placeholder="搜索群名称" @keyup.enter="load" />
      <button class="primary-button" type="button" @click="load">搜索</button>
    </div>
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>群名称</th>
          <th>群主</th>
          <th>状态</th>
          <th>成员</th>
          <th>今日消息</th>
          <th>敏感词</th>
          <th>举报</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.groupId">
          <td>{{ item.groupId }}</td>
          <td>{{ item.groupName }}</td>
          <td>{{ item.ownerName }}</td>
          <td><span class="status-pill">{{ item.status }}</span></td>
          <td>{{ item.memberCount }}</td>
          <td>{{ item.todayMessages }}</td>
          <td>{{ item.sensitiveHits }}</td>
          <td>{{ item.reportCount }}</td>
          <td>
            <button class="table-button" type="button" :disabled="loadingId === item.groupId" @click="toggleStatus(item)">
              {{ item.status === 'active' ? '禁用' : '启用' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>
