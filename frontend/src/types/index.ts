export interface Activity {
  id: string
  type: string
  title: string
  detail: string
  time: string
  status: 'success' | 'warning' | 'danger' | string
}

export interface TrendPoint {
  date: string
  discovered: number
  indexed: number
}

export interface DashboardData {
  documents: number
  chunks: number
  pendingReviews: number
  activePolicies: number
  failedTasks: number
  monthlyCost: number | null
  recentActivities: Activity[]
  ingestionTrend: TrendPoint[]
}

export interface KnowledgeDocument {
  id: string
  title: string
  sourceType: string
  sourceName: string
  url?: string | null
  technology: string
  version: string
  language: string
  tags: string[]
  status: string
  license: string
  updatedAt: string
  chunks: number
  excerpt: string
  content: string
}

export interface CollectionPolicy {
  id: string
  name: string
  topics: string[]
  sources: string[]
  schedule: string
  reviewMode: string
  dailyLimit: number
  monthlyBudget: number
  enabled: boolean
  lastRun: string
  nextRun: string
}

export interface CollectionTask {
  id: string
  policy: string
  source: string
  status: string
  startedAt: string
  discovered: number
  accepted: number
  duration: string
  message: string
}

export interface ReviewItem {
  id: string
  title: string
  source: string
  technology: string
  version: string
  relevance: number
  discoveredAt: string
  reason: string
  changes: string[]
}

export interface Citation {
  id: string
  title: string
  url: string
  section: string
  quote: string
  score: number
}

export interface SearchAnswer {
  resultVersions?: Record<string, string>
  answer: string
  citations: Citation[]
  results: KnowledgeDocument[]
  demo: boolean
}

export interface PagedResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

