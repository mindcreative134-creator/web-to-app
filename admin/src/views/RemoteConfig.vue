<template>
  <div>
    <h2 class="page-title">⚙️ Remote Config</h2>

    <div class="toolbar">
      <button class="btn btn-primary" @click="openCreate">+ New Config</button>
    </div>

    <div class="card">
      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>Key</th><th>Value</th><th>Type</th><th>Audience</th><th>Status</th><th>Action</th></tr>
          </thead>
          <tbody>
            <tr v-for="c in items" :key="c.id">
              <td><code>{{ c.config_key }}</code></td>
              <td style="max-width:200px;overflow:hidden;text-overflow:ellipsis">{{ c.config_value }}</td>
              <td><span class="badge badge-info">{{ c.value_type }}</span></td>
              <td><span class="badge badge-purple">{{ audienceLabel(c.target_audience) }}</span></td>
              <td>
                <span :class="['badge', c.is_active ? 'badge-success' : 'badge-danger']">
                  {{ c.is_active ? 'Enabled' : 'Disabled' }}
                </span>
              </td>
              <td class="flex gap-8">
                <button class="btn btn-secondary btn-sm" @click="openEdit(c)">Edit</button>
                <button class="btn btn-danger btn-sm" @click="removeItem(c.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!items.length" class="empty-state"><p>No configs found</p></div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3 class="modal-title">{{ isEdit ? 'Edit Config' : 'New Config' }}</h3>
        <div class="form-group">
          <label class="form-label">Key</label>
          <input v-model="form.config_key" class="input" placeholder="e.g. free_daily_build_limit" :disabled="isEdit" />
        </div>
        <div class="form-group">
          <label class="form-label">Value</label>
          <textarea v-model="form.config_value" class="textarea" placeholder="Config Value"></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Type</label>
            <select v-model="form.value_type" class="select">
              <option value="string">String</option>
              <option value="number">Number</option>
              <option value="boolean">Boolean</option>
              <option value="json">JSON</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Target Audience</label>
            <select v-model="form.target_audience" class="select">
              <option value="all">All</option>
              <option value="free">Free Users</option>
              <option value="pro">Pro Users</option>
              <option value="ultra">Ultra Users</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Description</label>
          <input v-model="form.description" class="input" placeholder="Config Description" />
        </div>
        <div v-if="isEdit" class="form-group">
          <label class="form-label"><input type="checkbox" v-model="form.is_active" /> Enabled</label>
        </div>
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showModal=false">Cancel</button>
          <button class="btn btn-primary" @click="saveItem" :disabled="saving">Save</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { configApi } from '../api'

const showToast = inject('showToast')
const items = ref([])
const loading = ref(true)
const showModal = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)

const defaultForm = () => ({
  config_key: '', config_value: '', value_type: 'string',
  target_audience: 'all', description: '', is_active: true,
})
const form = ref(defaultForm())

function openCreate() { isEdit.value = false; form.value = defaultForm(); showModal.value = true }
function openEdit(c) {
  isEdit.value = true; editId.value = c.id
  form.value = { ...c }
  showModal.value = true
}

async function loadItems() {
  loading.value = true
  try { const res = await configApi.list(); items.value = res.data }
  catch (e) { console.error(e) } finally { loading.value = false }
}

async function saveItem() {
  saving.value = true
  try {
    if (isEdit.value) await configApi.update(editId.value, form.value)
    else await configApi.create(form.value)
    showToast(isEdit.value ? 'Config updated' : 'Config created')
    showModal.value = false; loadItems()
  } catch (e) { showToast(e?.detail || 'Operation failed', 'error') }
  finally { saving.value = false }
}

async function removeItem(id) {
  if (!confirm('Are you sure you want to delete this config?')) return
  try { await configApi.remove(id); showToast('Deleted'); loadItems() }
  catch (e) { showToast('Delete failed', 'error') }
}

function audienceLabel(a) { return { all: 'All', free: 'Free Users', pro: 'Pro Users', ultra: 'Ultra Users' }[a] || a }

onMounted(loadItems)
</script>
