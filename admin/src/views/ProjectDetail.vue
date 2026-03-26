<template>
  <div>
    <div class="page-header">
      <button class="btn btn-secondary btn-sm" @click="$router.push('/projects')">← Back</button>
      <h2 class="page-title" v-if="project">{{ project.project_name }}</h2>
      <div v-if="project" class="page-meta">
        <code class="key-badge">{{ project.project_key }}</code>
        <a :href="shareUrl" target="_blank" class="btn btn-primary btn-sm">📤 Share Page</a>
      </div>
    </div>

    <div v-if="loading" class="spinner"></div>
    <template v-else-if="project">
      <!-- Tabs -->
      <div class="tabs">
        <button v-for="t in tabs" :key="t.key" :class="['tab', { active: tab === t.key }]"
          @click="tab = t.key; loadTab()">{{ t.icon }} {{ t.label }}</button>
      </div>

      <!-- Tab: Activation Codes -->
      <div v-if="tab === 'codes'" class="card mt-12">
        <div class="card-header">
          <h3>🔑 Activation Codes</h3>
          <div class="flex gap-8">
            <input v-model.number="codeCount" type="number" min="1" max="100" class="input" style="width:80px" placeholder="Qty" />
            <input v-model="codeLabel" class="input" style="width:140px" placeholder="Label (Optional)" />
            <button class="btn btn-primary btn-sm" @click="genCodes">Generate</button>
          </div>
        </div>
        <table v-if="codes.length">
          <thead><tr><th>Code</th><th>Label</th><th>Status</th><th>Device ID</th><th>Activated At</th></tr></thead>
          <tbody>
            <tr v-for="c in codes" :key="c.id">
              <td><code style="font-size:11px">{{ c.code || c.code_hash?.slice(0,12) + '...' }}</code></td>
              <td>{{ c.label || '-' }}</td>
              <td><span :class="['badge', c.status === 'used' ? 'badge-success' : c.status === 'disabled' ? 'badge-danger' : 'badge-secondary']">{{ c.status }}</span></td>
              <td style="font-size:11px;max-width:120px;overflow:hidden;text-overflow:ellipsis">{{ c.activated_device_id || '-' }}</td>
              <td style="font-size:12px">{{ fmtDate(c.activated_at) || '-' }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No codes found</p></div>
      </div>

      <!-- Tab: Announcements -->
      <div v-if="tab === 'announcements'" class="card mt-12">
        <div class="card-header">
          <h3>📢 Announcements</h3>
          <button class="btn btn-primary btn-sm" @click="showAnnForm = !showAnnForm">+ New</button>
        </div>
        <div v-if="showAnnForm" class="form-inline">
          <input v-model="annForm.title" class="input" placeholder="Title" />
          <textarea v-model="annForm.content" class="input" placeholder="Content" rows="3"></textarea>
          <div class="flex gap-8">
            <select v-model="annForm.display_type" class="select">
              <option value="popup">Popup</option><option value="banner">Banner</option><option value="fullscreen">Fullscreen</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="createAnn">Publish</button>
          </div>
        </div>
        <table v-if="announcements.length">
          <thead><tr><th>Title</th><th>Type</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            <tr v-for="a in announcements" :key="a.id">
              <td><strong>{{ a.title }}</strong></td>
              <td>{{ a.display_type }}</td>
              <td><span :class="['badge', a.is_active ? 'badge-success' : 'badge-danger']">{{ a.is_active ? 'Active' : 'Deactivated' }}</span></td>
              <td>
                <button class="btn btn-secondary btn-sm" @click="toggleAnn(a)">{{ a.is_active ? 'Deactivate' : 'Enable' }}</button>
                <button class="btn btn-danger btn-sm" @click="removeAnn(a.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No announcements found</p></div>
      </div>

      <!-- Tab: Versions -->
      <div v-if="tab === 'versions'" class="card mt-12">
        <div class="card-header">
          <h3>📦 Versions</h3>
          <button class="btn btn-primary btn-sm" @click="showVerForm = !showVerForm">+ Publish</button>
        </div>
        <div v-if="showVerForm" class="form-inline">
          <div class="flex gap-8">
            <input v-model.number="verForm.version_code" type="number" class="input" placeholder="Code (Number)" style="width:120px" />
            <input v-model="verForm.version_name" class="input" placeholder="Name (e.g. 1.0.0)" style="width:140px" />
          </div>
          <textarea v-model="verForm.changelog" class="input" placeholder="Changelog" rows="2"></textarea>
          <div class="flex gap-8 items-center">
            <input type="file" accept=".apk" @change="verForm.file = $event.target.files[0]" />
            <label><input type="checkbox" v-model="verForm.is_force_update" /> Force Update</label>
            <select v-model="verForm.upload_to" class="select" style="width:120px">
              <option value="both">Both</option><option value="github">GitHub</option><option value="gitee">Gitee</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="publishVer" :disabled="publishing">{{ publishing ? 'Uploading...' : 'Publish' }}</button>
          </div>
        </div>
        <table v-if="versions.length">
          <thead><tr><th>Version</th><th>Size</th><th>GitHub</th><th>Gitee</th><th>Force</th><th>Time</th></tr></thead>
          <tbody>
            <tr v-for="v in versions" :key="v.id">
              <td><strong>v{{ v.version_name }}</strong> ({{ v.version_code }})</td>
              <td>{{ v.file_size ? (v.file_size / 1024 / 1024).toFixed(1) + 'MB' : '-' }}</td>
              <td><a v-if="v.download_url_github" :href="v.download_url_github" target="_blank" class="link">Download</a><span v-else>-</span></td>
              <td><a v-if="v.download_url_gitee" :href="v.download_url_gitee" target="_blank" class="link">Download</a><span v-else>-</span></td>
              <td>{{ v.is_force_update ? '✅' : '' }}</td>
              <td style="font-size:12px">{{ fmtDate(v.created_at) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No versions found</p></div>
      </div>

      <!-- Tab: Remote Config -->
      <div v-if="tab === 'configs'" class="card mt-12">
        <div class="card-header">
          <h3>⚙️ Remote Config</h3>
          <button class="btn btn-primary btn-sm" @click="showCfgForm = !showCfgForm">+ Add</button>
        </div>
        <div v-if="showCfgForm" class="form-inline">
          <div class="flex gap-8">
            <input v-model="cfgForm.config_key" class="input" placeholder="Key" />
            <input v-model="cfgForm.config_value" class="input" placeholder="Value" />
            <select v-model="cfgForm.value_type" class="select" style="width:100px">
              <option value="string">String</option><option value="number">Number</option>
              <option value="boolean">Boolean</option><option value="json">JSON</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="createCfg">Add</button>
          </div>
        </div>
        <table v-if="configs.length">
          <thead><tr><th>Key</th><th>Value</th><th>Type</th><th>Action</th></tr></thead>
          <tbody>
            <tr v-for="c in configs" :key="c.id">
              <td><code>{{ c.config_key }}</code></td>
              <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis">{{ c.config_value }}</td>
              <td>{{ c.value_type }}</td>
              <td><button class="btn btn-danger btn-sm" @click="removeCfg(c.id)">Delete</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No configurations found</p></div>
      </div>

      <!-- Tab: Analytics -->
      <div v-if="tab === 'analytics'" class="card mt-12">
        <div class="card-header">
          <h3>📊 Analytics</h3>
          <div class="flex gap-8">
            <button v-for="d in [7,14,30]" :key="d" :class="['btn btn-sm', analyticsDays === d ? 'btn-primary' : 'btn-secondary']"
              @click="analyticsDays = d; loadAnalytics()">{{ d }}d</button>
          </div>
        </div>
        <template v-if="analytics">
          <div class="stats-row">
            <div class="mini-stat"><span class="mini-value">{{ analytics.summary.total_opens }}</span><span class="mini-label">Opens</span></div>
            <div class="mini-stat"><span class="mini-value">{{ analytics.summary.total_installs }}</span><span class="mini-label">Installs</span></div>
            <div class="mini-stat"><span class="mini-value">{{ analytics.summary.avg_daily_active }}</span><span class="mini-label">Avg Daily Active</span></div>
            <div class="mini-stat"><span class="mini-value">{{ analytics.summary.total_crashes }}</span><span class="mini-label">Crashes</span></div>
          </div>
          <div class="chart-container" v-if="analytics.trend.length">
            <Line :data="chartData" :options="chartOpts" />
          </div>
          <div class="dist-grid">
            <div class="dist-card" v-if="Object.keys(analytics.country_distribution || {}).length">
              <h4>🌍 Countries</h4>
              <div v-for="(v,k) in analytics.country_distribution" :key="k" class="dist-row">
                <span>{{ k }}</span><span>{{ v }}</span>
              </div>
            </div>
            <div class="dist-card" v-if="Object.keys(analytics.device_distribution || {}).length">
              <h4>📱 Devices</h4>
              <div v-for="(v,k) in analytics.device_distribution" :key="k" class="dist-row">
                <span>{{ k }}</span><span>{{ v }}</span>
              </div>
            </div>
            <div class="dist-card" v-if="Object.keys(analytics.os_distribution || {}).length">
              <h4>🤖 OS Versions</h4>
              <div v-for="(v,k) in analytics.os_distribution" :key="k" class="dist-row">
                <span>{{ k }}</span><span>{{ v }}</span>
              </div>
            </div>
            <div class="dist-card" v-if="Object.keys(analytics.version_distribution || {}).length">
              <h4>📦 App Versions</h4>
              <div v-for="(v,k) in analytics.version_distribution" :key="k" class="dist-row">
                <span>v{{ k }}</span><span>{{ v }}</span>
              </div>
            </div>
          </div>
          <div v-if="!analytics.trend.length" class="empty-state"><p>No analytics data</p></div>
        </template>
      </div>

      <!-- Tab: Webhooks -->
      <div v-if="tab === 'webhooks'" class="card mt-12">
        <div class="card-header">
          <h3>🔗 Webhook</h3>
          <button class="btn btn-primary btn-sm" @click="showWhForm = !showWhForm">+ Add</button>
        </div>
        <div v-if="showWhForm" class="form-inline">
          <input v-model="whForm.url" class="input" placeholder="Webhook URL (https://...)" />
          <div class="flex gap-8">
            <input v-model="whForm.secret" class="input" placeholder="Secret (Optional)" style="width:160px" />
            <select v-model="whForm.events" class="select" multiple style="width:280px;height:80px">
              <option value="code_activated">Code Activated</option>
              <option value="version_published">Version Published</option>
              <option value="daily_report">Daily Report</option>
              <option value="announcement_created">Announcement Created</option>
            </select>
            <button class="btn btn-primary btn-sm" @click="createWh">Add</button>
          </div>
        </div>
        <table v-if="webhooks.length">
          <thead><tr><th>URL</th><th>Events</th><th>Status</th><th>Last Trigger</th><th>Fail</th><th>Action</th></tr></thead>
          <tbody>
            <tr v-for="w in webhooks" :key="w.id">
              <td style="font-size:12px;max-width:200px;overflow:hidden;text-overflow:ellipsis">{{ w.url }}</td>
              <td><span class="badge badge-info" v-for="e in w.events" :key="e" style="margin:2px">{{ eventLabel(e) }}</span></td>
              <td><span :class="['badge', w.is_active ? 'badge-success' : 'badge-danger']">{{ w.is_active ? 'Active' : 'Disabled' }}</span></td>
              <td style="font-size:12px">{{ fmtDate(w.last_triggered_at) || 'Never' }}</td>
              <td>{{ w.failure_count }}</td>
              <td><button class="btn btn-danger btn-sm" @click="removeWh(w.id)">Delete</button></td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No webhooks found</p></div>
      </div>

      <!-- Tab: Backups -->
      <div v-if="tab === 'backups'" class="card mt-12">
        <div class="card-header">
          <h3>💾 Project Backup</h3>
          <div class="flex gap-8">
            <select v-model="backupPlatform" class="select" style="width:120px">
              <option value="github">GitHub</option><option value="gitee">Gitee</option>
            </select>
            <input type="file" accept=".zip" @change="backupFile = $event.target.files[0]" />
            <button class="btn btn-primary btn-sm" @click="doBackup" :disabled="backing">{{ backing ? 'Uploading...' : 'Upload' }}</button>
          </div>
        </div>
        <table v-if="backups.length">
          <thead><tr><th>Platform</th><th>Size</th><th>Status</th><th>Link</th><th>Time</th></tr></thead>
          <tbody>
            <tr v-for="b in backups" :key="b.id">
              <td><span :class="['badge', b.platform === 'github' ? 'badge-secondary' : 'badge-danger']">{{ b.platform }}</span></td>
              <td>{{ b.file_size ? (b.file_size / 1024 / 1024).toFixed(2) + 'MB' : '-' }}</td>
              <td><span :class="['badge', b.status === 'success' ? 'badge-success' : b.status === 'failed' ? 'badge-danger' : 'badge-warning']">{{ b.status }}</span></td>
              <td><a v-if="b.repo_url" :href="b.repo_url" target="_blank" class="link">View</a><span v-else>-</span></td>
              <td style="font-size:12px">{{ fmtDate(b.created_at) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else class="empty-state"><p>No backup records found</p></div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, inject } from 'vue'
import { useRoute } from 'vue-router'
import { projectApi } from '../api'
import { Line } from 'vue-chartjs'
import { Chart, CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip } from 'chart.js'

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip)

const route = useRoute()
const showToast = inject('showToast')
const projectId = computed(() => Number(route.params.id))

const loading = ref(true)
const project = ref(null)
const tab = ref('codes')

// Tab data
const codes = ref([])
const announcements = ref([])
const versions = ref([])
const configs = ref([])
const analytics = ref(null)
const webhooks = ref([])
const backups = ref([])

// Forms
const codeCount = ref(10)
const codeLabel = ref('')
const showAnnForm = ref(false)
const annForm = ref({ title: '', content: '', display_type: 'popup' })
const showVerForm = ref(false)
const verForm = ref({ version_code: 1, version_name: '1.0.0', changelog: '', file: null, is_force_update: false, upload_to: 'both' })
const publishing = ref(false)
const showCfgForm = ref(false)
const cfgForm = ref({ config_key: '', config_value: '', value_type: 'string' })
const analyticsDays = ref(7)
const showWhForm = ref(false)
const whForm = ref({ url: '', secret: '', events: ['code_activated'] })
const backupPlatform = ref('github')
const backupFile = ref(null)
const backing = ref(false)

const tabs = [
  { key: 'codes', icon: '🔑', label: 'Codes' },
  { key: 'announcements', icon: '📢', label: 'Announcements' },
  { key: 'versions', icon: '📦', label: 'Versions' },
  { key: 'configs', icon: '⚙️', label: 'Configs' },
  { key: 'analytics', icon: '📊', label: 'Analytics' },
  { key: 'webhooks', icon: '🔗', label: 'Webhooks' },
  { key: 'backups', icon: '💾', label: 'Backups' },
]

const shareUrl = computed(() => `https://api.shiaho.sbs/s/${project.value?.project_key || ''}`)

const chartData = computed(() => ({
  labels: (analytics.value?.trend || []).map(d => d.date),
  datasets: [
    { label: 'Opens', data: (analytics.value?.trend || []).map(d => d.opens), borderColor: '#6366f1', backgroundColor: 'rgba(99,102,241,0.1)', tension: 0.4, fill: true },
    { label: 'Active', data: (analytics.value?.trend || []).map(d => d.active), borderColor: '#10b981', backgroundColor: 'rgba(16,185,129,0.05)', tension: 0.4, fill: true },
  ],
}))
const chartOpts = { responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: true, labels: { color: '#888' } } },
  scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.06)' }, ticks: { color: '#888' } },
    x: { grid: { display: false }, ticks: { color: '#888' } } }
}

