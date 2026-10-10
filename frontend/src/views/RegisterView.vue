<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthShell from '../components/AuthShell.vue'
import { getApiErrorMessage } from '../api/errors'
import { useAuthStore } from '../stores/auth'

interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  email: string
  phone: string
}

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const isSubmitting = ref(false)
const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
})

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || value.length < 12 || value.length > 72 || new TextEncoder().encode(value).length > 72) {
    callback(new Error('密码需至少 12 位，且 UTF-8 长度不超过 72 字节'))
    return
  }
  callback()
}

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,32}$/, message: '使用 4–32 位字母、数字或下划线', trigger: 'blur' },
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号不能超过 32 位', trigger: 'blur' }],
}

async function submitRegistration(): Promise<void> {
  if (!formRef.value || isSubmitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  isSubmitting.value = true
  try {
    await authStore.register({
      username: form.username.trim(),
      password: form.password,
      email: form.email.trim() || undefined,
      phone: form.phone.trim() || undefined,
    })
    ElMessage.success('账户创建成功，请登录。')
    await router.push({ name: 'login', query: { username: form.username.trim() } })
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法完成注册，请稍后重试。'))
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <AuthShell eyebrow="CREATE YOUR ACCOUNT" title="创建账户" intro="先认识你，再一起照顾好它。">
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="submitRegistration"
    >
      <el-form-item label="用户名" prop="username">
        <el-input
          v-model="form.username"
          autocomplete="username"
          maxlength="32"
          placeholder="4–32 位字母、数字或下划线"
          size="large"
        />
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input
          v-model="form.password"
          autocomplete="new-password"
          maxlength="72"
          placeholder="至少 12 位"
          show-password
          size="large"
          type="password"
        />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input
          v-model="form.confirmPassword"
          autocomplete="new-password"
          maxlength="72"
          placeholder="再输入一次密码"
          show-password
          size="large"
          type="password"
        />
      </el-form-item>
      <el-form-item label="邮箱（选填）" prop="email">
        <el-input
          v-model="form.email"
          autocomplete="email"
          maxlength="254"
          placeholder="用于完善账户信息"
          size="large"
          type="email"
        />
      </el-form-item>
      <el-form-item label="手机号（选填）" prop="phone">
        <el-input
          v-model="form.phone"
          autocomplete="tel"
          maxlength="32"
          placeholder="请输入手机号"
          size="large"
          type="tel"
        />
      </el-form-item>

      <el-button class="submit-button" :loading="isSubmitting" native-type="submit" size="large" type="primary">
        创建我的账户
      </el-button>
    </el-form>

    <template #footer>
      已经有账户？<RouterLink to="/login">去登录</RouterLink>
    </template>
  </AuthShell>
</template>
