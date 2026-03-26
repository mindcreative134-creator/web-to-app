<template>
  <div>
    <h2 class="page-title">🔑 Activation Code Management</h2>

    <!-- Generate section -->
    <div class="card mb-20">
      <div class="card-header">
        <span class="card-title">Generate Activation Codes</span>
      </div>
      <div class="form-row">
        <div class="form-group">
          <label class="form-label">Member Tier</label>
          <select v-model="genForm.tier" class="select">
            <option value="pro">Pro</option>
            <option value="ultra">Ultra</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Plan Type</label>
          <select v-model="genForm.plan_type" class="select">
            <option :value="genForm.tier + '_monthly'">Monthly (30 days)</option>
            <option :value="genForm.tier + '_yearly'">Yearly (365 days)</option>
            <option :value="genForm.tier + '_lifetime'">Lifetime</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Quantity</label>
          <input v-model.number="genForm.count" type="number" class="input" min="1" max="500" />
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">Note (Optional)</label>
        <input v-model="genForm.batch_note" class="input" placeholder="e.g. Gumroad 2026-03" />
      </div>
      <div class="flex items-center gap-8">
        <button class="btn btn-primary" @click="generateCodes" :disabled="generating">
          {{ generating ? 'Generating...' : '🔑 Generate Codes' }}
        </button>
      </div>

      <!-- Generated codes -->
      <div v-if="generatedCodes.length" class="generated-codes mt-20">
        <div class="flex items-center justify-between mb-12">
          <span class="text-success">✅ Generated {{ generatedCodes.length }} codes</span>
          <button class="btn btn-secondary btn-sm" @click="copyCodes">📋 Copy All</button>
        </div>
        <div class="codes-list">
          <code v-for="c in generatedCodes" :key="c">{{ c }}</code>
        </div>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-grid mb-20">
      <div class="stat-card purple">
        <div class="stat-label">Total</div>
        <div class="stat-value">{{ stats.total || 0 }}</div>
      </div>
      <div class="stat-card green">
        <div class="stat-label">Unused</div>
        <div class="stat-value">{{ stats.unused || 0 }}</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-label">Used</div>
        <div class="stat-value">{{ stats.used || 0 }}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-label">Usage Rate</div>
        <div class="stat-value">{{ stats.usage_rate || 0 }}%</div>
      </div>
    </div>

    <!-- List -->
    <div class="card">
      <div class="card-header">
        <span class="card-title">Code List</span>
        <button class="btn btn-secondary btn-sm" @click="exportCodes">📥 Export CSV</button>
      </div>

      <div class="toolbar">
        <select v-model="filter.status" class="select" style="max-width:140px" @change="page=1;loadCodes()">
          <option :value="null">All Status</option>
          <option value="unused">Unused</option>
          <option value="used">Used</option>
          <option value="disabled">Disabled</option>
        </select>
        <select v-model="filter.plan_type" class="select" style="max-width:140px" @change="page=1;loadCodes()">
          <option :value="null">All Types</option>
          <option value="monthly">Monthly (Old)</option>
          <option value="pro_monthly">Pro Monthly</option>
          <option value="pro_yearly">Pro Yearly</option>
          <option value="pro_lifetime">Pro Lifetime</option>
          <option value="ultra_monthly">Ultra Monthly</option>
          <option value="ultra_yearly">Ultra Yearly</option>
          <option value="ultra_lifetime">Ultra Lifetime</option>
          <option value="quarterly">Quarterly (Old)</option>
          <option value="yearly">Yearly (Old)</option>
          <option value="lifetime">Lifetime (Old)</option>
        </select>
      </div>

      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>Code</th><th>Type</th><th>Days</th><th>Status</th><th>User</th><th>Batch</th><th>Action</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in codes" :key="c.id">
              <td><code style="font-size:12px">{{ c.code }}</code></td>
              <td>{{ c.plan_type }}</td>
              <td>{{ c.duration_days }}</td>
              <td>
                <span :class="['badge', statusBadge(c.status)]">{{ c.status }}</span>
              </td>
              <td>{{ c.used_by || '-' }}</td>
              <td class="text-muted" style="font-size:12px">{{ c.batch_id || '-' }}</td>
              <td>
                <button v-if="c.status === 'unused'" class="btn btn-danger btn-sm"
                  @click="disableCode(c.id)">Disable</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!codes.length" class="empty-state"><p>No activation codes found</p></div>
      </div>

      <div class="pagination" v-if="totalPages > 1">
        <button :disabled="page <= 1" @click="page--; loadCodes()">Prev</button>
        <span>{{ page }} / {{ totalPages }}</span>
        <button :disabled="page >= totalPages" @click="page++; loadCodes()">Next</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted, watch } from 'vue'
