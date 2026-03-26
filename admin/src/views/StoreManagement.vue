<template>
  <div class="slide-up">
    <div class="page-header">
      <div>
        <h1>Store Management</h1>
        <p class="text-secondary text-sm">Manage all items in App Store and Module Store</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card" v-for="(s, i) in statCards" :key="i" :style="{ animationDelay: i * 50 + 'ms' }">
        <div class="stat-icon" :style="{ background: s.bg }">
          <span v-html="s.svg"></span>
        </div>
        <div class="stat-body">
          <span class="stat-value">{{ s.value }}</span>
          <span class="stat-label">{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-row mt-20">
      <div class="search-wrap">
        <svg class="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        <input v-model="search" class="input search-input" placeholder="Search name, description, or tags" @input="debouncedLoad" />
      </div>
      <select v-model="typeFilter" class="select" style="width:140px" @change="page=1;loadItems()">
        <option value="">All Types</option>
        <option value="app">App</option>
        <option value="extension">Extension</option>
        <option value="browser_extension">Browser Extension</option>
      </select>
      <select v-model="statusFilter" class="select" style="width:120px" @change="page=1;loadItems()">
        <option value="">All Status</option>
        <option value="approved">Approved</option>
        <option value="pending">Pending</option>
        <option value="featured">Featured</option>
      </select>
      <select v-model="sortField" class="select" style="width:120px" @change="loadItems()">
        <option value="created_at">Created At</option>
        <option value="downloads">Downloads</option>
        <option value="rating">Rating</option>
        <option value="like_count">Likes</option>
        <option value="view_count">Views</option>
      </select>

      <!-- Batch Actions -->
      <div v-if="selectedIds.length" class="batch-actions">
        <span class="batch-count">Selected {{ selectedIds.length }}</span>
        <button class="btn btn-sm btn-primary" @click="batchAction('approve')">Approve</button>
        <button class="btn btn-sm btn-secondary" @click="batchAction('feature')">Feature</button>
        <button class="btn btn-sm btn-danger" @click="batchAction('delete')">Delete</button>
      </div>
    </div>

    <!-- Table -->
    <div class="card mt-16">
      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead><tr>
            <th style="width:36px"><input type="checkbox" :checked="allSelected" @change="toggleAll" class="check-all" /></th>
            <th>Item</th>
            <th>Type</th>
            <th>Category</th>
            <th>Stats</th>
            <th>Status</th>
            <th>Author</th>
            <th>Created At</th>
            <th style="width:100px"></th>
          </tr></thead>
          <tbody>
            <tr v-for="item in items" :key="item.id" class="item-row">
              <td><input type="checkbox" :value="item.id" v-model="selectedIds" class="row-check" /></td>
              <td>
                <div class="item-cell">
                  <div class="item-icon" :style="{ background: typeGradient(item.module_type) }">
                    {{ item.icon || item.name[0]?.toUpperCase() }}
                  </div>
                  <div class="item-info">
                    <div class="item-name">{{ item.name }}</div>
                    <div class="text-xs text-muted">{{ item.package_name || `v${item.version_name || '1.0'}` }}</div>
                  </div>
                </div>
              </td>
              <td><span :class="['badge', typeBadge(item.module_type)]">{{ typeLabel(item.module_type) }}</span></td>
              <td class="text-sm">{{ item.category || '—' }}</td>
              <td>
                <div class="metrics-mini">
                  <span title="Download">⬇ {{ fmtNum(item.downloads) }}</span>
                  <span title="View">👁 {{ fmtNum(item.view_count) }}</span>
                  <span title="Rating">⭐ {{ (item.rating || 0).toFixed(1) }}</span>
                </div>
              </td>
              <td>
                <div class="status-tags">
                  <span v-if="item.is_featured" class="badge badge-warning">Featured</span>
                  <span :class="['badge', item.is_approved ? 'badge-success' : 'badge-danger']">
                    {{ item.is_approved ? 'Approved' : 'Pending' }}
                  </span>
                </div>
              </td>
              <td class="text-sm">{{ item.author_name }}</td>
              <td class="text-sm text-muted">{{ fmtDate(item.created_at) }}</td>
              <td>
                <div class="action-btns">
                  <button class="btn btn-ghost btn-sm" @click="toggleFeatured(item)" :title="item.is_featured ? 'Unfeature' : 'Feature'">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" :stroke="item.is_featured ? '#fbbf24' : 'currentColor'" stroke-width="2">
                      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                    </svg>
                  </button>
                  <button class="btn btn-ghost btn-sm" @click="openDetail(item)" title="View Details">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                  <button class="btn btn-ghost btn-sm" @click="deleteItem(item)" title="Delete">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!items.length" class="empty-state"><p>No items found</p></div>
      </div>
      <div v-if="totalPages > 1" class="pagination">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; loadItems()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
        <span class="text-xs text-muted">{{ page }} / {{ totalPages }}</span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="page++; loadItems()">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </div>
    </div>

    <!-- Detail / Edit Modal -->
    <div v-if="detailItem" class="modal-overlay" @click.self="detailItem = null">
      <div class="modal modal-lg">
        <div class="flex justify-between items-center" style="margin-bottom:20px">
          <div>
            <h2 class="modal-title">{{ detailItem.name }}</h2>
            <span :class="['badge', typeBadge(detailItem.module_type)]" style="margin-top:6px">{{ typeLabel(detailItem.module_type) }}</span>
          </div>
          <button class="btn-icon" @click="detailItem = null">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>

        <div class="detail-grid">
          <!-- Left: Info -->
          <div class="detail-section">
            <h4 class="section-label">Item Info</h4>
            <div class="info-list">
              <div class="info-item"><span>ID</span><span># {{ detailItem.id }}</span></div>
              <div class="info-item"><span>Author</span><span>{{ detailItem.author_name }} (#{{ detailItem.author_id }})</span></div>
              <div class="info-item"><span>Category</span><span>{{ detailItem.category || '—' }}</span></div>
              <div class="info-item"><span>Version</span><span>{{ detailItem.version_name || '—' }} ({{ detailItem.version_code }})</span></div>
              <div class="info-item"><span>Package Name</span><span>{{ detailItem.package_name || '—' }}</span></div>
              <div class="info-item"><span>Tags</span><span>{{ detailItem.tags || '—' }}</span></div>
              <div class="info-item"><span>Downloads</span><span class="text-success">{{ detailItem.downloads }}</span></div>
              <div class="info-item"><span>Views</span><span>{{ detailItem.view_count }}</span></div>
              <div class="info-item"><span>Likes</span><span>{{ detailItem.like_count }}</span></div>
              <div class="info-item"><span>Rating</span><span>⭐ {{ (detailItem.rating || 0).toFixed(2) }} ({{ detailItem.rating_count }} reviews)</span></div>
              <div class="info-item"><span>Comments</span><span>{{ detailItem.comment_count }}</span></div>
              <div class="info-item"><span>Created At</span><span>{{ fmtDate(detailItem.created_at) }}</span></div>
              <div class="info-item"><span>Updated At</span><span>{{ fmtDate(detailItem.updated_at) }}</span></div>
            </div>
          </div>

          <!-- Right: Edit -->
          <div class="detail-section">
            <h4 class="section-label">Admin Actions</h4>
            <div class="form-group">
              <label class="form-label">Name</label>
              <input v-model="editForm.name" class="input" />
            </div>
            <div class="form-group">
              <label class="form-label">Category</label>
              <select v-model="editForm.category" class="select">
                <option value="">Uncategorized</option>
                <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Tags</label>
              <input v-model="editForm.tags" class="input" placeholder="Comma separated" />
            </div>
            <div class="form-group">
              <label class="form-label">Version Name</label>
              <input v-model="editForm.version_name" class="input" />
            </div>
            <div class="flex gap-16 mt-8">
              <label class="toggle-label"><input type="checkbox" v-model="editForm.is_approved" class="toggle" /> Approved</label>
              <label class="toggle-label"><input type="checkbox" v-model="editForm.is_featured" class="toggle" /> Featured</label>
            </div>
            <button class="btn btn-primary btn-sm mt-16" @click="saveItem" :disabled="saving" style="width:100%">Save Changes</button>

            <!-- Quick Actions -->
            <div class="quick-actions mt-16">
              <button v-if="!detailItem.is_approved" class="btn btn-sm" style="background:var(--green);color:#fff;width:100%" @click="quickApprove">
                ✓ Approve
              </button>
              <button v-else class="btn btn-sm btn-danger" style="width:100%" @click="quickReject">
                ✗ Revoke Approval
              </button>
            </div>
          </div>
        </div>

        <!-- Description preview -->
        <div v-if="detailItem.description" class="desc-preview mt-16">
          <h4 class="section-label">Description</h4>
          <div class="desc-content">{{ detailItem.description }}</div>
        </div>

        <!-- Screenshots -->
        <div v-if="detailItem.screenshots?.length" class="screenshots-section mt-16">
          <h4 class="section-label">Screenshots ({{ detailItem.screenshots.length }})</h4>
          <div class="screenshots-grid">
            <img v-for="(url, i) in detailItem.screenshots" :key="i" :src="url" class="screenshot-img" @error="$event.target.style.display='none'" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, inject, onMounted, reactive } from 'vue'
import { storeApi } from '../api'

const showToast = inject('showToast')
const items = ref([])
const loading = ref(true)
const search = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const sortField = ref('created_at')
const page = ref(1)
const total = ref(0)
const totalPages = ref(1)
const selectedIds = ref([])
const detailItem = ref(null)
const editForm = ref({})
const saving = ref(false)
const stats = ref({})

const categories = [
  'tools', 'social', 'education', 'entertainment', 'productivity',
  'lifestyle', 'business', 'news', 'finance', 'health', 'other'
]

const allSelected = computed(() =>
  items.value.length > 0 && selectedIds.value.length === items.value.length
)

const statCards = computed(() => {
  const s = stats.value
  return [
    { label: 'Total Items', value: s.total_items || 0, bg: 'linear-gradient(135deg, rgba(99,102,241,0.15), rgba(99,102,241,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#818cf8" stroke-width="1.8"><path d="M16.5 9.4l-9-5.19"/><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>' },
    { label: 'Apps', value: s.total_apps || 0, bg: 'linear-gradient(135deg, rgba(52,211,153,0.15), rgba(52,211,153,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="1.8"><rect x="5" y="2" width="14" height="20" rx="2" ry="2"/><line x1="12" y1="18" x2="12.01" y2="18"/></svg>' },
    { label: 'Extensions', value: s.total_extensions || 0, bg: 'linear-gradient(135deg, rgba(96,165,250,0.15), rgba(96,165,250,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" stroke-width="1.8"><path d="M14.7 6.3a1 1 0 000 1.4l1.6 1.6a1 1 0 001.4 0l3.77-3.77a6 6 0 01-7.94 7.94l-6.91 6.91a2.12 2.12 0 01-3-3l6.91-6.91a6 6 0 017.94-7.94l-3.76 3.76z"/></svg>' },
    { label: 'Featured', value: s.total_featured || 0, bg: 'linear-gradient(135deg, rgba(251,191,36,0.15), rgba(251,191,36,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" stroke-width="1.8"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' },
    { label: 'Pending', value: s.pending_count || 0, bg: 'linear-gradient(135deg, rgba(248,113,113,0.15), rgba(248,113,113,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f87171" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>' },
    { label: 'Total Downloads', value: fmtNum(s.total_downloads || 0), bg: 'linear-gradient(135deg, rgba(167,139,250,0.15), rgba(167,139,250,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#a78bfa" stroke-width="1.8"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>' },
  ]
})

let timer
function debouncedLoad() { clearTimeout(timer); timer = setTimeout(() => { page.value = 1; loadItems() }, 400) }

function toggleAll(e) {
  selectedIds.value = e.target.checked ? items.value.map(i => i.id) : []
}

async function loadStats() {
  try { const res = await storeApi.stats(); stats.value = res.data } catch {}
}

async function loadItems() {
  loading.value = true
  try {
    const params = {
      page: page.value, page_size: 20,
      sort: sortField.value, order: 'desc',
    }
    if (search.value) params.search = search.value
    if (typeFilter.value) params.module_type = typeFilter.value
    if (statusFilter.value) params.status = statusFilter.value

    const res = await storeApi.listItems(params)
    items.value = res.data.items
    total.value = res.data.total
    totalPages.value = res.data.total_pages
    selectedIds.value = []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function openDetail(item) {
  try {
    const res = await storeApi.getItem(item.id)
    detailItem.value = res.data
    editForm.value = {
      name: res.data.name,
      category: res.data.category || '',
      tags: res.data.tags || '',
      version_name: res.data.version_name || '',
      is_approved: res.data.is_approved,
      is_featured: res.data.is_featured,
    }
  } catch { showToast('Load failed', 'error') }
}

async function saveItem() {
  saving.value = true
  try {
    await storeApi.updateItem(detailItem.value.id, editForm.value)
    showToast('Saved')
    loadItems()
    loadStats()
    // Refresh detail
    const res = await storeApi.getItem(detailItem.value.id)
    detailItem.value = res.data
  } catch { showToast('Save failed', 'error') }
  finally { saving.value = false }
}

async function quickApprove() {
  try {
    await storeApi.updateItem(detailItem.value.id, { is_approved: true })
    showToast('Approved')
    detailItem.value.is_approved = true
    editForm.value.is_approved = true
    loadItems(); loadStats()
  } catch { showToast('Operation failed', 'error') }
}

async function quickReject() {
  if (!confirm('Revoke approval? The item will be hidden from the store.')) return
  try {
    await storeApi.updateItem(detailItem.value.id, { is_approved: false })
    showToast('Revoked')
    detailItem.value.is_approved = false
    editForm.value.is_approved = false
    loadItems(); loadStats()
  } catch { showToast('Operation failed', 'error') }
}

async function toggleFeatured(item) {
  try {
    await storeApi.updateItem(item.id, { is_featured: !item.is_featured })
    showToast(item.is_featured ? 'Unfeatured' : 'Featured')
    loadItems(); loadStats()
  } catch { showToast('Operation failed', 'error') }
}

async function deleteItem(item) {
  if (!confirm(`Delete "${item.name}"? This action cannot be undone.`)) return
  try {
    await storeApi.deleteItem(item.id)
    showToast('Deleted')
    loadItems(); loadStats()
  } catch { showToast('Delete failed', 'error') }
}

async function batchAction(action) {
  if (!selectedIds.value.length) return
  const labels = { approve: 'Approve', feature: 'Feature', delete: 'Delete' }
  if (action === 'delete' && !confirm(`Delete selected ${selectedIds.value.length} items?`)) return
  try {
    await storeApi.batch({ ids: selectedIds.value, action })
    showToast(`${labels[action]}d ${selectedIds.value.length} items`)
    selectedIds.value = []
    loadItems(); loadStats()
  } catch { showToast('Batch operation failed', 'error') }
}

function typeLabel(t) {
  return { app: 'App', extension: 'Extension', browser_extension: 'Browser Extension' }[t] || t
}
function typeBadge(t) {
  return { app: 'badge-success', extension: 'badge-info', browser_extension: 'badge-warning' }[t] || 'badge-secondary'
}
function typeGradient(t) {
  return {
    app: 'linear-gradient(135deg, #34d399, #059669)',
    extension: 'linear-gradient(135deg, #60a5fa, #3b82f6)',
    browser_extension: 'linear-gradient(135deg, #fbbf24, #d97706)',
  }[t] || 'linear-gradient(135deg, #6366f1, #4f46e5)'
}
function fmtNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}
function fmtDate(d) {
  return d ? new Date(d).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
}

onMounted(() => { loadStats(); loadItems() })
</script>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h1 { margin-bottom: 4px; }

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
}
.stat-card {
  background: var(--bg-card); backdrop-filter: blur(40px);
  border: 1px solid var(--border); border-radius: var(--r-lg);
  padding: 16px; display: flex; align-items: center; gap: 12px;
  animation: slideUp 0.5s var(--ease-spring) both;
  transition: transform var(--t-normal), box-shadow var(--t-normal);
}
.stat-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.stat-icon {
  width: 38px; height: 38px; min-width: 38px; border-radius: var(--r-sm);
  display: flex; align-items: center; justify-content: center;
}
.stat-body { display: flex; flex-direction: column; }
.stat-value { font-size: 1.3rem; font-weight: 700; letter-spacing: -0.03em; }
.stat-label { font-size: 0.72rem; color: var(--text-muted); margin-top: 1px; }

/* Filter Row */
.filter-row { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.search-wrap { position: relative; flex: 1; min-width: 200px; max-width: 320px; }
.search-icon { position: absolute; left: 12px; top: 50%; transform: translateY(-50%); color: var(--text-muted); pointer-events: none; }
.search-input { padding-left: 36px; }

.batch-actions {
  display: flex; align-items: center; gap: 8px;
  margin-left: auto;
  padding: 6px 14px; border-radius: var(--r-sm);
  background: var(--accent-subtle); border: 1px solid rgba(99,102,241,0.2);
  animation: fadeIn 0.3s var(--ease-out);
}
.batch-count { font-size: 0.8rem; color: var(--accent); font-weight: 500; white-space: nowrap; }

/* Table */
.item-row { transition: background var(--t-fast); }
.item-cell { display: flex; align-items: center; gap: 10px; }
.item-icon {
  width: 34px; height: 34px; min-width: 34px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.75rem; font-weight: 600; color: #fff;
}
.item-info { min-width: 0; }
.item-name { font-weight: 500; font-size: 0.87rem; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px; }

.metrics-mini { display: flex; gap: 10px; font-size: 0.75rem; color: var(--text-muted); }
.status-tags { display: flex; gap: 4px; }

.action-btns { display: flex; gap: 2px; }

.check-all, .row-check { accent-color: var(--accent); cursor: pointer; }

/* Detail Modal */
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-section { background: var(--bg-input); border-radius: var(--r-md); padding: 18px; }
.section-label { font-size: 0.8rem; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 14px; }
.info-list { display: flex; flex-direction: column; }
.info-item { display: flex; justify-content: space-between; padding: 7px 0; font-size: 0.83rem; border-bottom: 1px solid var(--border-light); }
.info-item span:first-child { color: var(--text-muted); white-space: nowrap; margin-right: 12px; }
.info-item span:last-child { text-align: right; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 200px; }
.info-item:last-child { border-bottom: none; }

.toggle-label { display: flex; align-items: center; gap: 6px; font-size: 0.85rem; color: var(--text-secondary); cursor: pointer; }
.toggle { accent-color: var(--accent); }

.quick-actions { display: flex; flex-direction: column; gap: 8px; }

.desc-preview { background: var(--bg-input); border-radius: var(--r-md); padding: 18px; }
.desc-content { font-size: 0.85rem; color: var(--text-secondary); white-space: pre-wrap; max-height: 200px; overflow-y: auto; line-height: 1.6; }

.screenshots-section { background: var(--bg-input); border-radius: var(--r-md); padding: 18px; }
.screenshots-grid { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 8px; }
.screenshot-img { height: 160px; border-radius: 8px; border: 1px solid var(--border); object-fit: cover; flex-shrink: 0; }

@media (max-width: 900px) {
  .detail-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
