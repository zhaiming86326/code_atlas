
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Link, RefreshRight } from '@element-plus/icons-vue'
import StatusTag from '../components/StatusTag.vue'
import DemoNotice from '../components/DemoNotice.vue'
import { api, runtimeState, type DocumentVersion, type ContentChunk, type ReviewRecord } from '../services/api'
import type { KnowledgeDocument } from '../types'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const doc = ref<KnowledgeDocument>()
const fallback = ref(false)

const embedding = ref<{vectorReady: boolean; items: {profile_id: string; model: string; dimensions: number; status: string; error: string}[]}>()
const embeddingBusy = ref(false)
async function vectorIndex() { embeddingBusy.value = true; try { await api.embeddingIndex(String(route.params.id)); embedding.value = await api.embeddingStatus(String(route.params.id)); ElMessage.success('向量索引完成') } catch(e) { ElMessage.error(e instanceof Error ? e.message : '向量索引失败'); embedding.value = await api.embeddingStatus(String(route.params.id)).catch(() => undefined) } finally { embeddingBusy.value = false } }
const versions = ref<DocumentVersion[]>([])
const linkedVersion = computed(() => versions.value.find(v => v.id === route.query.version))
const details = ref({ author: '未知', trust: '未知' })
const editVisible = ref(false)
const revisionVisible = ref(false)
const revision = ref({ content: '', format: 'MARKDOWN' })
const chunks = ref<ContentChunk[]>([])
const chunkVersion = ref('')
const citedChunk = ref<ContentChunk>()
const chunkPage = ref(1)
const chunkTotal = ref(0)
const chunkLoading = ref(false)
const records = ref<ReviewRecord[]>([])
const reviewPage = ref(1)
const reviewTotal = ref(0)
async function loadChunks(versionId: string, page = 1) {
  chunkLoading.value = true
  try {
    const result = await api.chunks(doc.value!.id, versionId, page)
    chunks.value = result.items; chunkTotal.value = result.total; chunkPage.value = page; chunkVersion.value = versionId
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '分块加载失败') }
  finally { chunkLoading.value = false }
}
async function loadReviews(page = 1) {
  try {
    const result = await api.reviewRecords(doc.value!.id, page)
    records.value = result.items; reviewTotal.value = result.total; reviewPage.value = page
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '审核记录加载失败') }
}
function openRevision() { if (doc.value) { revision.value.content = doc.value.content; revisionVisible.value = true } }

const tagsText = ref('')
const metadata = ref({ title: '', technology: '', version: '', language: '', author: '', license: '', trust: '' })
async function load() {
  loading.value = true
  try {
    doc.value = await api.document(String(route.params.id)); fallback.value = runtimeState.demoFallback
    if (!runtimeState.demoMode && !fallback.value) { embedding.value = await api.embeddingStatus(doc.value.id).catch(() => undefined);
      if (route.query.chunk && route.query.version) { const chunk = await api.chunk(String(route.query.chunk)); if (chunk.version_id === route.query.version) citedChunk.value = chunk; }
       versions.value = await api.versions(doc.value.id); details.value = await api.metadata(doc.value.id); await loadReviews(); if (chunkVersion.value) await loadChunks(chunkVersion.value) }
  } catch { doc.value = undefined } finally { loading.value = false }
}
onMounted(load)
async function run(action: () => Promise<unknown>) {
  if (runtimeState.demoMode || fallback.value) return ElMessage.warning('演示模式不能保存')
  try { await action(); await load(); ElMessage.success('操作成功') }
  catch (error) { if (error instanceof Error) ElMessage.error(error.message) }
}
async function openEdit() {
  if (!doc.value) return
  if (runtimeState.demoMode || fallback.value) return ElMessage.warning('演示模式不能保存')
  metadata.value = { title: doc.value.title, technology: doc.value.technology, version: doc.value.version, language: doc.value.language, license: doc.value.license, author: '', trust: '' }
  metadata.value.author = details.value.author; metadata.value.trust = details.value.trust
  tagsText.value = doc.value.tags.join(', '); editVisible.value = true
}
async function remove() {
  try {
    await ElMessageBox.confirm('永久删除资料、历史快照、审核记录和全部分块？', '删除资料', { type: 'warning' })
    await api.delete(doc.value!.id); router.push('/documents')
  } catch (error) { if (error instanceof Error) ElMessage.error(error.message) }
}

</script>

