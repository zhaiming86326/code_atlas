<script setup lang="ts">
import { FolderOpened } from '@element-plus/icons-vue'
defineProps<{ title: string; description: string }>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-icon"><FolderOpened /></div>
    <strong>{{ title }}</strong>
    <p>{{ description }}</p>
  </div>
</template>

