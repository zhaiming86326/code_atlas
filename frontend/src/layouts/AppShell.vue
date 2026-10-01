<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { runtimeState } from '../services/api'
import {
  ArrowLeft, ArrowRight, Bell, Collection, Cpu, DataAnalysis, DataLine,
  DocumentChecked, MagicStick, Menu, Search, Setting, Tickets, Close,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const mobileOpen = ref(false)

const menu = [
  { path: '/', label: '概览', icon: DataAnalysis },
  { path: '/documents', label: '资料管理', icon: Collection },
  { path: '/search', label: '检索与问答', icon: Search },
  { type: 'section', label: '主动采集' },
  { path: '/policies', label: '采集策略', icon: DataLine },
  { path: '/tasks', label: '采集任务', icon: Tickets },
  { path: '/reviews', label: '待审核资料', icon: DocumentChecked },
  { type: 'section', label: '系统' },
  { path: '/settings', label: '模型与预算', icon: Setting },
]

const title = computed(() => String(route.meta.title ?? 'CodeAtlas'))
const subtitle = computed(() => String(route.meta.subtitle ?? ''))

function navigate(path?: string) {
  if (!path) return
  mobileOpen.value = false
  router.push(path)
}
</script>

<template>
  <div class="app-shell" :class="{ 'sidebar-collapsed': collapsed }">
    <div v-if="mobileOpen" class="mobile-mask" @click="mobileOpen = false" />
    <aside class="sidebar" :class="{ 'mobile-open': mobileOpen }">
      <div class="brand">
        <div class="brand-mark"><Cpu /></div>
        <div v-if="!collapsed" class="brand-copy">
          <strong>CodeAtlas</strong>
          <span>代码图谱</span>
        </div>
        <button class="mobile-close" aria-label="关闭菜单" @click="mobileOpen = false"><Close /></button>
      </div>

      <nav class="nav-list">
        <template v-for="item in menu" :key="item.label">
          <div v-if="item.type === 'section' && !collapsed" class="nav-section">{{ item.label }}</div>
          <button
            v-else-if="item.path"
            class="nav-item"
            :class="{ active: route.path === item.path || (item.path === '/documents' && route.path.startsWith('/documents/')) }"
            :title="collapsed ? item.label : undefined"
            @click="navigate(item.path)"
          >
            <component :is="item.icon" class="nav-icon" />
            <span v-if="!collapsed">{{ item.label }}</span>
          </button>
        </template>
      </nav>

      <div class="sidebar-bottom">
        <div v-if="!collapsed" class="workspace-card">
          <div class="workspace-icon"><MagicStick /></div>
          <div><strong>个人知识空间</strong><span>{{ runtimeState.demoMode || runtimeState.demoFallback ? '阶段 1 · 演示模式' : '阶段 2 · 手动入库' }}</span></div>
        </div>
        <button class="collapse-button" @click="collapsed = !collapsed">
          <component :is="collapsed ? ArrowRight : ArrowLeft" />
          <span v-if="!collapsed">收起导航</span>
        </button>
      </div>
    </aside>

    <section class="main-area">
      <header class="topbar">
        <button class="mobile-menu" aria-label="打开菜单" @click="mobileOpen = true"><Menu /></button>
        <div class="page-heading">
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <div class="top-actions">
          <button class="icon-button" aria-label="通知"><Bell /><span class="notification-dot" /></button>
          <div class="profile"><span>CA</span><div><strong>管理员</strong><small>本地工作区</small></div></div>
        </div>
      </header>
      <main class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </main>
    </section>
  </div>
</template>
