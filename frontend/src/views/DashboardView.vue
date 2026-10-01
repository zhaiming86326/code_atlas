<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Coin, Collection, DataLine, DocumentChecked, Plus, Refresh, Search, Tickets, Grid } from '@element-plus/icons-vue'
import DemoNotice from '../components/DemoNotice.vue'
import { api, runtimeState } from '../services/api'
import type { DashboardData } from '../types'

const router = useRouter()
const loading = ref(true)
const data = ref<DashboardData>()
const fallback = ref(false)

const stats = computed(() => data.value ? [
  { label: '知识资料', value: data.value.documents.toLocaleString(), trend: '当前累计', icon: Collection, color: '#176b5b', tint: '#e9f5f1' },
  { label: '内容分块', value: data.value.chunks.toLocaleString(), trend: '包含历史索引', icon: Grid, color: '#276c9f', tint: '#edf5fb' },
  { label: '待审核', value: data.value.pendingReviews, trend: '需处理', icon: DocumentChecked, color: '#b37714', tint: '#fff6df' },
  { label: '运行策略', value: data.value.activePolicies, trend: '阶段 4 接通', icon: DataLine, color: '#6458a5', tint: '#f2effb' },
  { label: '失败任务', value: data.value.failedTasks, trend: '阶段 4 接通', icon: Tickets, color: '#b95951', tint: '#fff0ee' },
  { label: '本月估算', value: data.value.monthlyCost == null ? '未知' : `$${data.value.monthlyCost.toFixed(2)}`, trend: '阶段 3 配置价格', icon: Coin, color: '#258073', tint: '#e9f6f3' },
] : [])

async function load() {
  loading.value = true
  try { data.value = await api.dashboard(); fallback.value = runtimeState.demoFallback }
  catch { data.value = undefined } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="page-stack">
    <DemoNotice :fallback="fallback" />

    <div class="toolbar">
      <div class="toolbar-group">
        <button class="primary-button" @click="router.push('/documents')"><Plus />导入资料</button>
        <button class="secondary-button" @click="router.push('/search')"><Search />开始问答</button>
      </div>
      <button class="secondary-button" @click="load"><Refresh />刷新数据</button>
    </div>

    <section class="stats-grid">
      <article v-for="stat in stats" :key="stat.label" class="surface stat-card" :style="{ '--stat-color': stat.color, '--stat-tint': stat.tint }">
        <div class="stat-top"><div class="stat-icon"><component :is="stat.icon" /></div><span class="stat-trend">{{ stat.trend }}</span></div>
        <div class="stat-value">{{ stat.value }}</div>
        <div class="stat-label">{{ stat.label }}</div>
      </article>
    </section>

    <section v-if="data" class="dashboard-grid">
      <article class="surface">
        <div class="surface-header">
          <div><h2>近 7 日入库趋势</h2><p>候选资料发现量与完成索引量</p></div>
          <div class="chart-legend"><span><i class="legend-dot" />发现</span><span><i class="legend-dot indexed" />已索引</span></div>
        </div>
        <div class="chart-body">
          <div v-for="point in data.ingestionTrend" :key="point.date" class="bar-group">
            <div class="bar" :style="{ height: `${point.discovered * 2.7}px` }" :title="`发现 ${point.discovered}`" />
            <div class="bar indexed" :style="{ height: `${point.indexed * 2.7}px` }" :title="`索引 ${point.indexed}`" />
            <span class="bar-label">{{ point.date }}</span>
          </div>
        </div>
      </article>

      <article class="surface">
        <div class="surface-header"><div><h2>最近动态</h2><p>真实导入与审核记录（包含演示种子）</p></div><button class="table-action" @click="router.push('/documents')">查看资料</button></div>
        <div class="activity-list">
          <div v-for="activity in data.recentActivities" :key="activity.id" class="activity-item">
            <span class="activity-marker" :class="activity.status" />
            <div class="activity-copy"><strong>{{ activity.title }}</strong><span>{{ activity.detail }}</span></div>
            <span class="activity-time">{{ activity.time }}</span>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>
