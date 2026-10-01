<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay } from '@element-plus/icons-vue'
import DemoNotice from '../components/DemoNotice.vue'
import { api, runtimeState } from '../services/api'
import type { CollectionPolicy } from '../types'

const loading = ref(true)
const policies = ref<CollectionPolicy[]>([])
const fallback = ref(false)
const dialogVisible = ref(false)
const form = ref({ name: '', topics: '', domains: '', schedule: '每天 08:00', review: 'MANUAL', budget: 10 })

onMounted(async () => {
  try { policies.value = await api.policies(); fallback.value = runtimeState.demoFallback }
  catch { policies.value = [] } finally { loading.value = false }
})

function createPolicy() {
  if (!runtimeState.demoMode && !fallback.value) return ElMessage.info('采集策略将在阶段 4 接通，未保存')
  if (!form.value.name.trim()) return ElMessage.warning('请输入策略名称')
  policies.value.unshift({
    id: `demo-${Date.now()}`, name: form.value.name, topics: form.value.topics.split(',').map((v) => v.trim()).filter(Boolean),
    sources: ['指定网页', 'RSS'], schedule: form.value.schedule, reviewMode: form.value.review, dailyLimit: 20,
    monthlyBudget: form.value.budget, enabled: false, lastRun: '尚未运行', nextRun: '启用后计算',
  })
  dialogVisible.value = false
  ElMessage.success('演示策略已添加到当前页面')
}

function run(policy: CollectionPolicy) {
  ElMessage.success(`已模拟触发“${policy.name}”检查任务`)
}
</script>

<template>
  <div class="page-stack">
    <DemoNotice :fallback="fallback" text="策略开关和新增操作仅影响当前页面；调度、队列和预算强制控制将在阶段 4 接通。" />
    <el-alert title="采集策略尚未接通；本页仅保留阶段 1 演示交互。" type="info" :closable="false" /><div class="toolbar"><div><strong style="font-size: 14px">{{ policies.filter(p => p.enabled).length }} 个策略正在运行</strong></div><button class="primary-button" @click="dialogVisible = true"><Plus />新建策略</button></div>
    <section v-loading="loading" class="card-grid">
      <article v-for="policy in policies" :key="policy.id" class="surface policy-card">
        <div class="policy-top">
          <div class="policy-title"><h3>{{ policy.name }}</h3><span>{{ policy.schedule }} · {{ policy.reviewMode === 'MANUAL' ? '入库前人工审核' : '自动批准' }}</span></div>
          <el-switch v-model="policy.enabled" inline-prompt active-text="开" inactive-text="停" />
        </div>
        <div class="policy-section"><div class="policy-label">关注主题</div><div class="tags"><i v-for="topic in policy.topics" :key="topic" class="soft-tag">{{ topic }}</i></div></div>
        <div class="policy-section"><div class="policy-label">允许来源</div><div class="tags"><i v-for="source in policy.sources" :key="source" class="soft-tag">{{ source }}</i></div></div>
        <div class="policy-section policy-info">
          <div><span>每日上限</span><strong>{{ policy.dailyLimit }} 项</strong></div>
          <div><span>月度预算</span><strong>${{ policy.monthlyBudget.toFixed(2) }}</strong></div>
        </div>
        <div class="policy-footer"><span>上次：{{ policy.lastRun }}</span><button class="table-action" @click="run(policy)"><VideoPlay style="width: 13px; vertical-align: -2px" /> 立即检查</button></div>
      </article>
    </section>

    <el-dialog v-model="dialogVisible" title="新建采集策略" width="min(620px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="策略名称"><el-input v-model="form.name" placeholder="例如：Java 后端核心栈" /></el-form-item>
        <el-form-item label="关注主题"><el-input v-model="form.topics" placeholder="Java 21, Spring Boot 3.x, PostgreSQL" /><small style="color: #7c8b89">使用逗号分隔</small></el-form-item>
        <el-form-item label="允许域名"><el-input v-model="form.domains" placeholder="spring.io, openjdk.org, github.com" /></el-form-item>
        <div class="form-grid"><el-form-item label="检查频率"><el-select v-model="form.schedule" style="width:100%"><el-option label="每天 08:00" value="每天 08:00" /><el-option label="每 12 小时" value="每 12 小时" /><el-option label="每周一 09:00" value="每周一 09:00" /></el-select></el-form-item><el-form-item label="月度预算（美元）"><el-input-number v-model="form.budget" :min="0" :max="1000" style="width:100%" /></el-form-item></div>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="createPolicy">保存为停用状态</el-button></template>
    </el-dialog>
  </div>
</template>

