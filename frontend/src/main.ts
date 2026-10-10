import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { API_UNAUTHORIZED_EVENT, setAccessTokenProvider } from './api/http'
import { useAuthStore } from './stores/auth'

const pinia = createPinia()
const app = createApp(App)
app.use(pinia)

const authStore = useAuthStore(pinia)
setAccessTokenProvider(() => authStore.accessToken)
window.addEventListener(API_UNAUTHORIZED_EVENT, () => {
  authStore.clearSession()
  const currentRoute = router.currentRoute.value
  if (currentRoute.meta.requiresAuth) {
    void router.replace({ name: 'login', query: { redirect: currentRoute.fullPath } })
  }
})

app
  .use(router)
  .mount('#app')
