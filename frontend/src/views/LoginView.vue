<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AuthShell from '../components/AuthShell.vue'
import { getApiErrorMessage } from '../api/errors'
import { useAuthStore } from '../stores/auth'

interface LoginForm {
  username: string
  password: string
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const isSubmitting = ref(false)
const form = reactive<LoginForm>({
  username: typeof route.query.username === 'string' ? route.query.username : '',
  password: '',
})

const rules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submitLogin(): Promise<void> {
  if (!formRef.value || isSubmitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  isSubmitting.value = true
  try {
    await authStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success(`欢迎回来，${authStore.currentUser?.username ?? form.username.trim()}。`)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.push(redirect)
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '登录暂时没有成功，请稍后重试。'))
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthShell eyebrow="WELCOME BACK" title="欢迎回来" intro="登录后继续记录你们的日常。">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="submitLogin"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          autocomplete="username"
          maxlength="64"
          placeholder="请输入用户名"
          size="large"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          autocomplete="current-password"
          maxlength="72"
          placeholder="请输入密码"
          show-password
          size="large"
          type="password"
        />
      </el-form-item>

      <el-button class="submit-button" :loading="isSubmitting" native-type="submit" size="large" type="primary">
        登录 PetCare
      </el-button>
    </el-form>

    <template #footer>
      还没有账户？<RouterLink to="/register">创建账户</RouterLink>
    </template>
  </AuthShell>
</template>
