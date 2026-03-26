import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { guest: true },
    },
    {
        path: '/',
        component: () => import('../layouts/AdminLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            { path: '', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
            { path: 'users', name: 'Users', component: () => import('../views/Users.vue') },
            { path: 'activation', name: 'Activation', component: () => import('../views/Activation.vue') },
            { path: 'announcements', name: 'Announcements', component: () => import('../views/Announcements.vue') },
            { path: 'versions', name: 'Versions', component: () => import('../views/Versions.vue') },
            { path: 'config', name: 'Config', component: () => import('../views/RemoteConfig.vue') },
            { path: 'projects', name: 'Projects', component: () => import('../views/Projects.vue') },
            { path: 'projects/:id', name: 'ProjectDetail', component: () => import('../views/ProjectDetail.vue'), props: true },
            { path: 'store', name: 'StoreManagement', component: () => import('../views/StoreManagement.vue') },
            { path: 'moderation', name: 'Moderation', component: () => import('../views/Moderation.vue') },
            { path: 'push', name: 'PushNotifications', component: () => import('../views/PushNotifications.vue') },
            { path: 'maintenance', name: 'Maintenance', component: () => import('../views/Maintenance.vue') },
            { path: 'intelligence', name: 'Intelligence', component: () => import('../views/Intelligence.vue') },
            { path: 'analytics', name: 'Analytics', component: () => import('../views/Analytics.vue') },
        ],
    },
]

const router = createRouter({
    history: createWebHistory(),
    routes,
})

router.beforeEach((to, from, next) => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isLoggedIn) {
        next('/login')
    } else if (to.meta.guest && auth.isLoggedIn) {
        next('/')
    } else {
        next()
    }
})

export default router
