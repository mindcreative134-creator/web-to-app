<template>
  <div class="slide-up">
    <div class="page-header">
      <h1>Overview</h1>
      <p class="text-secondary text-sm">Platform operational data at a glance</p>
    </div>

    <div v-if="loading" class="spinner"></div>
    <template v-else>
      <!-- Stat Cards -->
      <div class="metrics-grid">
        <div class="metric-card" v-for="(m, i) in metrics" :key="i" :style="{ animationDelay: i * 60 + 'ms' }">
          <div class="metric-icon" :style="{ background: m.bg }">
            <span v-html="m.svg"></span>
          </div>
          <div class="metric-body">
            <span class="metric-value">{{ m.value }}</span>
            <span class="metric-label">{{ m.label }}</span>
          </div>
          <span v-if="m.sub" class="metric-sub">{{ m.sub }}</span>
        </div>
      </div>

      <!-- Chart -->
      <div class="card mt-24 slide-up" style="animation-delay:200ms">
        <div class="section-header">
          <h3>Activity Trend</h3>
          <span class="text-xs text-muted">Last 7 Days</span>
        </div>
        <div class="chart-wrap">
          <Line :data="chartData" :options="chartOptions" />
        </div>
      </div>

      <!-- Quick Nav -->
      <div class="quick-grid mt-24">
        <router-link v-for="q in quickLinks" :key="q.to" :to="q.to" class="quick-card slide-up" :style="{ animationDelay: q.delay }">
          <span class="quick-icon" v-html="q.svg"></span>
          <span class="quick-label">{{ q.label }}</span>
          <svg class="quick-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>
        </router-link>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { Line } from 'vue-chartjs'
import { Chart, CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip } from 'chart.js'
import { adminApi } from '../api'

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip)

const loading = ref(true)
const stats = ref({})

const metrics = computed(() => {
  const s = stats.value
  return [
    { label: 'Total Users', value: s.total_users || 0, sub: s.new_users_today ? `Today +${s.new_users_today}` : '', bg: 'linear-gradient(135deg, rgba(99,102,241,0.15), rgba(99,102,241,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#818cf8" stroke-width="1.8"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2"/><circle cx="9" cy="7" r="4"/></svg>' },
    { label: 'Pro Users', value: s.pro_users || 0, sub: `${s.pro_rate || 0}%`, bg: 'linear-gradient(135deg, rgba(96,165,250,0.15), rgba(96,165,250,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" stroke-width="1.8"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>' },
    { label: 'Ultra Users', value: s.ultra_users || 0, bg: 'linear-gradient(135deg, rgba(251,191,36,0.15), rgba(251,191,36,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" stroke-width="1.8"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>' },
    { label: 'Estimated MRR', value: `$${s.mrr || 0}`, bg: 'linear-gradient(135deg, rgba(52,211,153,0.15), rgba(52,211,153,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="1.8"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>' },
    { label: 'Today Active', value: s.active_today || 0, bg: 'linear-gradient(135deg, rgba(167,139,250,0.15), rgba(167,139,250,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#a78bfa" stroke-width="1.8"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/></svg>' },
    { label: 'Cloud Projects', value: s.total_projects || 0, sub: `Active ${s.active_projects || 0}`, bg: 'linear-gradient(135deg, rgba(244,114,182,0.15), rgba(244,114,182,0.05))', svg: '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f472b6" stroke-width="1.8"><path d="M18 10h-1.26A8 8 0 109 20h9a5 5 0 000-10z"/></svg>' },
  ]
})

const chartData = computed(() => ({
  labels: (stats.value.login_trend || []).map(d => d.date),
  datasets: [{
    label: 'Active Users',
    data: (stats.value.login_trend || []).map(d => d.count),
    borderColor: '#6366f1', backgroundColor: 'rgba(99,102,241,0.06)',
    tension: 0.4, fill: true, pointRadius: 3, pointBackgroundColor: '#6366f1',
    borderWidth: 2,
  }],
}))
const chartOptions = {
  responsive: true, maintainAspectRatio: false,
  plugins: { legend: { display: false }, tooltip: { backgroundColor: 'rgba(24,24,32,0.95)', titleFont: { family: 'Inter' }, bodyFont: { family: 'Inter' }, borderColor: 'rgba(255,255,255,0.1)', borderWidth: 1, cornerRadius: 8, padding: 10 } },
  scales: { y: { beginAtZero: true, grid: { color: 'rgba(255,255,255,0.04)' }, ticks: { color: 'rgba(255,255,255,0.3)', font: { family: 'Inter', size: 11 } } },
    x: { grid: { display: false }, ticks: { color: 'rgba(255,255,255,0.3)', font: { family: 'Inter', size: 11 } } } }
}

const quickLinks = [
  { to: '/analytics', label: 'Analytics', svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>', delay: '250ms' },
  { to: '/users', label: 'Users', svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2"/><circle cx="9" cy="7" r="4"/></svg>', delay: '310ms' },
  { to: '/activation', label: 'Activation', svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M21 2l-2 2m-7.61 7.61a5.5 5.5 0 11-7.78 7.78 5.5 5.5 0 017.78-7.78z"/></svg>', delay: '370ms' },
  { to: '/config', label: 'Plans & Config', svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33"/></svg>', delay: '430ms' },
]

onMounted(async () => {
  try { const res = await adminApi.dashboard(); stats.value = res.data }
  catch (e) { console.error(e) }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { margin-bottom: 28px; }
.page-header h1 { margin-bottom: 4px; }

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.metric-card {
  background: var(--bg-card); backdrop-filter: blur(40px);
  border: 1px solid var(--border); border-radius: var(--r-lg);
  padding: 18px; display: flex; align-items: flex-start; gap: 14px;
  position: relative; overflow: hidden;
  transition: transform var(--t-normal), box-shadow var(--t-normal);
  animation: slideUp 0.5s var(--ease-spring) both;
}
.metric-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.metric-icon {
  width: 40px; height: 40px; min-width: 40px; border-radius: var(--r-sm);
  display: flex; align-items: center; justify-content: center;
}
.metric-body { display: flex; flex-direction: column; }
.metric-value { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.03em; }
.metric-label { font-size: 0.75rem; color: var(--text-muted); margin-top: 2px; }
.metric-sub { position: absolute; top: 14px; right: 16px; font-size: 0.7rem; color: var(--text-muted); }

.section-header { display: flex; justify-content: space-between; align-items: center; padding: 18px 20px 0; }
.chart-wrap { height: 220px; padding: 12px 20px 20px; }

.quick-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 12px;
}
.quick-card {
  display: flex; align-items: center; gap: 12px;
  padding: 16px 18px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--r-md); text-decoration: none; color: var(--text-secondary);
  transition: all var(--t-normal); animation: slideUp 0.5s var(--ease-spring) both;
}
.quick-card:hover { background: var(--bg-card-hover); color: var(--text-primary); border-color: rgba(255,255,255,0.1); transform: translateY(-1px); }
.quick-icon { opacity: 0.5; }
.quick-label { flex: 1; font-size: 0.87rem; font-weight: 500; }
.quick-arrow { opacity: 0.2; transition: transform var(--t-fast); }
.quick-card:hover .quick-arrow { transform: translateX(3px); opacity: 0.5; }
</style>
