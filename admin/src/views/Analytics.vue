<template>
  <div class="analytics-page slide-up">
    <div class="page-header">
      <div>
        <h1>📊 Analytics</h1>
        <p class="page-desc">Multi-dimensional trend data, platform growth insights</p>
      </div>
    </div>

    <!-- Scope Switcher -->
    <div class="scope-bar">
      <button v-for="s in scopes" :key="s.value"
        :class="['scope-btn', { active: scope === s.value }]"
        @click="switchScope(s.value)">
        {{ s.label }}
      </button>
    </div>

    <!-- Summary Cards -->
    <div class="summary-row" v-if="summary">
      <div class="summary-card pro">
        <div class="summary-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#60a5fa" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
        </div>
        <div class="summary-info">
          <span class="summary-value">{{ summary.pro_total }}</span>
          <span class="summary-label">Pro Users</span>
        </div>
      </div>
      <div class="summary-card ultra">
        <div class="summary-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fbbf24" stroke-width="2"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
        </div>
        <div class="summary-info">
          <span class="summary-value">{{ summary.ultra_total }}</span>
          <span class="summary-label">Ultra Users</span>
        </div>
      </div>
      <div class="summary-card apps">
        <div class="summary-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#f472b6" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
        </div>
        <div class="summary-info">
          <span class="summary-value">{{ summary.store_apps_total }}</span>
          <span class="summary-label">Market Apps</span>
        </div>
      </div>
      <div class="summary-card modules">
        <div class="summary-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#34d399" stroke-width="2"><path d="M16.5 9.4l-9-5.19"/><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
        </div>
        <div class="summary-info">
          <span class="summary-value">{{ summary.store_modules_total }}</span>
          <span class="summary-label">Market Modules</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>Loading trend data...</span>
    </div>

    <template v-else>
      <!-- Chart 1: Active Users -->
      <div class="chart-card slide-up" style="animation-delay:100ms">
        <div class="chart-header">
          <div>
            <h3>Active User Trend</h3>
            <span class="chart-sub">{{ scopeLabel }} unique active users</span>
          </div>
          <div class="chart-legend">
            <span class="legend-dot" style="background:#818cf8"></span> Active Users
          </div>
        </div>
        <div class="chart-body">
          <Line :data="activeChartData" :options="chartOpts('Active Users')" />
        </div>
      </div>

      <!-- Chart 2: Paid Subscribers -->
      <div class="chart-card slide-up" style="animation-delay:200ms">
        <div class="chart-header">
          <div>
            <h3>Paid User Trend</h3>
            <span class="chart-sub">{{ scopeLabel }} new paid users</span>
          </div>
          <div class="chart-legend">
            <span class="legend-dot" style="background:#60a5fa"></span> Pro
            <span class="legend-dot" style="background:#fbbf24"></span> Ultra
          </div>
        </div>
        <div class="chart-body">
          <Line :data="paidChartData" :options="chartOpts('Paid Users')" />
        </div>
      </div>

      <!-- Chart 3: Store Trends -->
      <div class="chart-card slide-up" style="animation-delay:300ms">
        <div class="chart-header">
          <div>
            <h3>Market Content Trend</h3>
            <span class="chart-sub">{{ scopeLabel }} new apps & modules</span>
          </div>
          <div class="chart-legend">
            <span class="legend-dot" style="background:#f472b6"></span> Apps
            <span class="legend-dot" style="background:#34d399"></span> Modules
          </div>
        </div>
        <div class="chart-body">
          <Line :data="storeChartData" :options="chartOpts('Market')" />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart, CategoryScale, LinearScale, PointElement, LineElement,
  Filler, Tooltip, Legend
} from 'chart.js'
import { analyticsApi } from '../api'

Chart.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend)

const scope = ref('daily')
const loading = ref(true)
const trendData = ref({})
const summary = ref(null)

const scopes = [
  { value: 'daily', label: 'Daily (30d)' },
  { value: 'monthly', label: 'Monthly (12m)' },
  { value: 'yearly', label: 'Yearly' },
  { value: 'all', label: 'Lifetime' },
]

const scopeLabel = computed(() => {
  return { daily: 'Daily', monthly: 'Monthly', yearly: 'Yearly', all: 'History' }[scope.value] || ''
})

async function loadData() {
  loading.value = true
  try {
    const res = await analyticsApi.trends(scope.value)
    trendData.value = res.data || {}
    summary.value = res.data?.summary || null
  } catch (e) {
    console.error('Failed to load trends', e)
  } finally {
    loading.value = false
  }
}

function switchScope(s) {
  scope.value = s
  loadData()
}

// ── Chart Data ──

function makeDataset(label, data, color, fill = true) {
  return {
    label,
    data: (data || []).map(d => d.value),
    borderColor: color,
    backgroundColor: fill ? color.replace(')', ',0.08)').replace('rgb', 'rgba') : 'transparent',
    tension: 0.4,
    fill,
    pointRadius: data?.length > 60 ? 0 : 3,
    pointBackgroundColor: color,
    pointHoverRadius: 5,
    borderWidth: 2,
  }
}