async function loadProject() {
  try {
    const res = await projectApi.get(projectId.value)
    project.value = res.data
  } catch (e) { showToast('Failed to load project', 'error') }
  finally { loading.value = false }
}

async function loadTab() {
  if (tab.value === 'codes') loadCodes()
  else if (tab.value === 'announcements') loadAnns()
  else if (tab.value === 'versions') loadVers()
  else if (tab.value === 'configs') loadCfgs()
  else if (tab.value === 'analytics') loadAnalytics()
  else if (tab.value === 'webhooks') loadWhs()
  else if (tab.value === 'backups') loadBkps()
}

async function loadCodes() { try { const r = await projectApi.listCodes(projectId.value); codes.value = r.data } catch (e) { console.error('loadCodes failed:', e); showToast('Failed to load codes', 'error') } }
async function loadAnns() { try { const r = await projectApi.listAnns(projectId.value); announcements.value = r.data } catch (e) { console.error('loadAnns failed:', e); showToast('Failed to load announcements', 'error') } }
async function loadVers() { try { const r = await projectApi.listVersions(projectId.value); versions.value = r.data } catch (e) { console.error('loadVers failed:', e); showToast('Failed to load versions', 'error') } }
async function loadCfgs() { try { const r = await projectApi.listCfgs(projectId.value); configs.value = r.data } catch (e) { console.error('loadCfgs failed:', e); showToast('Failed to load configs', 'error') } }
async function loadWhs() { try { const r = await projectApi.listWebhooks(projectId.value); webhooks.value = r.data } catch (e) { console.error('loadWhs failed:', e); showToast('Failed to load webhooks', 'error') } }
async function loadBkps() { try { const r = await projectApi.listBackups(projectId.value); backups.value = r.data } catch (e) { console.error('loadBkps failed:', e); showToast('Failed to load backups', 'error') } }
async function loadAnalytics() { try { const r = await projectApi.analytics(projectId.value, analyticsDays.value); analytics.value = r.data } catch (e) { console.error('loadAnalytics failed:', e); showToast('Failed to load analytics', 'error') } }

