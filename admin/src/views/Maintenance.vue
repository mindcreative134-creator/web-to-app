<template>
  <div class="view-page">
    <div class="page-header">
      <h1>System Maintenance</h1>
      <p class="page-desc">Health checks, cache management, and database maintenance</p>
    </div>

    <!-- System Health -->
    <div class="stats-row">
      <div class="stat-card" :class="health.status === 'healthy' ? 'success' : 'warning'">
        <div class="stat-value">{{ health.status || '...' }}</div>
        <div class="stat-label">System Status</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ health.uptime || '-' }}</div>
        <div class="stat-label">Uptime</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ health.dbConnections ?? '-' }}</div>
        <div class="stat-label">DB Connections</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ health.memoryUsage || '-' }}</div>
        <div class="stat-label">Memory Usage</div>
      </div>
    </div>

    <!-- Maintenance Actions -->
    <div class="actions-grid">
      <div class="action-card">
        <div class="action-icon">🧹</div>
        <h3>Clear Cache</h3>
        <p>Clear all server-side cached data</p>
        <button class="btn btn-primary" @click="clearCache" :disabled="running.cache">
          {{ running.cache ? 'Clearing...' : 'Execute Clear' }}
        </button>
      </div>

      <div class="action-card">
        <div class="action-icon">💾</div>
        <h3>Database Backup</h3>
        <p>Create a full backup of the current database</p>
        <button class="btn btn-primary" @click="dbBackup" :disabled="running.backup">
          {{ running.backup ? 'Backing up...' : 'Create Backup' }}
        </button>
      </div>

      <div class="action-card">
        <div class="action-icon">🗑️</div>
        <h3>Data Cleanup</h3>
        <p>Cleanup expired tokens, logs, and temporary files</p>
        <button class="btn btn-warning" @click="cleanup" :disabled="running.cleanup">
          {{ running.cleanup ? 'Cleaning...' : 'Execute Cleanup' }}
        </button>
      </div>

      <div class="action-card">
        <div class="action-icon">🔄</div>
        <h3>Health Check</h3>
        <p>Check the health status of all system components</p>
        <button class="btn btn-ghost" @click="healthCheck" :disabled="running.health">
          {{ running.health ? 'Checking...' : 'Re-check' }}
        </button>
      </div>
    </div>

    <!-- Backup Management -->
    <div class="card mb-24">
      <div class="card-header-row">
        <h2 class="card-title">💾 Backup Management</h2>
        <span v-if="backupData.total_size" class="text-xs text-muted">
          Total {{ backupData.count }} backups · {{ backupData.total_size }}
        </span>
      </div>

      <div v-if="loadingBackups" class="empty-hint">Loading...</div>
      <div v-else-if="!backups.length" class="empty-hint">No backup files found</div>
      <div v-else class="backup-list">
        <div class="backup-item" v-for="b in backups" :key="b.filename">
          <div class="backup-info">
            <span class="backup-name">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              {{ b.filename }}
            </span>
            <span class="backup-meta">{{ b.size }} · {{ b.created_at }}</span>
          </div>
          <div class="backup-actions">
            <button class="btn btn-ghost btn-xs" @click="downloadBackup(b.filename)" title="Download">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              Download
            </button>
            <button class="btn btn-danger btn-xs" @click="deleteBackup(b.filename)" title="Delete">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
              Delete
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- System Stats -->
    <div class="card" v-if="systemStats">
      <h2 class="card-title">📊 System Statistics</h2>
      <div class="stats-list">
        <div class="stats-item" v-for="(val, key) in systemStats" :key="key">
          <span class="stats-key">{{ key }}</span>
          <span class="stats-val">{{ val }}</span>
        </div>
      </div>
    </div>

    <!-- Data Export -->
    <div class="card mt-24" style="margin-top:24px">
      <h2 class="card-title">📥 Data Export</h2>
      <div class="export-grid">
        <div class="action-card">
          <div class="action-icon">👤</div>
          <h3>Export User Data</h3>
          <p>Export all user account info to CSV file</p>
          <button class="btn btn-primary" @click="exportUsers" :disabled="running.exportUsers">
            {{ running.exportUsers ? 'Exporting...' : 'Export Users' }}
          </button>
        </div>
        <div class="action-card">
          <div class="action-icon">📁</div>
          <h3>Export Project Data</h3>
          <p>Export all cloud project info to CSV file</p>
          <button class="btn btn-primary" @click="exportProjects" :disabled="running.exportProjects">
            {{ running.exportProjects ? 'Exporting...' : 'Export Projects' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, inject, onMounted } from 'vue'
import { maintenanceApi } from '../api/index.js'

const showToast = inject('showToast')
const health = reactive({ status: '', uptime: '', dbConnections: 0, memoryUsage: '' })
const systemStats = ref(null)
const running = reactive({ cache: false, backup: false, cleanup: false, health: false, exportUsers: false, exportProjects: false })
const backups = ref([])
const backupData = reactive({ count: 0, total_size: '' })
const loadingBackups = ref(false)

async function healthCheck() {
  running.health = true
  try {
    const res = await maintenanceApi.healthCheck()
    const d = res?.data || {}
    health.status = d.status || 'unknown'
    health.uptime = d.uptime || '-'
    health.dbConnections = d.db_connections ?? '-'
    health.memoryUsage = d.memory_usage || '-'
  } catch {
    health.status = 'error'
    showToast('Health check failed', 'error')
  } finally {
    running.health = false
  }
}

async function clearCache() {
  running.cache = true
  try {
    const res = await maintenanceApi.clearCache()
    showToast(res?.message || 'Cache cleared')
  } catch {
    showToast('Clear failed', 'error')
  } finally {
    running.cache = false
  }
}

async function dbBackup() {
  running.backup = true
  try {
    const res = await maintenanceApi.dbBackup()
    if (res?.success === false) {
      showToast(res.message || 'Backup failed', 'error')
    } else {
      showToast(res?.message || 'Backup created')
      loadBackups()
    }
  } catch {
    showToast('Backup failed', 'error')
  } finally {
    running.backup = false
  }
}

async function cleanup() {
  if (!confirm('Are you sure you want to perform a cleanup? This will delete expired tokens and logs.')) return
  running.cleanup = true
  try {
    const res = await maintenanceApi.cleanup()
    showToast(res?.message || 'Cleanup complete')
  } catch {
    showToast('Cleanup failed', 'error')
  } finally {
    running.cleanup = false
  }
}

async function loadStats() {
  try {
    const res = await maintenanceApi.systemStats()
    systemStats.value = res?.data || null
  } catch {
    systemStats.value = null
  }
}

async function loadBackups() {
  loadingBackups.value = true
  try {
    const res = await maintenanceApi.listBackups()
    const d = res?.data || {}
    backups.value = d.backups || []
    backupData.count = d.count || 0
    backupData.total_size = d.total_size || ''
  } catch {
    backups.value = []
  } finally {
    loadingBackups.value = false
  }
}

function downloadBackup(filename) {
  maintenanceApi.downloadBackup(filename)
}

async function deleteBackup(filename) {
  if (!confirm(`Are you sure you want to delete backup "${filename}"? This action cannot be undone.`)) return
  try {
    await maintenanceApi.deleteBackup(filename)
    showToast('Deleted')
    loadBackups()
  } catch {
    showToast('Delete failed', 'error')
  }
}

async function exportUsers() {
  running.exportUsers = true
  try {
    const res = await maintenanceApi.exportUsers()
    const url = URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url; a.download = 'users_export.csv'; a.click()
    URL.revokeObjectURL(url)
    showToast('User data exported')
  } catch {
    showToast('Export failed', 'error')
  } finally {
    running.exportUsers = false
  }
}

async function exportProjects() {
  running.exportProjects = true
  try {
    const res = await maintenanceApi.exportProjects()
    const url = URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url; a.download = 'projects_export.csv'; a.click()
    URL.revokeObjectURL(url)
    showToast('Project data exported')
  } catch {
    showToast('Export failed', 'error')
  } finally {
    running.exportProjects = false
  }
}

onMounted(() => {
  healthCheck()
  loadStats()
  loadBackups()
})
</script>

<style scoped>
.view-page { max-width: 1000px; }
.page-header { margin-bottom: 28px; }
.page-header h1 { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.03em; }
.page-desc { color: var(--text-secondary); font-size: 0.87rem; margin-top: 4px; }
.mb-24 { margin-bottom: 24px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 28px; }
.stat-card {
  padding: 20px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
}
.stat-card.success { border-left: 3px solid var(--green); }
.stat-card.warning { border-left: 3px solid var(--yellow); }
.stat-value { font-size: 1.3rem; font-weight: 700; text-transform: capitalize; }
.stat-label { font-size: 0.78rem; color: var(--text-secondary); margin-top: 4px; }

.actions-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 28px; }
.export-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.action-card {
  padding: 24px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: border-color var(--t-fast);
}
.action-card:hover { border-color: var(--accent); }
.action-icon { font-size: 1.5rem; margin-bottom: 10px; }
.action-card h3 { font-size: 1rem; font-weight: 600; margin-bottom: 4px; }
.action-card p { font-size: 0.82rem; color: var(--text-secondary); margin-bottom: 14px; }