const activeChartData = computed(() => ({
  labels: (trendData.value.active || []).map(d => d.label),
  datasets: [
    makeDataset('Active Users', trendData.value.active, 'rgb(129,140,248)'),
  ],
}))

const paidChartData = computed(() => ({
  labels: (trendData.value.pro_subscribers || []).map(d => d.label),
  datasets: [
    makeDataset('Pro', trendData.value.pro_subscribers, 'rgb(96,165,250)'),
    makeDataset('Ultra', trendData.value.ultra_subscribers, 'rgb(251,191,36)'),
  ],
}))

const storeChartData = computed(() => ({
  labels: (trendData.value.store_apps || []).map(d => d.label),
  datasets: [
    makeDataset('Apps', trendData.value.store_apps, 'rgb(244,114,182)'),
    makeDataset('Modules', trendData.value.store_modules, 'rgb(52,211,153)'),
  ],
}))

function chartOpts(title) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: 'rgba(17,17,27,0.95)',
        titleFont: { family: 'Inter', size: 12, weight: '600' },
        bodyFont: { family: 'Inter', size: 12 },
        borderColor: 'rgba(255,255,255,0.08)',
        borderWidth: 1,
        cornerRadius: 10,
        padding: 12,
        displayColors: true,
        boxWidth: 8,
        boxHeight: 8,
        boxPadding: 4,
        usePointStyle: true,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(255,255,255,0.04)', drawBorder: false },
        ticks: { color: 'rgba(255,255,255,0.3)', font: { family: 'Inter', size: 11 }, padding: 8 },
      },
      x: {
        grid: { display: false },
        ticks: {
          color: 'rgba(255,255,255,0.3)',
          font: { family: 'Inter', size: 11 },
          maxRotation: 45,
          padding: 4,
          autoSkip: true,
          maxTicksLimit: 15,
        },
      },
    },
  }
}

onMounted(loadData)
</script>

<style scoped>
.analytics-page { max-width: 1100px; }

.page-header { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: flex-end; }
.page-header h1 { font-size: 1.5rem; font-weight: 700; letter-spacing: -0.03em; }
.page-desc { color: var(--text-secondary); font-size: 0.87rem; margin-top: 4px; }

/* ── Scope Bar ── */
.scope-bar {
  display: flex; gap: 6px; margin-bottom: 24px;
  padding: 4px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--r-md); width: fit-content;
}
.scope-btn {
  padding: 8px 18px; border: none; border-radius: var(--r-sm);
  font-family: var(--font); font-size: 0.82rem; font-weight: 500;
  background: transparent; color: var(--text-secondary); cursor: pointer;
  transition: all var(--t-fast);
}
.scope-btn:hover { color: var(--text-primary); background: rgba(255,255,255,0.04); }
.scope-btn.active {
  background: var(--accent); color: #fff;
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}

/* ── Summary ── */
.summary-row {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px;
}
.summary-card {
  display: flex; align-items: center; gap: 14px;
  padding: 18px 20px; border-radius: var(--r-md);
  background: var(--bg-card); border: 1px solid var(--border);
  transition: transform var(--t-fast), box-shadow var(--t-fast);
}
.summary-card:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.summary-icon {
  width: 42px; height: 42px; min-width: 42px; border-radius: var(--r-sm);
  display: flex; align-items: center; justify-content: center;
}
.summary-card.pro .summary-icon { background: rgba(96,165,250,0.12); }
.summary-card.ultra .summary-icon { background: rgba(251,191,36,0.12); }
.summary-card.apps .summary-icon { background: rgba(244,114,182,0.12); }
.summary-card.modules .summary-icon { background: rgba(52,211,153,0.12); }
.summary-info { display: flex; flex-direction: column; }
.summary-value { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.03em; }
.summary-label { font-size: 0.75rem; color: var(--text-muted); margin-top: 2px; }

/* ── Chart Cards ── */
.chart-card {
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--r-lg); margin-bottom: 20px; overflow: hidden;
  transition: box-shadow var(--t-normal);
}
.chart-card:hover { box-shadow: var(--shadow-md); }
.chart-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  padding: 22px 24px 0;
}
.chart-header h3 { font-size: 1.05rem; font-weight: 600; margin-bottom: 2px; }
.chart-sub { font-size: 0.75rem; color: var(--text-muted); }
.chart-legend {
  display: flex; align-items: center; gap: 12px;
  font-size: 0.78rem; color: var(--text-secondary);
}
.legend-dot {
  width: 8px; height: 8px; border-radius: 50%; display: inline-block; margin-right: 4px;
}
.chart-body { height: 260px; padding: 12px 24px 20px; }

/* ── States ── */
.loading-state {
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; padding: 80px 20px; color: var(--text-secondary);
}

@media (max-width: 900px) {
  .summary-row { grid-template-columns: repeat(2, 1fr); }
  .scope-bar { flex-wrap: wrap; }
}
@media (max-width: 600px) {
  .summary-row { grid-template-columns: 1fr; }
}
</style>
