<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <div class="login-card">
      <div class="login-brand">
        <div class="login-logo">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"/><path d="M2 17l10 5 10-5"/><path d="M2 12l10 5 10-5"/></svg>
        </div>
        <h1>WebToApp Pro</h1>
        <p class="text-sm text-muted">Management Console</p>
      </div>

      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label class="form-label">Email</label>
          <input v-model="email" type="email" class="input" placeholder="admin@example.com" required autofocus />
        </div>
        <div class="form-group">
          <label class="form-label">Password</label>
          <input v-model="password" type="password" class="input" placeholder="Enter password" required />
        </div>
        <button type="submit" class="btn btn-primary login-btn" :disabled="loading">
          <span v-if="loading" class="spinner-inline"></span>
          <span v-else>Login</span>
        </button>
        <p v-if="error" class="error-text">{{ error }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '../api'

const router = useRouter()
const email = ref(''), password = ref(''), error = ref(''), loading = ref(false)

async function handleLogin() {
  loading.value = true; error.value = ''
  try {
    const res = await authApi.login({ account: email.value, password: password.value })
    const user = res.data.user || res.data
    const tokens = res.data.tokens || res.data

    // Verify admin privileges
    if (!user.is_admin) {
      error.value = 'This account does not have admin privileges'
      return
    }

    localStorage.setItem('access_token', tokens.access_token)
    localStorage.setItem('refresh_token', tokens.refresh_token)
    localStorage.setItem('user', JSON.stringify(user))
    router.push('/')
  } catch (e) {
    error.value = e?.detail || 'Login failed'
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.login-bg {
  position: absolute; inset: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(99,102,241,0.08) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(167,139,250,0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(244,114,182,0.05) 0%, transparent 50%);
}
.login-card {
  position: relative; z-index: 1;
  width: 380px; padding: 44px 36px;
  background: rgba(24, 24, 32, 0.7);
  backdrop-filter: blur(60px) saturate(1.5);
  border: 1px solid var(--border);
  border-radius: var(--r-xl);
  box-shadow: var(--shadow-lg);
  animation: modalIn 0.6s var(--ease-spring);
}
.login-brand { text-align: center; margin-bottom: 32px; }
.login-logo {
  width: 52px; height: 52px; margin: 0 auto 16px;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--accent), #a78bfa);
  border-radius: 14px; color: #fff;
  box-shadow: 0 8px 24px rgba(99,102,241,0.25);
}
.login-brand h1 { font-size: 1.4rem; font-weight: 700; letter-spacing: -0.03em; margin-bottom: 4px; }
.login-form { display: flex; flex-direction: column; gap: 16px; }
.login-btn { width: 100%; height: 44px; font-size: 0.9rem; font-weight: 600; margin-top: 4px; }
.error-text { color: var(--red); font-size: 0.8rem; text-align: center; }
.spinner-inline {
  width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.2);
  border-top-color: #fff; border-radius: 50%; animation: spin 0.7s linear infinite;
}
</style>
