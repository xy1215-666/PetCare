<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const displayName = computed(() => authStore.currentUser?.username ?? '宠物家人')
const avatarLetter = computed(() => displayName.value.slice(0, 1).toUpperCase())

function logout(): void {
  authStore.clearSession()
  void router.replace({ name: 'login' })
  ElMessage.success('已安全退出登录。')
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="topbar-inner">
        <RouterLink class="brand" :to="{ name: 'dashboard' }" aria-label="PetCare AI 首页">
          <span class="brand-icon" aria-hidden="true">P</span>
          <span>PetCare <strong>AI</strong></span>
        </RouterLink>

        <nav class="main-nav" aria-label="主导航">
          <RouterLink :to="{ name: 'dashboard' }">首页</RouterLink>
          <RouterLink :to="{ name: 'pets' }">我的宠物</RouterLink>
          <RouterLink :to="{ name: 'expenses' }">消费记录</RouterLink>
          <RouterLink :to="{ name: 'ai-chat' }">智能助手</RouterLink>
        </nav>

        <div class="account-chip">
          <span class="avatar" aria-hidden="true">{{ avatarLetter }}</span>
          <span class="account-name">{{ displayName }}</span>
          <button class="logout-button" type="button" @click="logout">退出登录</button>
        </div>
      </div>
    </header>

    <main class="page-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at 4% 18%, rgb(226 237 222 / 52%), transparent 26%),
    #f6f7f2;
}

.topbar {
  position: sticky;
  z-index: 10;
  top: 0;
  border-bottom: 1px solid rgb(72 98 78 / 9%);
  background: rgb(255 255 255 / 88%);
  backdrop-filter: blur(14px);
}

.topbar-inner {
  display: flex;
  width: min(100% - 48px, 1180px);
  min-height: 76px;
  align-items: center;
  margin: 0 auto;
}

.brand {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 11px;
  color: #2b3d31;
  font-size: 18px;
  font-weight: 500;
}

.brand strong {
  color: #65856c;
  font-weight: 700;
}

.brand-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 13px;
  background: #55856d;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
}

.main-nav {
  display: flex;
  height: 76px;
  align-items: center;
  gap: 8px;
  margin-left: clamp(32px, 6vw, 82px);
}

.main-nav a {
  padding: 10px 15px;
  border-radius: 12px;
  color: #69766d;
  font-size: 14px;
  transition: background 150ms ease, color 150ms ease;
}

.main-nav a:hover,
.main-nav a.router-link-active {
  background: #edf3eb;
  color: #41694f;
}

.account-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  color: #47564b;
  font-size: 13px;
  font-weight: 600;
}

.logout-button {
  padding: 8px 11px;
  border: 1px solid #e1e8df;
  border-radius: 11px;
  background: #fff;
  color: #617468;
  cursor: pointer;
  font-size: 12px;
  transition: border-color 150ms ease, color 150ms ease, background 150ms ease;
}

.logout-button:hover {
  border-color: #cdddcf;
  background: #f5f8f3;
  color: #41694f;
}

.avatar {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #dbe7da;
  border-radius: 50%;
  background: #eef4eb;
  color: #4b765b;
  font-weight: 700;
}

.page-content {
  width: min(100% - 48px, 1120px);
  margin: 0 auto;
  padding: 48px 0 72px;
}

@media (max-width: 640px) {
  .topbar-inner {
    width: min(100% - 32px, 1180px);
    min-height: 66px;
    flex-wrap: wrap;
  }

  .brand {
    font-size: 16px;
  }

  .brand-icon {
    width: 34px;
    height: 34px;
  }

  .main-nav {
    order: 3;
    width: 100%;
    height: 48px;
    gap: 0;
    margin: 0;
  }

  .main-nav a {
    padding: 9px 12px;
  }

  .account-name {
    display: none;
  }

  .logout-button {
    padding: 7px 9px;
  }

  .page-content {
    width: min(100% - 32px, 1120px);
    padding: 30px 0 52px;
  }
}
</style>
