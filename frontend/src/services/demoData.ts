import type { CollectionPolicy, CollectionTask, DashboardData, KnowledgeDocument, ReviewItem, SearchAnswer } from '../types'

export const demoDashboard: DashboardData = {
  documents: 1284,
  chunks: 18642,
  pendingReviews: 7,
  activePolicies: 4,
  failedTasks: 1,
  monthlyCost: 8.37,
  recentActivities: [
    { id: 'a1', type: 'review', title: '发现 Redis 8.2 发布说明', detail: '等待确认版本相关性', time: '12 分钟前', status: 'warning' },
    { id: 'a2', type: 'index', title: 'Spring Boot 文档完成索引', detail: '新增 184 个分块', time: '48 分钟前', status: 'success' },
    { id: 'a3', type: 'collect', title: 'Java 官方来源检查完成', detail: '发现 3 项，接受 2 项', time: '2 小时前', status: 'success' },
    { id: 'a4', type: 'error', title: 'Vue Blog 抓取失败', detail: '上游返回 429，已安排重试', time: '3 小时前', status: 'danger' },
  ],
  ingestionTrend: [
    { date: '09-24', discovered: 18, indexed: 12 }, { date: '09-25', discovered: 24, indexed: 19 },
    { date: '09-26', discovered: 15, indexed: 14 }, { date: '09-27', discovered: 31, indexed: 22 },
    { date: '09-28', discovered: 20, indexed: 18 }, { date: '09-29', discovered: 42, indexed: 35 },
    { date: '09-30', discovered: 28, indexed: 21 },
  ],
}

export const demoDocuments: KnowledgeDocument[] = [
  { id: 'doc-001', title: 'Spring Boot 3.5 Reference Documentation', sourceType: 'OFFICIAL_DOC', sourceName: 'Spring', url: 'https://docs.spring.io/spring-boot/', technology: 'Spring Boot', version: '3.5.x', language: 'en', tags: ['Java', 'Web', '官方文档'], status: 'INDEXED', license: 'Apache-2.0', updatedAt: '2026-09-29 16:20', chunks: 184, excerpt: 'Spring Boot makes it easy to create stand-alone, production-grade Spring applications.', content: '# Spring Boot 3.5\n\n本文档介绍自动配置、外部化配置、Web 开发、数据访问、测试和生产运维。\n\n## Externalized Configuration\n\nSpring Boot 支持 properties、YAML、环境变量和命令行参数。' },
  { id: 'doc-002', title: 'PostgreSQL 16：向量检索与全文检索组合实践', sourceType: 'MANUAL_NOTE', sourceName: '个人笔记', url: null, technology: 'PostgreSQL', version: '16', language: 'zh', tags: ['pgvector', '混合检索', 'RAG'], status: 'INDEXED', license: 'Private', updatedAt: '2026-09-28 21:45', chunks: 26, excerpt: '使用 RRF 合并 ts_rank 与向量相似度结果，兼顾精确词与语义召回。', content: '# PostgreSQL 混合检索\n\n精确的类名、错误码和 API 名称使用全文检索召回，自然语言问题使用向量召回，最后通过 RRF 合并排名。' },
  { id: 'doc-003', title: 'Redis 8.2 Release Notes', sourceType: 'GITHUB_RELEASE', sourceName: 'redis/redis', url: 'https://github.com/redis/redis/releases', technology: 'Redis', version: '8.2', language: 'en', tags: ['Release', '升级', '缓存'], status: 'PENDING_REVIEW', license: 'BSD-3-Clause', updatedAt: '2026-09-30 08:12', chunks: 0, excerpt: 'New release candidate discovered by the Redis tracking policy.', content: '尚未批准，批准后才会解析正文并建立正式索引。' },
  { id: 'doc-004', title: 'Vue 3 Composition API 常见陷阱', sourceType: 'WEB_PAGE', sourceName: 'Vue.js Blog', url: 'https://vuejs.org/guide/', technology: 'Vue', version: '3.5', language: 'zh', tags: ['前端', 'Composition API'], status: 'INDEXED', license: 'MIT', updatedAt: '2026-09-27 10:30', chunks: 41, excerpt: '响应式解构、watch 依赖和组件生命周期中的常见问题。', content: '# Composition API\n\n保留响应式引用时使用 toRefs；watchEffect 适合自动跟踪同步执行期间读取的依赖。' },
  { id: 'doc-005', title: 'Java 21 Virtual Threads Guide', sourceType: 'OFFICIAL_DOC', sourceName: 'OpenJDK', url: 'https://openjdk.org/jeps/444', technology: 'Java', version: '21', language: 'en', tags: ['并发', '虚拟线程', 'JEP 444'], status: 'INDEXED', license: 'GPL-2.0', updatedAt: '2026-09-26 14:10', chunks: 33, excerpt: 'Virtual threads are lightweight threads that reduce the effort of writing concurrent applications.', content: '# Virtual Threads\n\n虚拟线程适合大量以阻塞 I/O 为主的并发任务，不用于让 CPU 密集型任务运行得更快。' },
]

