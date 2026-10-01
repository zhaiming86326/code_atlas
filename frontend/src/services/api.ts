import type { CollectionPolicy, CollectionTask, DashboardData, KnowledgeDocument, PagedResponse, ReviewItem, SearchAnswer } from '../types'
import { demoAnswer, demoDashboard, demoDocuments, demoPolicies, demoReviews, demoTasks } from './demoData'
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'

export const runtimeState = reactive({ demoFallback: false, demoMode: false, error: '' })

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: { ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...(init?.headers ?? {}) },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.message || `HTTP ${response.status}`)
  }
  runtimeState.demoFallback = false
  return response.status === 204 || response.headers.get('content-length') === '0' ? undefined as T : response.text().then(text => text ? JSON.parse(text) : undefined)
}

async function withFallback<T>(loader: () => Promise<T>, fallback: T): Promise<T> {
  try {
    const health = await request<{ demoMode: boolean }>('/api/health')
    runtimeState.demoMode = health.demoMode
    runtimeState.error = ''
    return await loader()
  } catch (error) {
    runtimeState.error = error instanceof Error ? error.message : '请求失败'
    if (import.meta.env.VITE_DEMO_FALLBACK !== 'true') {
      ElMessage.error(runtimeState.error)
      throw error
    }
    runtimeState.demoFallback = true
    await new Promise((resolve) => setTimeout(resolve, 180))
    return fallback
  }
}

export interface ModelSettings { role: string; baseUrl: string; model: string; dimensions: number; revision: string; inputPricePerMillion: number | null; outputPricePerMillion: number | null; priceBasis: string; keyConfigured: boolean }
export interface ModelState { vectorReady: boolean; models: ModelSettings[]; calls: { calls: number; failed: number; unknown_cost_calls: number; known_cost: number | null; items: { id: string; role: string; model: string; status: string; estimated_cost: number | null; error: string | null }[] } }
export interface RagHit { chunkId: string; documentId: string; versionId: string; title: string; url: string; technology: string; version: string; section: string; content: string; score: number }
export interface RagAnswer { answer: string; status: string; demo: boolean; searchMode: string; results: RagHit[]; citations: { number: number; documentId: string; versionId: string; chunkId: string; title: string; url: string; section: string; quote: string }[] }
export const api = {
  models: () => request<ModelState>('/api/models'),
  saveModels: (models: ModelSettings[]) => request<ModelState>('/api/models', { method: 'PUT', body: JSON.stringify(models) }),
  rag: (payload: unknown) => request<RagAnswer>('/api/rag/answer', { method: 'POST', body: JSON.stringify(payload) }),
  retrieve: (payload: unknown) => request<RagHit[]>('/api/rag/retrieve', { method: 'POST', body: JSON.stringify(payload) }),
  chunk: (id: string) => request<ContentChunk>(`/api/chunks/${id}`),
  embeddingIndex: (id: string) => request<unknown>(`/api/documents/${id}/embedding-index`, { method: 'POST' }),
  embeddingStatus: (id: string) => request<{ vectorReady: boolean; items: { profile_id: string; model: string; dimensions: number; status: string; error: string }[] }>(`/api/documents/${id}/embedding-index`),
  dashboard: () => withFallback(() => request<DashboardData>('/api/dashboard'), demoDashboard),
  documents: (query = '', technology = '', status = '', page = 1) => withFallback(() => request<PagedResponse<KnowledgeDocument>>(`/api/documents?${new URLSearchParams({ query, technology, status, page: String(page), pageSize: '20' })}`), { items: demoDocuments, page: 1, pageSize: 20, total: demoDocuments.length }),
  document: (id: string) => withFallback(() => request<KnowledgeDocument>(`/api/documents/${id}`), demoDocuments.find((item) => item.id === id) ?? demoDocuments[0]),
  policies: () => withFallback(() => request<CollectionPolicy[]>('/api/collection-policies'), demoPolicies),
  tasks: () => withFallback(() => request<CollectionTask[]>('/api/collection-tasks'), demoTasks),
  reviews: () => withFallback(() => request<ReviewItem[]>('/api/reviews'), demoReviews),
  answer: (query: string, technology: string, version: string, filters: Record<string, string> = {}) => withFallback(
    () => request<SearchAnswer>('/api/search/answer', { method: 'POST', body: JSON.stringify({ query, technologies: technology ? [technology] : [], version, ...filters, searchMode: runtimeState.demoMode ? 'HYBRID' : 'KEYWORD' }) }),
    demoAnswer,
  ),
  importUrl: (url: string, metadata: unknown) => request<{ document: KnowledgeDocument; duplicate: boolean }>('/api/imports/url', { method: 'POST', body: JSON.stringify({ url, metadata }) }),
  importNote: (title: string, content: string, metadata: unknown) => request<{ document: KnowledgeDocument; duplicate: boolean }>('/api/imports/note', { method: 'POST', body: JSON.stringify({ title, content, metadata }) }),
  importFile: (file: File, metadata: unknown) => {
    const form = new FormData(); form.append('file', file); form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }))
    return request<{ document: KnowledgeDocument; duplicate: boolean }>('/api/imports/file', { method: 'POST', body: form })
  },
  versions: (id: string) => request<DocumentVersion[]>(`/api/documents/${id}/versions`),
  version: (id: string, versionId: string) => request<DocumentVersion>(`/api/documents/${id}/versions/${versionId}`),
  appendVersion: (id: string, content: string, format: string) => request<{ document: KnowledgeDocument; duplicate: boolean }>(`/api/documents/${id}/versions`, { method: 'POST', body: JSON.stringify({ content, format }) }),
  chunks: (id: string, versionId: string, page = 1) => request<PagedResponse<ContentChunk>>(`/api/documents/${id}/versions/${versionId}/chunks?page=${page}`),
  reviewRecords: (documentId: string, page = 1) => request<PagedResponse<ReviewRecord>>(`/api/review-records?documentId=${documentId}&page=${page}`),
  metadata: (id: string) => request<{ author: string; trust: string }>(`/api/documents/${id}/metadata`),
  edit: (id: string, metadata: unknown) => request<KnowledgeDocument>(`/api/documents/${id}`, { method: 'PUT', body: JSON.stringify(metadata) }),
  review: (id: string, versionId: string, approved: boolean) => request<KnowledgeDocument>(`/api/documents/${id}/review`, { method: 'POST', body: JSON.stringify({ versionId, approved }) }),
  reindex: (id: string) => request<KnowledgeDocument>(`/api/documents/${id}/reindex`, { method: 'POST' }),
  archive: (id: string, archived: boolean) => request<void>(`/api/documents/${id}/archive?archived=${archived}`, { method: 'POST' }),
  delete: (id: string) => request<void>(`/api/documents/${id}`, { method: 'DELETE' }),
}

export interface DocumentVersion { id: string; sequence_no: number; title: string; technology: string; version_range: string; language: string; content_hash: string; raw_hash: string; parser_version: string; review_status: string; index_status: string; collected_at: string; content: string }
export interface ContentChunk { id: string; version_id: string; ordinal: number; section_path: string; content: string; chunker_version: string }
export interface ReviewRecord { id: string; version_id: string; sequence_no: number; decision: string; note: string; reviewed_at: string }