<template>
  <div v-loading="loading" class="page-stack">
    <section v-if="embedding" class="surface" style="padding:20px">
      <div v-if="citedChunk"><h3>当前引用证据 · {{ citedChunk.section_path }}</h3><pre style="white-space:pre-wrap">{{ citedChunk.content }}</pre></div>
      <h3>向量索引（与审核、正文索引分别记录）</h3><p v-if="!embedding.vectorReady">数据库尚未安装 pgvector 或未执行 004_pgvector.sql。</p>
      <el-button :loading="embeddingBusy" :disabled="!embedding.vectorReady" @click="vectorIndex">为当前可检索版本生成向量</el-button>
      <p v-for="item in embedding.items" :key="item.profile_id">{{ item.model }} · {{ item.dimensions }} 维 · {{ item.status }} {{ item.error }}</p>
    </section>
    <div class="breadcrumb"><a href="#" @click.prevent="router.push('/documents')">资料管理</a><span>/</span><span>{{ doc?.title ?? '加载中' }}</span></div>
    <DemoNotice :fallback="fallback" text="当前显示演示正文快照，历史版本与重新索引将在后续阶段接通。" />
    <div class="toolbar">
      <button class="secondary-button" @click="router.push('/documents')"><ArrowLeft />返回列表</button>
      <div class="toolbar-group"><button class="secondary-button" :disabled="!doc || runtimeState.demoMode || fallback" @click="openRevision">新增版本</button><button class="secondary-button" :disabled="!doc" @click="run(() => api.reindex(doc!.id))"><RefreshRight />重新索引</button><button class="primary-button" @click="openEdit">编辑元数据</button></div>
    </div>

    <section v-if="linkedVersion" class="surface document-content">
      <h2>本次搜索命中的快照：{{ linkedVersion.title }}</h2>
      <p>版本 {{ linkedVersion.sequence_no }} · {{ linkedVersion.technology }} / {{ linkedVersion.version_range }} · {{ linkedVersion.review_status }}</p>
      <pre style="white-space:pre-wrap;overflow-wrap:anywhere;font:inherit">{{ linkedVersion.content }}</pre>
      <a :href="`/api/documents/${doc!.id}/versions/${linkedVersion.id}/raw`">下载命中版本的原始快照</a>
      <p>下方为当前展示版本的管理信息，可能包含尚未审核的更新。</p>
    </section>
    <el-alert v-else-if="route.query.version && !loading" title="未找到指定历史快照，资料可能已删除。下方仅显示当前资料。" type="warning" />
    <section v-if="doc" class="detail-grid">
      <article class="surface document-content">
        <h1>{{ doc.title }}</h1>
        <pre style="white-space: pre-wrap; overflow-wrap:anywhere; font:inherit">{{ doc.content }}</pre>
        <p>正文按纯文本显示，保留代码缩进；不会执行脚本或代码。</p>
      </article>
      <aside class="page-stack">
        <article class="surface">
          <div class="surface-header"><div><h2>资料信息</h2><p>来源、版本与索引元数据</p></div><StatusTag :status="doc.status" /></div>
          <div class="meta-list">
            <div class="meta-row"><span>技术</span><strong>{{ doc.technology }}</strong></div>
            <div class="meta-row"><span>版本范围</span><span>{{ doc.version }}</span></div>
            <div class="meta-row"><span>来源</span><span>{{ doc.sourceName }}</span></div>
            <div class="meta-row"><span>来源类型</span><span>{{ doc.sourceType }}</span></div>
            <div class="meta-row"><span>语言</span><span>{{ doc.language }}</span></div>
            <div class="meta-row"><span>作者 / 组织</span><span>{{ details.author }}</span></div>
            <div class="meta-row"><span>可信度</span><span>{{ details.trust }}</span></div>
            <div class="meta-row"><span>发布时间</span><span>未知</span></div>
            <div class="meta-row"><span>采集时间</span><span>{{ versions[0]?.collected_at || '未知' }}</span></div>
            <div class="meta-row"><span>许可</span><span>{{ doc.license }}</span></div>
            <div class="meta-row"><span>内容分块</span><span>{{ doc.chunks || '尚未建立' }}</span></div>
            <div class="meta-row"><span>更新时间</span><span>{{ doc.updatedAt }}</span></div>
          </div>
        </article>
        <article class="surface" style="padding: 18px">
          <div class="tags"><i v-for="tag in doc.tags" :key="tag" class="soft-tag">{{ tag }}</i></div>
          <a v-if="doc.url" :href="doc.url" target="_blank" rel="noreferrer" class="secondary-button" style="width: 100%; margin-top: 15px; text-decoration: none"><Link />打开原始来源</a>
        </article>
      </aside>
    </section>
    <section v-if="doc && versions.length" class="surface" style="padding:20px">
      <h2>历史版本与审核</h2>
      <p>最新版本待审核时，检索继续使用上一个已批准版本；版本批准与关键词索引状态分别记录。</p>
      <div class="toolbar-group">
        <el-button @click="run(() => api.archive(doc!.id, doc!.status !== 'ARCHIVED'))">{{ doc.status === 'ARCHIVED' ? '取消归档' : '归档' }}</el-button>
        <el-button type="danger" @click="remove">永久删除</el-button>
      </div>
      <article v-for="(v, i) in versions" :key="v.id" style="margin-top:20px">
        <strong>版本 {{ v.sequence_no }} · {{ v.review_status }} · {{ v.index_status }}</strong>
        <p>{{ v.collected_at }} · {{ v.parser_version }} · SHA-256 {{ v.content_hash }}</p>
        <a :href="`/api/documents/${doc.id}/versions/${v.id}/raw`">下载原始快照</a>
        <el-button v-if="i === 0" @click="run(() => api.review(doc!.id, v.id, true))">批准并建立关键词索引</el-button>
        <el-button v-if="i === 0" @click="run(() => api.review(doc!.id, v.id, false))">拒绝</el-button>
        <el-button @click="loadChunks(v.id)">查看分块</el-button>
        <details><summary>查看此版本正文</summary><pre style="white-space:pre-wrap">{{ v.content }}</pre></details>
      </article>
    </section>
    <section v-if="chunkVersion" v-loading="chunkLoading" class="surface" style="padding:20px">
      <h2>版本分块 · {{ chunkTotal }} 条</h2>
      <p v-if="!chunks.length">此版本尚未批准索引，或没有可显示的分块。</p>
      <article v-for="chunk in chunks" :key="chunk.id"><strong>#{{ chunk.ordinal + 1 }} {{ chunk.section_path || '正文' }}</strong><p>{{ chunk.chunker_version }}</p><pre style="white-space:pre-wrap">{{ chunk.content }}</pre></article>
      <el-pagination :total="chunkTotal" :page-size="20" v-model:current-page="chunkPage" @current-change="(page: number) => loadChunks(chunkVersion, page)" />
    </section>
    <section v-if="doc && !runtimeState.demoMode && !fallback" class="surface" style="padding:20px">
      <h2>审核历史 · {{ reviewTotal }} 条</h2>
      <p v-if="!records.length">尚无审核记录。</p>
      <article v-for="record in records" :key="record.id"><strong>版本 {{ record.sequence_no }} · {{ record.decision === 'APPROVED' ? '批准' : '拒绝' }}</strong><p>{{ record.reviewed_at }} · {{ record.note || '无备注' }}</p></article>
      <el-pagination :total="reviewTotal" :page-size="20" v-model:current-page="reviewPage" @current-change="loadReviews" />
    </section>
    <el-dialog v-model="revisionVisible" title="新增待审核版本" width="min(760px,94vw)">
      <el-alert title="不会覆盖旧快照；新版本通过审核后才进入正式搜索。手工修订会记录在解析器标识中。" type="info" :closable="false" />
      <el-select v-model="revision.format"><el-option label="Markdown" value="MARKDOWN" /><el-option label="TXT" value="TXT" /><el-option label="HTML" value="HTML" /></el-select>
      <el-input v-model="revision.content" type="textarea" :rows="15" />
      <template #footer><el-button type="primary" @click="run(async () => { const result = await api.appendVersion(doc!.id, revision.content, revision.format); if (result.duplicate) ElMessage.info('内容已存在，未新增版本'); revisionVisible = false })">保存为待审核版本</el-button></template>
    </el-dialog>
    <el-dialog v-model="editVisible" title="编辑元数据" width="min(520px,92vw)">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="metadata.title" /></el-form-item>
        <el-form-item label="技术"><el-input v-model="metadata.technology" /></el-form-item>
        <el-form-item label="版本范围"><el-input v-model="metadata.version" /></el-form-item>
        <el-form-item label="语言"><el-input v-model="metadata.language" /></el-form-item>
        <el-form-item label="标签（逗号分隔）"><el-input v-model="tagsText" /></el-form-item>
        <el-form-item label="作者 / 组织"><el-input v-model="metadata.author" /></el-form-item>
        <el-form-item label="许可"><el-input v-model="metadata.license" /></el-form-item>
        <el-form-item label="可信度"><el-input v-model="metadata.trust" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="run(async () => { await api.edit(doc!.id, { ...metadata, tags: tagsText.split(/[,，]/) }); editVisible = false })">保存</el-button></template>
    </el-dialog>
  </div>
</template>
