<template>
  <div>
    <h2 class="page-title">📢 Announcement Management</h2>

    <div class="toolbar">
      <button class="btn btn-primary" @click="openCreate">+ New Announcement</button>
    </div>

    <div class="card">
      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>Title</th><th>Type</th><th>Audience</th><th>Status</th><th>Start Time</th><th>Action</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in items" :key="a.id">
              <td><strong>{{ a.title }}</strong></td>
              <td><span class="badge badge-info">{{ a.display_type }}</span></td>
              <td><span class="badge badge-purple">{{ audienceLabel(a.target_audience) }}</span></td>
              <td>
                <span :class="['badge', a.is_active ? 'badge-success' : 'badge-danger']">
                  {{ a.is_active ? 'Active' : 'Deactivated' }}
                </span>
              </td>
              <td>{{ formatDate(a.start_at) }}</td>
              <td class="flex gap-8">
                <button class="btn btn-secondary btn-sm" @click="openEdit(a)">Edit</button>
                <button class="btn btn-danger btn-sm" @click="removeItem(a.id)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!items.length" class="empty-state"><p>No announcements found</p></div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3 class="modal-title">{{ isEdit ? 'Edit Announcement' : 'New Announcement' }}</h3>
        <div class="form-group">
          <label class="form-label">Title</label>
          <input v-model="form.title" class="input" placeholder="Announcement Title" />
        </div>
        <div class="form-group">
          <label class="form-label">Content</label>
          <textarea v-model="form.content" class="textarea" placeholder="Announcement content (Markdown supported)"></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">English Content (Optional)</label>
          <textarea v-model="form.content_en" class="textarea" placeholder="English content"></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Display Type</label>
            <select v-model="form.display_type" class="select">
              <option value="popup">Popup</option>
              <option value="banner">Banner</option>
              <option value="fullscreen">Fullscreen</option>
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
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Start Time</label>
            <input v-model="form.start_at" type="datetime-local" class="input" />
          </div>
          <div class="form-group">
            <label class="form-label">End Time (Optional)</label>
            <input v-model="form.end_at" type="datetime-local" class="input" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Action URL (Optional)</label>
          <input v-model="form.action_url" class="input" placeholder="https://..." />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label"><input type="checkbox" v-model="form.is_active" /> Activate Now</label>
          </div>
          <div class="form-group">
            <label class="form-label"><input type="checkbox" v-model="form.dismissible" /> Dismissible</label>
          </div>
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
import { announcementApi } from '../api'

const showToast = inject('showToast')
const items = ref([])
const loading = ref(true)
const showModal = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)

const defaultForm = () => ({
  title: '', content: '', content_en: '', display_type: 'popup',
  target_audience: 'all', start_at: new Date().toISOString().slice(0,16),
  end_at: '', action_url: '', is_active: true, dismissible: true,
})
const form = ref(defaultForm())

function openCreate() { isEdit.value = false; form.value = defaultForm(); showModal.value = true }
function openEdit(a) {
  isEdit.value = true; editId.value = a.id
  form.value = { ...a, start_at: a.start_at?.slice(0,16) || '', end_at: a.end_at?.slice(0,16) || '' }
  showModal.value = true
}

async function loadItems() {
  loading.value = true
  try {
    const res = await announcementApi.list({ page: 1, page_size: 50 })
    items.value = res.data
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function saveItem() {
  saving.value = true
  try {
    const payload = { ...form.value }
    if (!payload.end_at) delete payload.end_at
    if (isEdit.value) await announcementApi.update(editId.value, payload)
    else await announcementApi.create(payload)
    showToast(isEdit.value ? 'Announcement updated' : 'Announcement created')
    showModal.value = false; loadItems()
  } catch (e) { showToast(e?.detail || 'Operation failed', 'error') }
  finally { saving.value = false }
}

async function removeItem(id) {
  if (!confirm('Are you sure you want to delete this announcement?')) return
  try { await announcementApi.remove(id); showToast('Deleted'); loadItems() }
  catch (e) { showToast('Deletion failed', 'error') }
}

function audienceLabel(a) { return { all: 'All', free: 'Free Users', pro: 'Pro Users', ultra: 'Ultra Users' }[a] || a }
function formatDate(d) { return d ? new Date(d).toLocaleString('en-US') : '-' }

onMounted(loadItems)
</script>
