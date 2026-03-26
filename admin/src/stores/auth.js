import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'

export const useAuthStore = defineStore('auth', () => {
    const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
    const accessToken = ref(localStorage.getItem('access_token') || '')

    const isLoggedIn = computed(() => !!accessToken.value)
    const isAdmin = computed(() => user.value?.is_admin ?? false)

    async function login(email, password) {
        const res = await authApi.login({ account: email, password })
        const userData = res.data.user || res.data
        const tokens = res.data.tokens || res.data
        user.value = userData
        accessToken.value = tokens.access_token
        localStorage.setItem('user', JSON.stringify(userData))
        localStorage.setItem('access_token', tokens.access_token)
        localStorage.setItem('refresh_token', tokens.refresh_token)
        return res
    }

    function logout() {
        user.value = null
        accessToken.value = ''
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        localStorage.removeItem('user')
    }

    return { user, accessToken, isLoggedIn, isAdmin, login, logout }
})
