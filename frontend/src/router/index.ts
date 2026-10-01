import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '概览', subtitle: '知识资产、采集和索引状态' } },
    { path: '/documents', name: 'documents', component: () => import('../views/DocumentsView.vue'), meta: { title: '资料管理', subtitle: '管理来源、版本、许可与索引状态' } },
    { path: '/documents/:id', name: 'document-detail', component: () => import('../views/DocumentDetailView.vue'), meta: { title: '资料详情', subtitle: '查看来源、元数据和正文快照' } },
    { path: '/search', name: 'search', component: () => import('../views/SearchView.vue'), meta: { title: '检索与问答', subtitle: '阶段 2 关键词搜索；阶段 3 接通引用问答' } },
    { path: '/policies', name: 'policies', component: () => import('../views/PoliciesView.vue'), meta: { title: '采集策略', subtitle: '定义主题、来源、频率、审核与预算' } },
    { path: '/tasks', name: 'tasks', component: () => import('../views/TasksView.vue'), meta: { title: '采集任务', subtitle: '跟踪发现、解析、重试和执行结果' } },
    { path: '/reviews', name: 'reviews', component: () => import('../views/ReviewsView.vue'), meta: { title: '待审核资料', subtitle: '确认相关性、版本与许可后再入库' } },
    { path: '/settings', name: 'settings', component: () => import('../views/SettingsView.vue'), meta: { title: '模型与预算', subtitle: '配置 LLM、Embedding 与费用上限' } },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title ?? 'CodeAtlas')} · CodeAtlas`
})

export default router

