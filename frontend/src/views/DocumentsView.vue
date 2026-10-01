<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import DemoNotice from '../components/DemoNotice.vue'
import EmptyState from '../components/EmptyState.vue'
import StatusTag from '../components/StatusTag.vue'
import { api, runtimeState } from '../services/api'
import type { KnowledgeDocument } from '../types'

const router = useRouter()
const loading = ref(true)
const documents = ref<KnowledgeDocument[]>([])
const query = ref('')
const technology = ref('')
const status = ref('')
const fallback = ref(false)
const importVisible = ref(false)
const importType = ref('URL')
const importValue = ref('')
const selectedFile = ref<File>()
const importing = ref(false)
const page = ref(1)
const total = ref(0)
const metadata = ref({ title: '', technology: '', version: '', language: '', author: '', license: '', trust: '', tags: [] as string[] })
const tagsText = ref('')
function chooseFile(event: Event) { selectedFile.value = (event.target as HTMLInputElement).files?.[0] }


const filtered = computed(() => documents.value)

const technologies = computed(() => [...new Set(documents.value.map((doc) => doc.technology))])

async function load() {
  loading.value = true
  try {
    const response = await api.documents(query.value, technology.value, status.value, page.value)
    documents.value = response.items; total.value = response.total
    fallback.value = runtimeState.demoFallback
  } catch { documents.value = [] } finally { loading.value = false }
}
async function importDocument() {
  if (runtimeState.demoMode || fallback.value) return ElMessage.warning('演示模式不能写入，请连接 PostgreSQL 后端')
  importing.value = true
  try {
    const m = { ...metadata.value, tags: tagsText.value.split(/[,，]/).map(t => t.trim()).filter(Boolean) }
    const result = importType.value === 'FILE'
      ? await api.importFile(selectedFile.value ?? (() => { throw new Error('请选择文件') })(), m)
      : importType.value === 'NOTE' ? await api.importNote(metadata.value.title, importValue.value, m)
      : await api.importUrl(importValue.value, m)
    ElMessage.success(result.duplicate ? '内容已存在，未创建重复版本' : '导入成功，等待审核')
    importVisible.value = false; importValue.value = ''; await load()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '导入失败') }
  finally { importing.value = false }
}

onMounted(load)
</script>

<template>
  <div class="page-stack">
    <DemoNotice :fallback="fallback" text="列表、筛选和详情已接通 API；导入和删除操作将在阶段 2 持久化。" />
    <div class="toolbar">
      <div class="toolbar-group">
        <el-input v-model="query" class="filter-input" clearable placeholder="搜索正文、标题或标签" @keyup.enter="page = 1; load()"><template #prefix><Search /></template></el-input>
        <el-select v-model="technology" clearable placeholder="全部技术" style="width: 150px"><el-option v-for="item in technologies" :key="item" :label="item" :value="item" /></el-select>
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 140px"><el-option label="已索引" value="INDEXED" /><el-option label="待审核" value="PENDING_REVIEW" /><el-option label="已拒绝" value="REJECTED" /><el-option label="已归档" value="ARCHIVED" /></el-select><el-button @click="page = 1; load()">查询</el-button>
      </div>
      <button class="primary-button" @click="importVisible = true"><Plus />导入资料</button>
    </div>

    <section class="surface">
      <div class="surface-header"><div><h2>全部资料</h2><p>共 {{ filtered.length }} 条，正式问答默认只使用已批准并完成索引的内容</p></div></div>
      <div v-loading="loading" class="table-scroll">
        <table v-if="filtered.length" class="data-table">
          <thead><tr><th>资料</th><th>技术 / 版本</th><th>来源</th><th>状态</th><th>分块</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="doc in filtered" :key="doc.id">
              <td><div class="doc-title"><router-link :to="`/documents/${doc.id}`">{{ doc.title }}</router-link><span>{{ doc.excerpt }}</span><div class="tags"><i v-for="tag in doc.tags.slice(0, 3)" :key="tag" class="soft-tag">{{ tag }}</i></div></div></td>
              <td><div class="source-cell"><strong>{{ doc.technology }}</strong><small>{{ doc.version }}</small></div></td>
              <td><div class="source-cell"><span>{{ doc.sourceName }}</span><small>{{ doc.sourceType }}</small></div></td>
              <td><StatusTag :status="doc.status" /></td>
              <td>{{ doc.chunks || '—' }}</td>
              <td>{{ doc.updatedAt }}</td>
              <td><button class="table-action" @click="router.push(`/documents/${doc.id}`)">查看</button></td>
            </tr>
          </tbody>
        </table>
        <EmptyState v-else title="没有匹配的资料" description="调整搜索词或清除筛选条件后再试。" />
      </div>
      <div class="pagination-row"><span>显示 {{ filtered.length }} 条结果</span><el-pagination small layout="prev, pager, next" :total="total" :page-size="20" v-model:current-page="page" @current-change="load" /></div>
    </section>

    <el-dialog v-model="importVisible" title="导入资料" width="min(520px, 92vw)">
      <el-tabs v-model="importType"><el-tab-pane label="网页 URL" name="URL" /><el-tab-pane label="本地文件" name="FILE" /><el-tab-pane label="个人笔记" name="NOTE" /></el-tabs>
      <el-alert title="UTF-8 文本，最大 2 MiB。相同文件名 / 笔记标题 / 最终 URL 视为同一来源，变化内容创建待审核新版本。" type="info" :closable="false" />
      <el-input v-if="importType !== 'FILE'" v-model="importValue" :type="importType === 'NOTE' ? 'textarea' : 'text'" :rows="8" :placeholder="importType === 'URL' ? 'https://docs.example.com/guide' : 'Markdown 笔记正文'" />
      <input v-else type="file" accept=".md,.markdown,.txt,.html,.htm" aria-label="选择资料文件" @change="chooseFile" />
      <el-form label-position="top">
        <el-form-item label="标题（笔记必填，其他留空自动解析）"><el-input v-model="metadata.title" /></el-form-item>
        <el-form-item label="技术名称"><el-input v-model="metadata.technology" placeholder="未知字段留空" /></el-form-item>
        <el-form-item label="版本范围"><el-input v-model="metadata.version" placeholder="如 21 或 3.5.x；由你确认，不使用 AI 猜测" /></el-form-item>
        <el-form-item label="语言"><el-input v-model="metadata.language" placeholder="zh / en / 未知" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="tagsText" placeholder="逗号分隔" /></el-form-item>
        <el-form-item label="作者 / 组织"><el-input v-model="metadata.author" /></el-form-item>
        <el-form-item label="许可"><el-input v-model="metadata.license" /></el-form-item>
        <el-form-item label="来源可信度"><el-input v-model="metadata.trust" placeholder="自行填写；不确定请留空" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="importVisible = false">取消</el-button><el-button type="primary" :loading="importing" @click="importDocument">导入并提交审核</el-button></template>
    </el-dialog>
  </div>
</template>
