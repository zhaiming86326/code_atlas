<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ status: string }>()
const mapping: Record<string, [string, string]> = {
  ARCHIVED: ['已归档', 'info'], APPROVED: ['已批准', 'success'], REJECTED: ['已拒绝', 'danger'],
  INDEXED: ['已索引', 'success'], PENDING_REVIEW: ['待审核', 'warning'], SUCCEEDED: ['已完成', 'success'],
  RETRYING: ['重试中', 'warning'], PAUSED: ['已暂停', 'info'], FAILED: ['失败', 'danger'], RUNNING: ['运行中', 'primary'],
}
const value = computed(() => mapping[props.status] ?? [props.status, 'info'])
</script>

<template><el-tag :type="value[1] as any" effect="light" round>{{ value[0] }}</el-tag></template>

