<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import DemoNotice from '../components/DemoNotice.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { api, runtimeState } from '../services/api'
import type { CollectionTask } from '../types'

const loading = ref(true)
const tasks = ref<CollectionTask[]>([])
const fallback = ref(false)
const query = ref('')
const status = ref('')
const filtered = computed(() => tasks.value.filter((task) => (!query.value || `${task.id} ${task.policy} ${task.source}`.toLowerCase().includes(query.value.toLowerCase())) && (!status.value || task.status === status.value)))

async function load() {
  loading.value = true
  try { tasks.value = await api.tasks(); fallback.value = runtimeState.demoFallback }
  catch { tasks.value = [] } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-stack">
    <DemoNotice :fallback="fallback" text="当前展示任务状态模型；持久化队列、幂等锁和失败重试将在阶段 4 实现。" />
    <div class="toolbar">
      <div class="toolbar-group"><el-input v-model="query" class="filter-input" clearable placeholder="搜索任务、策略或来源"><template #prefix><Search /></template></el-input><el-select v-model="status" clearable placeholder="全部状态" style="width:140px"><el-option label="已完成" value="SUCCEEDED" /><el-option label="重试中" value="RETRYING" /><el-option label="已暂停" value="PAUSED" /></el-select></div>
      <button class="secondary-button" @click="load"><Refresh />刷新</button>
    </div>
    <section class="surface">
      <div class="surface-header"><div><h2>执行记录</h2><p>显示来源发现、解析和入库任务</p></div></div>
      <div v-loading="loading" class="table-scroll">
        <table v-if="filtered.length" class="data-table">
          <thead><tr><th>任务</th><th>策略</th><th>来源</th><th>状态</th><th>发现 / 接受</th><th>开始时间</th><th>耗时</th><th>结果</th></tr></thead>
          <tbody><tr v-for="task in filtered" :key="task.id"><td><strong>{{ task.id }}</strong></td><td>{{ task.policy }}</td><td>{{ task.source }}</td><td><StatusTag :status="task.status" /></td><td>{{ task.discovered }} / {{ task.accepted }}</td><td>{{ task.startedAt }}</td><td>{{ task.duration }}</td><td style="min-width:220px">{{ task.message }}</td></tr></tbody>
        </table>
        <EmptyState v-else title="没有匹配的任务" description="清除搜索条件或等待下一次采集运行。" />
      </div>
    </section>
  </div>
</template>

