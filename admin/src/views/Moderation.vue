<template>
  <div class="view-page">
    <div class="page-header">
      <h1>Content Moderation</h1>
      <p class="page-desc">Review community modules and handle user reports</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats-row">
      <div class="stat-card accent">
        <div class="stat-value">{{ stats.pendingReports }}</div>
        <div class="stat-label">Pending Reports</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-value">{{ stats.pendingModules }}</div>
        <div class="stat-label">Pending Modules</div>
      </div>
      <div class="stat-card success">
        <div class="stat-value">{{ stats.resolvedToday }}</div>
        <div class="stat-label">Resolved Today</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.bannedUsers }}</div>
        <div class="stat-label">Banned Users</div>
      </div>
    </div>

    <!-- Tab Switch -->
    <div class="tab-bar">
      <button v-for="tab in tabs" :key="tab.key"
        :class="['tab-btn', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key">
        {{ tab.label }}
        <span v-if="tab.badge" class="badge">{{ tab.badge }}</span>
      </button>
    </div>

    <!-- Report List -->
    <div v-if="activeTab === 'reports'" class="content-section">
      <div v-if="reports.length === 0" class="empty-state">
        <p>🎉 No pending reports</p>
      </div>
      <div v-for="report in reports" :key="report.id" class="report-card">
        <div class="report-header">
          <span class="report-type" :class="report.reason">{{ report.reason }}</span>
          <span class="report-time">{{ report.created_at }}</span>
        </div>
        <p class="report-detail">{{ report.details || 'No details provided' }}</p>
        <div class="report-meta">
          <span>Reporter: #{{ report.reporter_id }}</span>
          <span>Module: #{{ report.module_id }}</span>
        </div>
        <div class="report-actions">
          <button class="btn btn-sm btn-success" @click="resolveReport(report.id, 'resolved')">
            Approve
          </button>
          <button class="btn btn-sm btn-danger" @click="resolveReport(report.id, 'action_taken')">
            Sanction
          </button>
          <button class="btn btn-sm btn-ghost" @click="resolveReport(report.id, 'dismissed')">
            Dismiss
          </button>
        </div>
      </div>
    </div>

    <!-- Module Approval List -->
    <div v-if="activeTab === 'modules'" class="content-section">
      <div v-if="pendingModules.length === 0" class="empty-state">
        <p>🎉 No pending modules</p>
      </div>
      <div v-for="mod in pendingModules" :key="mod.id" class="module-review-card">
        <div class="module-info">
          <h3>{{ mod.name }}</h3>
          <p>{{ mod.description || 'No description' }}</p>
          <span class="module-author">Author: {{ mod.author_name }}</span>
        </div>
        <div class="module-actions">
          <button class="btn btn-sm btn-success" @click="approveModule(mod.id)">Approve</button>
          <button class="btn btn-sm btn-danger" @click="showRejectDialog(mod)">Reject</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, inject, onMounted } from 'vue'
import { moderationApi } from '../api/index.js'

const showToast = inject('showToast')
const activeTab = ref('reports')
const reports = ref([])
const pendingModules = ref([])

const stats = reactive({
  pendingReports: 0,
  pendingModules: 0,
  resolvedToday: 0,
  bannedUsers: 0,
})

const tabs = computed(() => [
  { key: 'reports', label: 'Reports', badge: stats.pendingReports || null },
  { key: 'modules', label: 'Module Review', badge: stats.pendingModules || null },
])

async function loadReports() {
  try {
    const { data } = await moderationApi.listReports({ status: 'pending' })
    reports.value = data?.data?.reports || []
  } catch {
    reports.value = []
  }
}

async function loadPendingModules() {
  try {
    const { data } = await moderationApi.listPendingModules({ status: 'pending' })
    pendingModules.value = data?.data?.modules || []
  } catch {
    pendingModules.value = []
  }
}

async function loadStats() {
  try {
    const { data } = await moderationApi.stats()
    const d = data?.data || data || {}
    stats.pendingReports = d.pending_reports || 0
    stats.pendingModules = d.pending_modules || 0
    stats.resolvedToday = d.resolved_today || 0
    stats.bannedUsers = d.banned_users || 0
  } catch {
    // fallback — use local counts
  }
}

