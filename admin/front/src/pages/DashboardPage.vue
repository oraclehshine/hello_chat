<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import {
  dashboardWsUrl,
  getDashboardOverview,
  getDashboardTrends,
  type DashboardOverview,
  type RealtimeEvent,
} from '../api'

const overview = ref<DashboardOverview | null>(null)
const liveEvents = ref<RealtimeEvent[]>([])
const chartEl = ref<HTMLDivElement | null>(null)
let chart: echarts.ECharts | null = null
let socket: WebSocket | null = null

async function load() {
  overview.value = await getDashboardOverview()
  liveEvents.value = overview.value.realtimeEvents
  const trends = await getDashboardTrends()
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['在线用户', '消息量', '敏感词命中'] },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trends.map((item) => item.label) },
    yAxis: { type: 'value' },
    series: [
      { name: '在线用户', type: 'line', smooth: true, data: trends.map((item) => item.onlineUsers) },
      { name: '消息量', type: 'line', smooth: true, data: trends.map((item) => item.messages) },
      { name: '敏感词命中', type: 'bar', data: trends.map((item) => item.sensitiveHits) },
    ],
  })
}

function connectWs() {
  socket = new WebSocket(dashboardWsUrl())
  socket.addEventListener('message', (event) => {
    const payload = JSON.parse(event.data) as RealtimeEvent
    liveEvents.value = [payload, ...liveEvents.value].slice(0, 8)
  })
}

onMounted(async () => {
  await load()
  connectWs()
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  socket?.close()
  chart?.dispose()
})
</script>

<template>
  <section class="page-title">
    <div>
      <h1>首页大看板</h1>
      <p>实时掌握用户、消息、敏感词和待处理事项。</p>
    </div>
  </section>

  <section class="grid metrics dashboard-metrics">
    <article v-for="card in overview?.cards" :key="card.key" class="metric-card">
      <span>{{ card.label }}</span>
      <strong>{{ card.value.toLocaleString() }}{{ card.unit }}</strong>
      <em>{{ card.delta >= 0 ? '+' : '' }}{{ card.delta }} 今日变化</em>
    </article>
  </section>

  <section class="dashboard-section">
    <article class="panel trend-panel">
      <h2>实时趋势</h2>
      <div ref="chartEl" class="chart"></div>
    </article>
  </section>

  <section class="grid dashboard-bottom dashboard-section">
    <article class="panel compact-panel">
      <h2>实时动态</h2>
      <div class="event-list">
        <div v-for="event in liveEvents" :key="event.time + event.title" class="event-item">
          <strong>{{ event.title }}</strong>
          <p>{{ event.content }}</p>
          <span>{{ event.time }}</span>
        </div>
      </div>
    </article>

    <article class="panel compact-panel">
      <h2>待办事项</h2>
      <div class="todo-list">
        <div v-for="todo in overview?.todos" :key="todo.label" class="todo-item">
          <strong>{{ todo.label }}</strong>
          <span>{{ todo.value }} 条待处理</span>
        </div>
      </div>
    </article>
  </section>
</template>