export const demoPolicies: CollectionPolicy[] = [
  { id: 'policy-1', name: 'Java 后端核心栈', topics: ['Java 21', 'Spring Boot 3.x', 'PostgreSQL', 'Redis'], sources: ['官方文档', 'GitHub Releases', 'RSS'], schedule: '每天 08:00', reviewMode: 'MANUAL', dailyLimit: 50, monthlyBudget: 12, enabled: true, lastRun: '今天 08:04', nextRun: '明天 08:00' },
  { id: 'policy-2', name: '前端框架动态', topics: ['Vue 3', 'TypeScript', 'Vite'], sources: ['官方博客', 'GitHub Releases'], schedule: '每周一 09:00', reviewMode: 'MANUAL', dailyLimit: 30, monthlyBudget: 6, enabled: true, lastRun: '周一 09:02', nextRun: '下周一 09:00' },
  { id: 'policy-3', name: 'AI 工程与 RAG', topics: ['Spring AI', 'pgvector', 'Reranker'], sources: ['指定网页', 'RSS'], schedule: '每 12 小时', reviewMode: 'MANUAL', dailyLimit: 20, monthlyBudget: 10, enabled: false, lastRun: '09-26 20:00', nextRun: '已暂停' },
]

export const demoTasks: CollectionTask[] = [
  { id: 'task-2491', policy: 'Java 后端核心栈', source: 'GitHub Releases', status: 'SUCCEEDED', startedAt: '今天 08:00', discovered: 12, accepted: 7, duration: '2m 18s', message: '完成' },
  { id: 'task-2490', policy: 'Java 后端核心栈', source: 'Spring 官方文档', status: 'SUCCEEDED', startedAt: '今天 08:00', discovered: 8, accepted: 2, duration: '1m 46s', message: '正文哈希发现 2 项变化' },
  { id: 'task-2489', policy: '前端框架动态', source: 'Vue Blog', status: 'RETRYING', startedAt: '昨天 09:00', discovered: 0, accepted: 0, duration: '32s', message: 'HTTP 429，将在 15 分钟后重试' },
  { id: 'task-2488', policy: 'AI 工程与 RAG', source: '指定网页', status: 'PAUSED', startedAt: '09-26 20:00', discovered: 16, accepted: 9, duration: '4m 09s', message: '策略已暂停' },
]

export const demoReviews: ReviewItem[] = [
  { id: 'review-1', title: 'Redis 8.2 Release Notes', source: 'github.com/redis/redis', technology: 'Redis', version: '8.2', relevance: 0.96, discoveredAt: '12 分钟前', reason: '命中版本发布、性能与破坏性变更规则', changes: ['新增命令行为说明', '弃用旧配置项', '复制性能改进'] },
  { id: 'review-2', title: 'Spring Boot 3.5.7 available now', source: 'spring.io/blog', technology: 'Spring Boot', version: '3.5.7', relevance: 0.91, discoveredAt: '1 小时前', reason: '命中关注版本 3.x 和官方来源', changes: ['依赖升级', '安全修复', '配置元数据修正'] },
  { id: 'review-3', title: 'PostgreSQL 18 Query Optimizer Deep Dive', source: 'example.dev', technology: 'PostgreSQL', version: '18', relevance: 0.78, discoveredAt: '3 小时前', reason: '主题相关，但版本不在当前 16—17 范围', changes: ['优化器变化', '性能测试'] },
]

export const demoAnswer: SearchAnswer = {
  answer: '根据当前知识库，Spring Boot 3.5 支持通过 properties、YAML、环境变量和命令行参数提供外部化配置。配置有明确的属性源优先级，部署时可以使用环境变量覆盖包内默认值。对于包含密钥的配置，应从运行环境注入，避免写入源码。[1]\n\n如果要在容器中部署，建议把非敏感默认值保留在 application.yml，将数据库密码和模型密钥通过环境变量传入。',
  citations: [{ id: 'cite-1', title: 'Spring Boot 3.5 Reference Documentation', url: 'https://docs.spring.io/spring-boot/', section: 'Externalized Configuration', quote: 'Spring Boot lets you externalize your configuration so that you can work with the same application code in different environments.', score: 0.94 }],
  results: [demoDocuments[0]],
  demo: true,
}

