<template>
  <div>
    <div class="flex items-center justify-between mb-20">
      <h2 class="page-title">☁️ Cloud Project Management</h2>
      <button class="btn btn-primary" @click="showCreate = true">+ New Project</button>
    </div>

    <div v-if="loading" class="spinner"></div>

    <div v-else-if="!projects.length" class="empty-hero">
      <div class="empty-icon">☁️</div>
      <h3>No cloud projects yet</h3>
      <p>Once created, you can provide activation codes, announcements, version updates, and remote configuration for your apps.</p>
      <button class="btn btn-primary" @click="showCreate = true">Create First Project</button>
    </div>

    <div v-else class="projects-grid">
      <div v-for="p in projects" :key="p.id" class="project-card" @click="$router.push(`/projects/${p.id}`)">
        <div class="project-card-head">
          <div class="project-icon">{{ p.project_name[0]?.toUpperCase() }}</div>
          <div>
            <h3>{{ p.project_name }}</h3>
            <span class="text-muted" style="font-size:12px">{{ p.package_name || 'Package name not set' }}</span>
          </div>
        </div>
        <p class="project-desc">{{ p.description || 'No description' }}</p>
        <div class="project-meta">
          <code class="project-key">{{ p.project_key }}</code>
          <span :class="['badge', p.is_active ? 'badge-success' : 'badge-danger']">
            {{ p.is_active ? 'Active' : 'Disabled' }}
          </span>
        </div>
        <div class="project-date text-muted">Created on {{ formatDate(p.created_at) }}</div>
      </div>
    </div>

    <!-- Create Modal -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <h3 class="modal-title">New Cloud Project</h3>
        <div class="form-group">
          <label class="form-label">Project Name</label>
          <input v-model="form.project_name" class="input" placeholder="e.g. CoolApp" />
        </div>
        <div class="form-group">
          <label class="form-label">Package Name (Optional)</label>
          <input v-model="form.package_name" class="input" placeholder="e.g. com.example.coolapp" />
        </div>
        <div class="form-group">
          <label class="form-label">Description (Optional)</label>
          <textarea v-model="form.description" class="textarea" placeholder="Project Description"></textarea>
        </div>
        <div class="form-actions">
          <button class="btn btn-secondary" @click="showCreate = false">Cancel</button>
          <button class="btn btn-primary" @click="createProject" :disabled="creating">
            {{ creating ? 'Creating...' : 'Create Project' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, inject, onMounted } from 'vue'
import { projectApi } from '../api'

const showToast = inject('showToast')
const projects = ref([])
const loading = ref(true)
const showCreate = ref(false)
const creating = ref(false)
const form = ref({ project_name: '', package_name: '', description: '' })

async function loadProjects() {
  loading.value = true
  try {
    const res = await projectApi.list()
    projects.value = res.data
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function createProject() {
  if (!form.value.project_name) return showToast('Please enter project name', 'error')
  creating.value = true
  try {
    await projectApi.create(form.value)
    showToast('Project created')
    showCreate.value = false
    form.value = { project_name: '', package_name: '', description: '' }
    loadProjects()
  } catch (e) { showToast(e?.detail || 'Creation failed', 'error') }
  finally { creating.value = false }
}

function formatDate(d) { return d ? new Date(d).toLocaleDateString('en-US') : '-' }

onMounted(loadProjects)
</script>

<style scoped>
.page-title { font-size: 22px; font-weight: 700; margin: 0; }

.empty-hero {
  text-align: center; padding: 80px 20px;
  background: var(--bg-card); border: 1px dashed var(--border);
  border-radius: var(--radius);
}
.empty-icon { font-size: 48px; margin-bottom: 16px; }
.empty-hero h3 { font-size: 18px; margin-bottom: 8px; }
.empty-hero p { color: var(--text-muted); font-size: 14px; max-width: 400px; margin: 0 auto 20px; }

.projects-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }

.project-card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); padding: 20px; cursor: pointer;
  transition: var(--transition);
}
.project-card:hover { border-color: var(--accent); transform: translateY(-2px); box-shadow: 0 4px 20px rgba(99,102,241,0.1); }

.project-card-head { display: flex; align-items: center; gap: 14px; margin-bottom: 12px; }
.project-icon {
  width: 44px; height: 44px; border-radius: 12px;
  background: var(--gradient-2); display: flex;
  align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: white; flex-shrink: 0;
}
.project-card-head h3 { font-size: 16px; font-weight: 600; margin: 0; }
.project-desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 14px; line-height: 1.5; }
.project-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.project-key {
  font-size: 11px; padding: 3px 8px;
  background: var(--bg-input); border: 1px solid var(--border);
  border-radius: 4px; color: var(--accent);
}
.project-date { font-size: 12px; }
</style>
