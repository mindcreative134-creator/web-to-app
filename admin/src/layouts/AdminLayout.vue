<template>
  <div class="app-shell">
    <!-- Sidebar -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <div class="brand">
          <div class="brand-icon">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
          </div>
          <transition name="fade-text">
            <span v-if="!sidebarCollapsed" class="brand-name">WebToApp Pro</span>
          </transition>
        </div>
        <button class="btn-icon collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
      </div>

      <nav class="sidebar-nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item"
          :class="{ active: $route.path === item.path || ($route.path.startsWith(item.path) && item.path !== '/') }" @click="closeMobile">
          <span class="nav-icon" v-html="item.svg"></span>
          <transition name="fade-text">
            <span v-if="!sidebarCollapsed" class="nav-label">{{ item.label }}</span>
          </transition>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="nav-item logout-btn" @click="logout">
          <span class="nav-icon"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg></span>
          <transition name="fade-text">
            <span v-if="!sidebarCollapsed" class="nav-label">Logout</span>
          </transition>
        </button>
        <transition name="fade-text">
          <div v-if="!sidebarCollapsed" class="sidebar-version">© 2026 WebToApp Pro</div>
        </transition>
      </div>
    </aside>

    <!-- Main -->
    <main class="main-content slide-up">
      <router-view />
    </main>

    <!-- Toast -->
    <transition name="toast">
      <div v-if="toast.visible" :class="['toast', `toast-${toast.type}`]">
        {{ toast.message }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, provide } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const sidebarCollapsed = ref(false)

const toast = reactive({ visible: false, message: '', type: 'success' })
let toastTimer
function showToast(message, type = 'success') {
  toast.message = message; toast.type = type; toast.visible = true
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.visible = false }, 3000)
}
provide('showToast', showToast)

function logout() {
  localStorage.clear()
  router.push('/login')
}
function closeMobile() {}

const svgDashboard = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>'
const svgUsers = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>'
const svgKey = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.78 7.78 5.5 5.5 0 017.78-7.78zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"/></svg>'
const svgMegaphone = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>'
const svgPackage = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M16.5 9.4l-9-5.19"/><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>'
const svgSettings = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/></svg>'

const svgShield = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>'
const svgBell = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/><line x1="12" y1="2" x2="12" y2="4"/></svg>'
const svgTool = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>'
const svgBrain = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a5 5 0 00-5 5c0 .74.16 1.43.45 2.06A5 5 0 004 14c0 1.4.57 2.66 1.5 3.57A4 4 0 004 20a4 4 0 004 2h4a4 4 0 004-2 4 4 0 00-1.5-2.43A5 5 0 0016 14a5 5 0 00-3.45-4.94c.29-.63.45-1.32.45-2.06a5 5 0 00-1-3"/></svg>'

const svgStore = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>'

const svgCloud = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M18 10h-1.26A8 8 0 109 20h9a5 5 0 000-10z"/></svg>'

const svgChart = '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>'

const navItems = [
  { path: '/', label: 'Overview', svg: svgDashboard },
  { path: '/analytics', label: 'Analytics', svg: svgChart },
  { path: '/users', label: 'Users', svg: svgUsers },
  { path: '/activation', label: 'Activation', svg: svgKey },
  { path: '/projects', label: 'Projects', svg: svgCloud },
  { path: '/announcements', label: 'Announcements', svg: svgMegaphone },
  { path: '/versions', label: 'Versions', svg: svgPackage },
  { path: '/config', label: 'Configs', svg: svgSettings },
  { path: '/store', label: 'Store', svg: svgStore },
  { path: '/moderation', label: 'Moderation', svg: svgShield },
  { path: '/push', label: 'Push', svg: svgBell },
  { path: '/maintenance', label: 'Maintenance', svg: svgTool },
  { path: '/intelligence', label: 'Security', svg: svgBrain },
]
</script>

<style scoped>
.app-shell { display: flex; min-height: 100vh; }

/* ─── Sidebar ─── */
.sidebar {
  width: 220px; min-height: 100vh; position: fixed; left: 0; top: 0; z-index: 50;
  background: var(--bg-sidebar);
  backdrop-filter: blur(40px) saturate(1.6);
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
  transition: width 0.4s var(--ease-out);
}
.sidebar.collapsed { width: 64px; }

.sidebar-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 16px; border-bottom: 1px solid var(--border);
}
.brand { display: flex; align-items: center; gap: 10px; overflow: hidden; }
.brand-icon {
  width: 34px; height: 34px; min-width: 34px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--accent), #a78bfa);
  border-radius: var(--r-sm); color: #fff;
}
.brand-name { font-weight: 700; font-size: 0.95rem; white-space: nowrap; letter-spacing: -0.02em; }
.collapse-btn { transition: transform var(--t-normal); }
.sidebar.collapsed .collapse-btn { transform: rotate(180deg); }

.sidebar-nav { flex: 1; padding: 12px 8px; display: flex; flex-direction: column; gap: 2px; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 9px 12px; border-radius: var(--r-sm);
  color: var(--text-secondary); text-decoration: none;
  transition: all var(--t-fast); cursor: pointer; border: none; background: none;
  font-family: var(--font); font-size: 0.87rem; width: 100%;
}
.nav-item:hover { background: rgba(255,255,255,0.04); color: var(--text-primary); }
.nav-item.active { background: var(--accent-subtle); color: var(--accent-hover); }
.nav-item.active .nav-icon { color: var(--accent); }
.nav-icon {
  width: 18px; height: 18px; min-width: 18px;
  display: flex; align-items: center; justify-content: center;
  transition: color var(--t-fast);
}
.nav-label { white-space: nowrap; overflow: hidden; }

.sidebar-footer { padding: 8px; border-top: 1px solid var(--border); }
.logout-btn { color: var(--text-muted); }
.logout-btn:hover { color: var(--red); background: var(--red-subtle); }
.sidebar-version { font-size: 0.68rem; color: var(--text-muted); text-align: center; padding: 8px 0 4px; opacity: 0.5; }

/* ─── Main ─── */
.main-content {
  flex: 1; margin-left: 220px; padding: 32px 36px;
  transition: margin-left 0.4s var(--ease-out);
  min-height: 100vh;
}
.sidebar.collapsed ~ .main-content { margin-left: 64px; }

/* ─── Transitions ─── */
.fade-text-enter-active { transition: opacity 0.2s ease, width 0.2s ease; }
.fade-text-leave-active { transition: opacity 0.15s ease; }
.fade-text-enter-from, .fade-text-leave-to { opacity: 0; }
.toast-enter-active { animation: slideInRight 0.45s var(--ease-spring); }
.toast-leave-active { animation: slideInRight 0.3s var(--ease-out) reverse; }
</style>