async function genCodes() {
  try { await projectApi.generateCodes(projectId.value, { count: codeCount.value, label: codeLabel.value || undefined }); showToast('Codes generated'); loadCodes() }
  catch (e) { showToast(e.detail || 'Generation failed', 'error') }
}
async function createAnn() {
  try { await projectApi.createAnn(projectId.value, annForm.value); showToast('Announcement published'); showAnnForm.value = false; loadAnns() }
  catch (e) { console.error('createAnn failed:', e); showToast('Publish failed', 'error') }
}
async function toggleAnn(a) {
  try { await projectApi.updateAnn(projectId.value, a.id, { ...a, is_active: !a.is_active }); loadAnns() } catch (e) { console.error('toggleAnn failed:', e); showToast('Operation failed', 'error') }
}
async function removeAnn(id) {
  if (!confirm('Delete announcement?')) return
  try { await projectApi.removeAnn(projectId.value, id); loadAnns() } catch (e) { console.error('removeAnn failed:', e); showToast('Delete failed', 'error') }
}
async function publishVer() {
  if (!verForm.value.file) return showToast('Please select APK file', 'error')
  publishing.value = true
  try {
    const fd = new FormData()
    fd.append('apk_file', verForm.value.file)
    fd.append('version_code', verForm.value.version_code)
    fd.append('version_name', verForm.value.version_name)
    fd.append('changelog', verForm.value.changelog || '')
    fd.append('is_force_update', verForm.value.is_force_update)
    fd.append('upload_to', verForm.value.upload_to)
    await projectApi.publishVersion(projectId.value, fd)
    showToast('Version published successfully'); showVerForm.value = false; loadVers()
  } catch (e) { showToast(e.detail || 'Publish failed', 'error') }
  finally { publishing.value = false }
}
async function createCfg() {
  try { await projectApi.createCfg(projectId.value, cfgForm.value); showToast('Configuration added'); showCfgForm.value = false; loadCfgs() }
  catch { showToast('Addition failed', 'error') }
}
async function removeCfg(id) {
  if (!confirm('Delete configuration?')) return
  try { await projectApi.removeCfg(projectId.value, id); loadCfgs() } catch (e) { console.error('removeCfg failed:', e); showToast('Delete failed', 'error') }
}
async function createWh() {
  if (!whForm.value.url) return showToast('Please enter URL', 'error')
  try {
    await projectApi.createWebhook(projectId.value, {
      url: whForm.value.url, secret: whForm.value.secret || undefined,
      events: Array.isArray(whForm.value.events) ? whForm.value.events.join(',') : whForm.value.events,
    })
    showToast('Webhook added'); showWhForm.value = false; loadWhs()
  } catch (e) { showToast(e.detail || 'Addition failed', 'error') }
}
async function removeWh(id) {
  if (!confirm('Delete Webhook?')) return
  try { await projectApi.removeWebhook(projectId.value, id); loadWhs() } catch (e) { console.error('removeWh failed:', e); showToast('Delete failed', 'error') }
}
async function doBackup() {
  if (!backupFile.value) return showToast('Please select ZIP file', 'error')
  backing.value = true
  try {
    const fd = new FormData()
    fd.append('file', backupFile.value)
    const r = await projectApi.backup(projectId.value, backupPlatform.value, fd)
    showToast(r.message || 'Backup completed'); loadBkps()
  } catch (e) { showToast(e.detail || 'Backup failed', 'error') }
  finally { backing.value = false }
}

