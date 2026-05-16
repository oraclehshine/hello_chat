<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAuditLogs, type AuditLogRow } from '../api'

const rows = ref<AuditLogRow[]>([])

onMounted(async () => {
  rows.value = (await listAuditLogs()).list
})
</script>

<template>
  <section class="page-title">
    <div>
      <h1>历史记录</h1>
      <p>管理操作、审核处理和系统事件的审计入口。</p>
    </div>
  </section>

  <section class="panel">
    <table>
      <thead>
        <tr>
          <th>ID</th>
          <th>管理员</th>
          <th>模块</th>
          <th>动作</th>
          <th>对象</th>
          <th>IP</th>
          <th>时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in rows" :key="item.logId">
          <td>{{ item.logId }}</td>
          <td>{{ item.adminName }}</td>
          <td>{{ item.module }}</td>
          <td>{{ item.action }}</td>
          <td>{{ item.target }}</td>
          <td>{{ item.ip }}</td>
          <td>{{ item.createdAt }}</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