async function resolveReport(id, resolution) {
  try {
    await moderationApi.resolveReport(id, { resolution })
    showToast('Report handled')
    loadReports()
    loadStats()
  } catch {
    showToast('Operation failed', 'error')
  }
}

async function approveModule(id) {
  try {
    await moderationApi.approveModule(id)
    showToast('Module approved')
    loadPendingModules()
    loadStats()
  } catch {
    showToast('Operation failed', 'error')
  }
}

function showRejectDialog(mod) {
  const reason = prompt(`Enter reason for rejecting "${mod.name}":`)
  if (reason) {
    moderationApi.rejectModule(mod.id, { reason })
      .then(() => { showToast('Module rejected'); loadPendingModules(); loadStats() })
      .catch(() => showToast('Operation failed', 'error'))
  }
}

onMounted(() => {
  loadStats()
  loadReports()
  loadPendingModules()
})
</script>

<style scoped>
.view-page { max-width: 1000px; }
.page-header { margin-bottom: 28px; }
.page-header h1 { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.03em; }
.page-desc { color: var(--text-secondary); font-size: 0.87rem; margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
.stat-card {
  padding: 20px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
}
.stat-card.accent { border-left: 3px solid var(--accent); }
.stat-card.warning { border-left: 3px solid var(--yellow); }
.stat-card.success { border-left: 3px solid var(--green); }
.stat-value { font-size: 1.5rem; font-weight: 700; }
.stat-label { font-size: 0.78rem; color: var(--text-secondary); margin-top: 4px; }

.tab-bar { display: flex; gap: 4px; margin-bottom: 20px; padding: 4px; background: var(--bg-card); border-radius: var(--r-sm); border: 1px solid var(--border); }
.tab-btn {
  padding: 8px 16px; border: none; background: transparent; border-radius: var(--r-xs);
  color: var(--text-secondary); cursor: pointer; font-family: var(--font); font-size: 0.85rem;
  display: flex; align-items: center; gap: 6px; transition: all var(--t-fast);
}
.tab-btn:hover { color: var(--text-primary); }
.tab-btn.active { background: var(--accent); color: #fff; }
.badge {
  background: var(--red); color: #fff; font-size: 0.7rem; padding: 1px 6px;
  border-radius: 10px; font-weight: 600; min-width: 18px; text-align: center;
}

.empty-state { text-align: center; padding: 60px 20px; color: var(--text-secondary); font-size: 1rem; }

.report-card, .module-review-card {
  padding: 18px 20px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
  margin-bottom: 12px; transition: border-color var(--t-fast);
}
.report-card:hover, .module-review-card:hover { border-color: var(--accent); }
.report-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.report-type { font-size: 0.78rem; padding: 3px 8px; border-radius: var(--r-xs); background: var(--red-subtle); color: var(--red); font-weight: 600; }
.report-time { font-size: 0.75rem; color: var(--text-muted); }
.report-detail { font-size: 0.87rem; color: var(--text-secondary); margin-bottom: 8px; }
.report-meta { font-size: 0.78rem; color: var(--text-muted); display: flex; gap: 16px; margin-bottom: 12px; }
.report-actions { display: flex; gap: 8px; }

.module-info h3 { font-size: 1rem; font-weight: 600; margin-bottom: 4px; }
.module-info p { font-size: 0.87rem; color: var(--text-secondary); margin-bottom: 6px; }
.module-author { font-size: 0.78rem; color: var(--text-muted); }
.module-actions { display: flex; gap: 8px; margin-top: 12px; }

.btn { padding: 7px 14px; border: none; border-radius: var(--r-xs); font-family: var(--font); font-size: 0.82rem; cursor: pointer; transition: all var(--t-fast); }
.btn-sm { padding: 5px 12px; font-size: 0.8rem; }
.btn-success { background: var(--green); color: #fff; }
.btn-success:hover { filter: brightness(1.1); }
.btn-danger { background: var(--red); color: #fff; }
.btn-danger:hover { filter: brightness(1.1); }
.btn-ghost { background: transparent; border: 1px solid var(--border); color: var(--text-secondary); }
.btn-ghost:hover { border-color: var(--text-primary); color: var(--text-primary); }
</style>
