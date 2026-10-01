<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api, type RagAnswer } from '../services/api'
const query=ref('虚拟线程')
const technology=ref(''),version=ref(''),mode=ref('KEYWORD'),includeDemo=ref(false)
const filters=ref({sourceType:'',language:'',from:'',to:''})
const loading=ref(false),answer=ref<RagAnswer>(),error=ref('')
async function ask(generate: boolean) {
 loading.value=true;error.value='';answer.value=undefined
 const payload={query:query.value,technologies:technology.value?[technology.value]:[],version:version.value,searchMode:mode.value,includeDemo:includeDemo.value,...filters.value}
 try{answer.value=generate?await api.rag(payload):{answer:'以下为检索证据，未调用生成模型。',status:'RETRIEVED',results:await api.retrieve(payload),citations:[],demo:includeDemo.value,searchMode:mode.value}}
 catch(e){error.value=e instanceof Error?e.message:'请求失败';ElMessage.error(error.value)}finally{loading.value=false}
}
</script>
<template>
 <div class="page-stack">
  <el-alert title="只检索已批准并完成正文索引的资料。语义/混合检索还需成功生成向量；引用校验不会验证每个结论的语义正确性。" type="info" :closable="false" />
  <article class="surface ask-panel">
   <h3>搜索与引用问答</h3><el-input v-model="query" type="textarea" :rows="3" placeholder="输入问题，注明技术版本" />
   <div class="ask-options"><el-input v-model="technology" placeholder="技术精确名称，可留空" /><el-input v-model="version" placeholder="版本精确匹配，例如 21" /><el-select v-model="mode"><el-option label="关键词（无需模型）" value="KEYWORD" /><el-option label="语义" value="SEMANTIC" /><el-option label="混合 RRF" value="HYBRID" /></el-select></div>
   <div class="ask-options"><el-select v-model="filters.sourceType" clearable placeholder="来源"><el-option label="网页" value="WEB_PAGE" /><el-option label="文件" value="FILE" /><el-option label="笔记" value="MANUAL_NOTE" /></el-select><el-input v-model="filters.language" placeholder="语言 zh / en" /><label>开始日 UTC<input v-model="filters.from" type="date" /></label><label>结束日 UTC<input v-model="filters.to" type="date" /></label></div>
   <el-checkbox v-model="includeDemo">显式包含演示种子（回答标记为演示）</el-checkbox>
   <div class="ask-actions"><el-button :loading="loading" @click="ask(false)">检索证据</el-button><el-button type="primary" :loading="loading" @click="ask(true)">生成引用回答</el-button></div>
  </article>
  <el-alert v-if="error" :title="error" type="error" :closable="false" />
  <article v-if="answer" class="surface answer-panel">
   <el-tag :type="answer.demo?'warning':'info'">{{ answer.demo?'包含演示资料':'知识库已有资料' }} · {{ answer.status }} · {{ answer.searchMode }}</el-tag>
   <div class="answer-body" style="white-space:pre-wrap">{{ answer.answer }}</div>
   <div v-for="citation in answer.citations" :key="citation.number" class="citation"><router-link :to="{path:`/documents/${citation.documentId}`,query:{version:citation.versionId,chunk:citation.chunkId}}">[{{ citation.number }}] {{ citation.title }} · {{ citation.section }}</router-link><blockquote>{{ citation.quote }}</blockquote><a v-if="citation.url" :href="citation.url" target="_blank" rel="noreferrer">原始来源</a></div>
   <el-empty v-if="!answer.results.length" description="没有符合条件的证据，请导入并审核相关资料" />
   <details v-for="hit in answer.results" :key="hit.chunkId" class="citation"><summary>{{ hit.title }} · {{ hit.technology }} {{ hit.version }} · {{ hit.section }} · 排序分 {{ hit.score.toFixed(4) }}</summary><router-link :to="{path:`/documents/${hit.documentId}`,query:{version:hit.versionId,chunk:hit.chunkId}}">定位资料版本</router-link><pre style="white-space:pre-wrap">{{ hit.content }}</pre></details>
  </article>
 </div>
</template>
