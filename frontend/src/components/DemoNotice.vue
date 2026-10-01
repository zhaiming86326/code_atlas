<script setup lang="ts">
defineProps<{ fallback?: boolean; text?: string }>()
import { runtimeState } from '../services/api'
</script>

<template>
  <div class="demo-notice" :class="{ fallback }">
    <span class="demo-dot" />
    <div>
      <strong>{{ runtimeState.error ? `请求失败：${runtimeState.error}` : fallback ? '后端暂未连接，正在使用前端演示数据' : runtimeState.demoMode ? '阶段 1 演示环境' : 'PostgreSQL 真实数据' }}</strong>
      <span>{{ runtimeState.demoMode || fallback ? (text ?? '演示数据不会持久化。') : '手动导入、审核与关键词搜索可用；引用问答需配置模型，语义/混合检索还需 pgvector 与向量索引。主动采集将在阶段 4 接通。' }}</span>
    </div>
  </div>
</template>

