<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, type ModelState, type ModelSettings } from '../services/api'
const state = ref<ModelState>()
const models = ref<ModelSettings[]>([])
const busy = ref(false)
const error = ref('')
async function load() { try { state.value = await api.models(); models.value = state.value.models; error.value = '' } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' } }
async function save() { busy.value = true; try { state.value = await api.saveModels(models.value); ElMessage.success('公开配置已保存；密钥仍从后端读取') } catch(e) { ElMessage.error(e instanceof Error ? e.message : '保存失败') } finally { busy.value = false } }
onMounted(load)
</script>
<template>
 <div class="page-stack">
  <el-alert v-if="error" :title="error" type="error" :closable="false" /><el-button v-if="error" @click="load">重试</el-button>
  <el-alert title="OpenAI 兼容接口：Base URL 包含 /v1。密钥只填写 backend/config/models.yml 或后端环境变量，页面不接收密钥。" type="info" :closable="false" />
  <el-alert v-if="state && !state.vectorReady" title="pgvector 尚未就绪，请在服务器安装扩展并执行 database/004_pgvector.sql；关键词检索可继续使用。" type="warning" :closable="false" />
  <section class="settings-grid">
   <article class="surface settings-form">
    <div v-for="model in models" :key="model.role" class="form-section">
     <h3>{{ model.role === 'LLM' ? '生成模型 LLM' : '向量模型 Embedding' }}</h3>
     <p>密钥状态：{{ model.keyConfigured ? '后端已配置' : '后端未配置' }}</p>
     <div class="form-grid">
      <div class="field full"><label>Base URL</label><el-input v-model="model.baseUrl" /></div>
      <div class="field"><label>模型名（填写厂商实际模型名）</label><el-input v-model="model.model" /></div>
      <div class="field"><label>修订标识（更换同名模型时递增）</label><el-input v-model="model.revision" /></div>
      <div v-if="model.role==='EMBEDDING'" class="field"><label>实际返回维度</label><el-input-number v-model="model.dimensions" :min="1" :max="16000" /></div>
      <div class="field"><label>输入价格 USD / 百万 Token，留空为未知</label><el-input type="number" :model-value="model.inputPricePerMillion ?? ''" @update:model-value="model.inputPricePerMillion = $event === '' ? null : Number($event)" /></div>
      <div v-if="model.role==='LLM'" class="field"><label>输出价格 USD / 百万 Token</label><el-input type="number" :model-value="model.outputPricePerMillion ?? ''" @update:model-value="model.outputPricePerMillion = $event === '' ? null : Number($event)" /></div>
      <div class="field full"><label>价格依据（来源与日期）</label><el-input v-model="model.priceBasis" /></div>
     </div>
    </div>
    <el-button type="primary" :loading="busy" :disabled="!state" @click="save">保存公开配置</el-button>
   </article>
   <aside v-if="state" class="page-stack">
    <article class="surface budget-summary"><h3>本月调用统计</h3><p>调用 {{ state.calls.calls }} 次，失败 {{ state.calls.failed }} 次</p><p>已知费用：{{ state.calls.known_cost == null ? '未知' : `$${state.calls.known_cost}` }}</p><p>费用未知的调用：{{ state.calls.unknown_cost_calls }} 次</p><p>已知费用仅为部分合计。采集预算硬限制在阶段 4 实现。</p></article>
    <article class="surface" style="padding:20px"><h3>索引兼容性</h3><p>Base URL、模型、维度、修订标识共同确定索引。更换配置后，请在文档详情重新生成向量。</p><el-button @click="load">刷新统计</el-button></article>
   </aside>
  </section>
  <el-table v-if="state" :data="state.calls.items"><el-table-column prop="role" label="角色" /><el-table-column prop="model" label="模型" /><el-table-column prop="status" label="状态" /><el-table-column label="费用 USD"><template #default="{row}">{{ row.estimated_cost == null ? '未知' : row.estimated_cost }}</template></el-table-column><el-table-column prop="error" label="错误" /></el-table>
 </div>
</template>
