<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close } from '@element-plus/icons-vue'
import DemoNotice from '../components/DemoNotice.vue'
import EmptyState from '../components/EmptyState.vue'
import { api, runtimeState } from '../services/api'
import type { ReviewItem } from '../types'

const loading = ref(true)
const reviews = ref<ReviewItem[]>([])
const fallback = ref(false)

async function load() {
  loading.value = true
  try { reviews.value = await api.reviews(); fallback.value = runtimeState.demoFallback }
  catch { reviews.value = [] } finally { loading.value = false }
}
onMounted(load)
async function decide(item: ReviewItem, action: 'approve' | 'reject') {
  if (runtimeState.demoMode || fallback.value) return ElMessage.warning('演示模式不能保存审核')
  try {
    const versions = await api.versions(item.id)
    await ElMessageBox.confirm(`确定要${action === 'approve' ? '批准' : '拒绝'}“${item.title}”的当前版本吗？请先查看正文。`, '审核', { type: 'warning' })
    await api.review(item.id, versions[0]!.id, action === 'approve')
    await load(); ElMessage.success('审核记录已保存')
  } catch (error) { if (error instanceof Error) ElMessage.error(error.message) }
}

</script>

<template>
  <div class="page-stack">
    <DemoNotice :fallback="fallback" text="审核动作目前只更新页面；阶段 2 将保存审核记录，并在批准后创建解析和索引任务。" />
    <div class="toolbar"><div><strong style="font-size: 14px">{{ reviews.length }} 条候选资料等待判断</strong></div><span style="color:#6f7e7c;font-size:12px">按相关度从高到低排列</span></div>
    <section v-loading="loading">
      <div v-if="reviews.length" class="review-grid">
        <article v-for="item in reviews" :key="item.id" class="surface review-card">
          <div class="review-head"><div><h3>{{ item.title }}</h3><div class="review-source">{{ item.source }} · {{ item.discoveredAt }}</div><div class="tags" style="margin-top:10px"><i class="soft-tag">{{ item.technology }}</i><i class="soft-tag">{{ item.version }}</i></div></div><router-link :to="`/documents/${item.id}`">查看正文</router-link></div>
          <div class="review-reason">{{ item.reason }}</div>
          <div class="policy-label">检测到的变化</div><ul class="change-list"><li v-for="change in item.changes" :key="change">{{ change }}</li></ul>
          <div class="review-actions"><button class="secondary-button" @click="decide(item, 'reject')"><Close />拒绝</button><button class="primary-button" @click="decide(item, 'approve')"><Check />批准入库</button></div>
        </article>
      </div>
      <div v-else class="surface"><EmptyState title="审核队列已清空" description="新发现的候选资料会先在这里等待确认。" /></div>
    </section>
  </div>
</template>