.btn { padding: 8px 16px; border: none; border-radius: var(--r-xs); font-family: var(--font); font-size: 0.85rem; cursor: pointer; transition: all var(--t-fast); display: inline-flex; align-items: center; gap: 6px; }
.btn-primary { background: var(--accent); color: #fff; }
.btn-primary:hover { filter: brightness(1.1); }
.btn-warning { background: var(--yellow, #fbbf24); color: #000; }
.btn-warning:hover { filter: brightness(1.1); }
.btn-ghost { background: transparent; border: 1px solid var(--border); color: var(--text-secondary); }
.btn-ghost:hover { border-color: var(--text-primary); color: var(--text-primary); }
.btn-danger { background: rgba(239,68,68,0.15); color: #f87171; border: 1px solid rgba(239,68,68,0.2); }
.btn-danger:hover { background: rgba(239,68,68,0.25); }
.btn-xs { padding: 4px 10px; font-size: 0.78rem; }
.btn:disabled { opacity: 0.6; cursor: not-allowed; }

.card {
  padding: 24px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
}
.card-header-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 1.05rem; font-weight: 600; }

/* Backup list */
.backup-list { display: flex; flex-direction: column; gap: 2px; }
.backup-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; border-radius: var(--r-xs);
  background: rgba(255,255,255,0.02);
  border: 1px solid transparent;
  transition: all 0.15s;
}
.backup-item:hover { background: rgba(255,255,255,0.04); border-color: var(--border); }
.backup-info { display: flex; flex-direction: column; gap: 4px; min-width: 0; flex: 1; }
.backup-name { display: flex; align-items: center; gap: 8px; font-size: 0.87rem; font-weight: 500; color: var(--text-primary); }
.backup-name svg { opacity: 0.4; flex-shrink: 0; }
.backup-meta { font-size: 0.75rem; color: var(--text-muted); padding-left: 24px; }
.backup-actions { display: flex; gap: 8px; flex-shrink: 0; margin-left: 16px; }

.empty-hint { padding: 32px; text-align: center; color: var(--text-muted); font-size: 0.87rem; }

/* Stats */
.stats-list { display: flex; flex-direction: column; gap: 8px; }
.stats-item { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 0.87rem; }
.stats-key { color: var(--text-secondary); }
.stats-val { font-weight: 500; font-variant-numeric: tabular-nums; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .actions-grid { grid-template-columns: 1fr; }
}
</style>
