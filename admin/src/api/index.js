import axios from 'axios'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'https://api.shiaho.sbs/api/v1',
    timeout: 15000,
    headers: { 'Content-Type': 'application/json' },
})

// Request interceptor — attach JWT token
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('access_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// Response interceptor — handle 401 refresh
api.interceptors.response.use(
    (res) => res.data,
    async (error) => {
        if (error.response?.status === 401 && !error.config._retry) {
            error.config._retry = true
            const refreshToken = localStorage.getItem('refresh_token')
            if (refreshToken) {
                try {
                    const { data } = await axios.post(
                        `${api.defaults.baseURL}/auth/refresh`,
                        { refresh_token: refreshToken }
                    )
                    localStorage.setItem('access_token', data.data.access_token)
                    localStorage.setItem('refresh_token', data.data.refresh_token)
                    error.config.headers.Authorization = `Bearer ${data.data.access_token}`
                    return api(error.config)
                } catch {
                    localStorage.removeItem('access_token')
                    localStorage.removeItem('refresh_token')
                    window.location.href = '/login'
                }
            } else {
                localStorage.removeItem('access_token')
                localStorage.removeItem('refresh_token')
                window.location.href = '/login'
            }
        }
        return Promise.reject(error.response?.data || error)
    }
)

// ─── Auth ───
export const authApi = {
    login: (data) => api.post('/auth/login', data),
    refresh: (data) => api.post('/auth/refresh', data),
}

// ─── Admin ───
export const adminApi = {
    dashboard: () => api.get('/admin/dashboard'),
    listUsers: (params) => api.get('/admin/users', { params }),
    getUser: (id) => api.get(`/admin/users/${id}`),
    updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
}

// ─── Activation ───
export const activationApi = {
    generate: (data) => api.post('/admin/activation/generate', data),
    list: (params) => api.get('/admin/activation/list', { params }),
    disable: (id) => api.put(`/admin/activation/${id}/disable`),
    stats: () => api.get('/admin/activation/stats'),
    exportCsv: (params) => api.get('/admin/activation/export', { params, responseType: 'blob' }),
}

// ─── Announcements ───
export const announcementApi = {
    list: (params) => api.get('/admin/announcements', { params }),
    create: (data) => api.post('/admin/announcements', data),
    update: (id, data) => api.put(`/admin/announcements/${id}`, data),
    remove: (id) => api.delete(`/admin/announcements/${id}`),
}

// ─── App Version ───
export const versionApi = {
    list: (params) => api.get('/admin/app-versions', { params }),
    create: (data) => api.post('/admin/app-versions', data),
    togglePublish: (id) => api.put(`/admin/app-versions/${id}/publish`),
}

// ─── Remote Config ───
export const configApi = {
    list: () => api.get('/admin/remote-config'),
    create: (data) => api.post('/admin/remote-config', data),
    update: (id, data) => api.put(`/admin/remote-config/${id}`, data),
    remove: (id) => api.delete(`/admin/remote-config/${id}`),
}

// ─── Projects (Pro user cloud) ───
const P = '/projects'
export const projectApi = {
    list: () => api.get(P),
    create: (data) => api.post(P, data),
    get: (id) => api.get(`${P}/${id}`),
    update: (id, data) => api.put(`${P}/${id}`, data),
    remove: (id) => api.delete(`${P}/${id}`),
    // codes
    generateCodes: (id, data) => api.post(`${P}/${id}/codes/generate`, data),
    listCodes: (id, params) => api.get(`${P}/${id}/codes`, { params }),
    // announcements
    createAnn: (id, data) => api.post(`${P}/${id}/announcements`, data),
    listAnns: (id) => api.get(`${P}/${id}/announcements`),
    updateAnn: (id, annId, data) => api.put(`${P}/${id}/announcements/${annId}`, data),
    removeAnn: (id, annId) => api.delete(`${P}/${id}/announcements/${annId}`),
    // versions
    publishVersion: (id, formData) => api.post(`${P}/${id}/versions/publish`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000,
    }),
    listVersions: (id) => api.get(`${P}/${id}/versions`),
    // configs
    createCfg: (id, data) => api.post(`${P}/${id}/configs`, data),
    listCfgs: (id) => api.get(`${P}/${id}/configs`),
    updateCfg: (id, cfgId, data) => api.put(`${P}/${id}/configs/${cfgId}`, data),
    removeCfg: (id, cfgId) => api.delete(`${P}/${id}/configs/${cfgId}`),
    // analytics
    analytics: (id, days = 7) => api.get(`${P}/${id}/analytics`, { params: { days } }),
    // webhooks
    listWebhooks: (id) => api.get(`${P}/${id}/webhooks`),
    createWebhook: (id, data) => api.post(`${P}/${id}/webhooks`, data),
    removeWebhook: (id, whId) => api.delete(`${P}/${id}/webhooks/${whId}`),
    // backups
    listBackups: (id) => api.get(`${P}/${id}/backups`),
    backup: (id, platform, formData) => api.post(`${P}/${id}/backup?platform=${platform}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }, timeout: 120000,
    }),
}

// ─── Community Moderation (Admin) ───
export const moderationApi = {
    stats: () => api.get('/admin/community/stats'),
    listReports: (params) => api.get('/admin/community/reports', { params }),
    resolveReport: (id, data) => api.put(`/admin/community/reports/${id}/resolve`, null, { params: data }),
    listPendingModules: (params) => api.get('/admin/community/modules', { params }),
    approveModule: (id) => api.put(`/admin/community/modules/${id}/approve`),
    rejectModule: (id, data) => api.put(`/admin/community/modules/${id}/reject`, data),
    deleteModule: (id) => api.delete(`/admin/community/modules/${id}`),
    deleteComment: (id) => api.delete(`/admin/community/comments/${id}`),
}

// ─── Push Notifications (Admin) ───
export const pushApi = {
    send: (data) => api.post('/admin/push/global', data),
    sendToUser: (data) => api.post('/admin/push/user', data),
    history: (params) => api.get('/admin/push/history', { params }),
    stats: () => api.get('/admin/push/stats'),
}

// ─── System Maintenance (Admin) ───
export const maintenanceApi = {
    healthCheck: () => api.get('/admin/health'),
    cleanup: (data) => api.post('/admin/cleanup/all'),
    cleanupViewLogs: (days) => api.post('/admin/cleanup/view-logs', null, { params: { days } }),
    systemStats: () => api.get('/admin/stats'),
    clearCache: () => api.post('/admin/clear-cache'),
    dbBackup: () => api.post('/admin/backup'),
    listBackups: () => api.get('/admin/backups'),
    downloadBackup: (filename) => {
        const token = localStorage.getItem('access_token')
        const base = import.meta.env.VITE_API_URL || 'https://api.shiaho.sbs/api/v1'
        window.open(`${base}/admin/backups/${encodeURIComponent(filename)}?token=${token}`, '_blank')
    },
    deleteBackup: (filename) => api.delete(`/admin/backups/${encodeURIComponent(filename)}`),
    exportUsers: () => api.get('/admin/export/users', { responseType: 'blob' }),
    exportProjects: () => api.get('/admin/export/projects', { responseType: 'blob' }),
}

// ─── Intelligence Dashboard (Admin) ───
export const intelligenceApi = {
    dashboard: () => api.get('/admin/intelligence/overview'),
    threats: (params) => api.get('/admin/intelligence/threats', { params }),
    circuitBreakers: () => api.get('/admin/intelligence/circuits'),
    abuse: (params) => api.get('/admin/intelligence/abuse', { params }),
    advanced: () => api.get('/admin/intelligence/advanced'),
}

// ─── Store Management (Admin) ───
export const storeApi = {
    stats: () => api.get('/admin/store/stats'),
    listItems: (params) => api.get('/admin/store/items', { params }),
    getItem: (id) => api.get(`/admin/store/items/${id}`),
    updateItem: (id, data) => api.put(`/admin/store/items/${id}`, data),
    deleteItem: (id) => api.delete(`/admin/store/items/${id}`),
    batch: (data) => api.post('/admin/store/batch', data),
}

// ─── Analytics (Admin) ───
export const analyticsApi = {
    trends: (scope = 'daily') => api.get('/admin/analytics/trends', { params: { scope } }),
}

export default api