function eventLabel(e) {
  return { code_activated: 'Activated', version_published: 'Published', daily_report: 'Report', announcement_created: 'Notice' }[e] || e
}
function fmtDate(d) { return d ? new Date(d).toLocaleString('en-US') : '' }

onMounted(() => { loadProject(); loadCodes() })
</script>

<style scoped>
.page-header { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0; }
.page-meta { display: flex; align-items: center; gap: 10px; margin-left: auto; }
.key-badge { background: var(--bg-input); padding: 4px 10px; border-radius: 6px; font-size: 11px; }

.tabs { display: flex; gap: 2px; border-bottom: 1px solid var(--border); flex-wrap: wrap; }
.tab {
  padding: 8px 14px; font-size: 13px; color: var(--text-secondary);
  background: none; border: none; border-bottom: 2px solid transparent;
  cursor: pointer; font-family: inherit; transition: var(--transition);
}
.tab:hover { color: var(--text-primary); }
.tab.active { color: var(--accent); border-bottom-color: var(--accent); }

.card-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid var(--border); }
.card-header h3 { font-size: 15px; font-weight: 600; margin: 0; }
.form-inline { padding: 14px 16px; display: flex; flex-direction: column; gap: 10px; border-bottom: 1px solid var(--border); }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; padding: 16px; }
.mini-stat { display: flex; flex-direction: column; align-items: center; padding: 12px; background: var(--bg-input); border-radius: var(--radius); }
.mini-value { font-size: 22px; font-weight: 700; }
.mini-label { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

.chart-container { height: 200px; padding: 0 16px 16px; }

.dist-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 10px; padding: 0 16px 16px; }
.dist-card { background: var(--bg-input); border-radius: var(--radius); padding: 12px; }
.dist-card h4 { font-size: 13px; margin: 0 0 8px; }
.dist-row { display: flex; justify-content: space-between; padding: 3px 0; font-size: 12px; border-bottom: 1px solid var(--border); }

.link { color: var(--accent); text-decoration: none; font-size: 13px; }
.link:hover { text-decoration: underline; }
</style>