import { activationApi } from '../api'

const showToast = inject('showToast')
const genForm = ref({ tier: 'pro', plan_type: 'pro_monthly', count: 10, batch_note: '' })

// Automatically update plan_type when member tier changes
watch(() => genForm.value.tier, (newTier, oldTier) => {
  const current = genForm.value.plan_type
  if (current.endsWith('_lifetime')) {
    genForm.value.plan_type = newTier + '_lifetime'
    return
  }
  if (current === 'lifetime') return // Old lifetime does not need switching
  // Replace old tier prefix with new tier prefix
  if (current.startsWith(oldTier + '_')) {
    genForm.value.plan_type = current.replace(oldTier + '_', newTier + '_')
  } else {
    genForm.value.plan_type = newTier + '_monthly'
  }
})
const generatedCodes = ref([])
const generating = ref(false)
const stats = ref({})
const codes = ref([])
const loading = ref(true)
const page = ref(1)
const totalPages = ref(1)
const filter = ref({ status: null, plan_type: null })

async function generateCodes() {
  generating.value = true
  try {
    const res = await activationApi.generate(genForm.value)
    generatedCodes.value = res.data.codes
    showToast(`Generated ${res.data.count} codes`)
    loadStats()
    loadCodes()
  } catch (e) { showToast(e?.detail || 'Generation failed', 'error') }
  finally { generating.value = false }
}

function copyCodes() {
  navigator.clipboard.writeText(generatedCodes.value.join('\n'))
  showToast('Copied to clipboard')
}

async function loadStats() {
  try {
    const res = await activationApi.stats()
    stats.value = res.data
  } catch (e) { console.error(e) }
}

async function loadCodes() {
  loading.value = true
  try {
    const params = { page: page.value, page_size: 20 }
    if (filter.value.status) params.status = filter.value.status
    if (filter.value.plan_type) params.plan_type = filter.value.plan_type
    const res = await activationApi.list(params)
    codes.value = res.data
    totalPages.value = res.total_pages
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function disableCode(id) {
  if (!confirm('Are you sure you want to disable this code?')) return
  try {
    await activationApi.disable(id)
    showToast('Disabled')
    loadCodes()
    loadStats()
  } catch (e) { showToast(e?.detail || 'Operation failed', 'error') }
}

async function exportCodes() {
  try {
    const res = await activationApi.exportCsv({ status: filter.value.status || 'unused' })
    const url = URL.createObjectURL(new Blob([res]))
    const a = document.createElement('a')
    a.href = url; a.download = 'activation_codes.csv'; a.click()
    URL.revokeObjectURL(url)
  } catch (e) { showToast('Export failed', 'error') }
}

function statusBadge(s) {
  return { unused: 'badge-success', used: 'badge-info', disabled: 'badge-danger', expired: 'badge-warning' }[s] || 'badge-info'
}

onMounted(() => { loadStats(); loadCodes() })
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 12px; }
.codes-list { display: flex; flex-wrap: wrap; gap: 8px; max-height: 200px; overflow-y: auto; }
.codes-list code {
  padding: 6px 12px; background: var(--bg-input); border: 1px solid var(--border);
  border-radius: 6px; font-size: 13px; color: var(--accent);
}
@media (max-width: 900px) { .stats-grid { grid-template-columns: repeat(2,1fr); } }
</style>
