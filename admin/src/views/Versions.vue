<template>
  <div>
    <h2 class="page-title">📦 Version Management</h2>

    <div class="toolbar">
      <button class="btn btn-primary" @click="openCreate">+ Publish New Version</button>
    </div>

    <div class="card">
      <div v-if="loading" class="spinner"></div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>Version Code</th><th>Version Name</th><th>Title</th><th>Force Update</th><th>Status</th><th>Operation</th></tr>
          </thead>
          <tbody>
            <tr v-for="v in items" :key="v.id">
              <td><strong>{{ v.version_code }}</strong></td>
              <td>v{{ v.version_name }}</td>
              <td>{{ v.title }}</td>
              <td>
                <span :class="['badge', v.is_force_update ? 'badge-danger' : 'badge-success']">
                  {{ v.is_force_update ? 'Force' : 'Optional' }}
                </span>
              </td>
              <td>
                <span :class="['badge', v.is_published ? 'badge-success' : 'badge-warning']">
                  {{ v.is_published ? 'Published' : 'Draft' }}
                </span>
              </td>
              <td class="flex gap-8">
                <button class="btn btn-sm" :class="v.is_published ? 'btn-secondary' : 'btn-success'"
                  @click="togglePublish(v.id)">
                  {{ v.is_published ? 'Unpublish' : 'Publish' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!items.length" class="empty-state"><p>No versions found</p></div>
      </div>
    </div>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal">
        <h3 class="modal-title">Publish New Version</h3>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">Version Code (code)</label>
            <input v-model.number="form.version_code" type="number" class="input" placeholder="e.g. 200" />
          </div>
          <div class="form-group">
            <label class="form-label">Version Name</label>
            <input v-model="form.version_name" class="input" placeholder="e.g. 2.0.0" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">Title</label>
          <input v-model="form.title" class="input" placeholder="Update Title" />
        </div>
        <div class="form-group">
          <label class="form-label">Changelog</label>
          <textarea v-model="form.changelog" class="textarea" placeholder="- Feature A\n- Fixed Bug B"></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">Download URL</label>
          <input v-model="form.download_url" class="input" placeholder="https://..." />
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">File Size (bytes)</label>
            <input v-model.number="form.file_size" type="number" class="input" />
          </div>
          <div class="form-group">
            <label class="form-label">Min Version Code</label>
            <input v-model.number="form.min_version_code" type="number" class="input" placeholder="Force update if below this version" />
          </div>
        </div>
        <div class="form-group">
          <label class="form-label"><input type="checkbox" v-model="form.is_force_update" /> Force Update</label>
        </div>
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showModal=false">Cancel</button>
          <button class="btn btn-primary" @click="createVersion" :disabled="saving">Save</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { versionApi } from '../api'

const showToast = inject('showToast')
const items = ref([])
const loading = ref(true)
const showModal = ref(false)
const saving = ref(false)
const form = ref({
  version_code: 0, version_name: '', title: '', changelog: '',
  download_url: '', file_size: 0, min_version_code: 0, is_force_update: false,
})

function openCreate() {
  form.value = { version_code: 0, version_name: '', title: '', changelog: '',
    download_url: '', file_size: 0, min_version_code: 0, is_force_update: false }
  showModal.value = true
}

async function loadItems() {
  loading.value = true
  try { const res = await versionApi.list({ page: 1, page_size: 50 }); items.value = res.data }
  catch (e) { console.error(e) } finally { loading.value = false }
}

async function createVersion() {
  saving.value = true
  try {
    await versionApi.create(form.value)
    showToast('Version created'); showModal.value = false; loadItems()
  } catch (e) { showToast(e?.detail || 'Creation failed', 'error') }
  finally { saving.value = false }
}

async function togglePublish(id) {
  try { await versionApi.togglePublish(id); showToast('Status updated'); loadItems() }
  catch (e) { showToast('Operation failed', 'error') }
}

onMounted(loadItems)
</script>
