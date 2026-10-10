<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { createExpense, updateExpense } from '../api/expenses'
import type { ExpensePayload, ExpenseRecord } from '../api/expenses'
import type { PetProfile } from '../api/pets'
import { getApiErrorMessage } from '../api/errors'
import { expenseCategories } from '../domain/expenseMetrics'

const props = defineProps<{
  modelValue: boolean
  pets: PetProfile[]
  expense?: ExpenseRecord | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: []
}>()

const formRef = ref<FormInstance>()
const isSaving = ref(false)

function todayText(): string {
  const date = new Date()
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return offsetDate.toISOString().slice(0, 10)
}

function blankForm() {
  return {
    petId: 0,
    category: 'FOOD' as ExpensePayload['category'],
    amount: 0.01,
    expenseDate: todayText(),
    merchant: '',
    description: '',
    remark: '',
  }
}

const form = reactive(blankForm())
const rules: FormRules = {
  category: [{ required: true, message: '请选择消费分类', trigger: 'change' }],
  amount: [{ required: true, message: '请填写金额', trigger: 'blur' }],
  expenseDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

watch(() => [props.modelValue, props.expense] as const, ([visible]) => {
  if (!visible) return
  Object.assign(form, blankForm())
  if (props.expense) {
    Object.assign(form, {
      petId: props.expense.petId ?? 0,
      category: props.expense.category,
      amount: props.expense.amount,
      expenseDate: props.expense.expenseDate,
      merchant: props.expense.merchant ?? '',
      description: props.expense.description ?? '',
      remark: props.expense.remark ?? '',
    })
  }
}, { immediate: true })

async function submit(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (!Number.isFinite(form.amount) || form.amount <= 0 || form.amount > 999999.99) {
    ElMessage.warning('金额需大于 0，且不超过 ¥999,999.99。')
    return
  }
  try {
    isSaving.value = true
    const payload: ExpensePayload = {
      petId: form.petId > 0 ? form.petId : null,
      category: form.category,
      amount: Math.round(form.amount * 100) / 100,
      expenseDate: form.expenseDate,
      merchant: form.merchant.trim() || undefined,
      description: form.description.trim() || undefined,
      remark: form.remark.trim() || undefined,
    }
    if (props.expense) await updateExpense(props.expense.id, payload)
    else await createExpense(payload)
    ElMessage.success(props.expense ? '消费记录已更新。' : '记下这笔消费了。')
    emit('update:modelValue', false)
    emit('saved')
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法保存消费记录。'))
  } finally {
    isSaving.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="expense ? '编辑消费记录' : '记一笔消费'"
    width="min(560px, calc(100vw - 32px))"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
      <div class="expense-form-grid">
        <el-form-item label="记在谁名下" prop="petId" class="full-line">
          <el-select v-model="form.petId" placeholder="可以作为家庭公共支出" clearable>
            <el-option label="不指定宠物 · 公共支出" :value="0" />
            <el-option v-for="pet in pets" :key="pet.id" :label="pet.name" :value="pet.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="消费分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option v-for="category in expenseCategories" :key="category.value" :value="category.value">
              <span class="category-option"><span>{{ category.icon }}</span>{{ category.label }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :max="999999.99" :precision="2" :step="1" controls-position="right">
            <template #prefix>¥</template>
          </el-input-number>
        </el-form-item>
        <el-form-item label="消费日期" prop="expenseDate" class="full-line">
          <el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" :disabled-date="(date: Date) => date.getTime() > Date.now()" />
        </el-form-item>
        <el-form-item label="商家" prop="merchant">
          <el-input v-model="form.merchant" maxlength="160" placeholder="例如：附近宠物医院" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" maxlength="500" placeholder="例如：日常体检" />
        </el-form-item>
        <el-form-item label="备注" prop="remark" class="full-line">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="想补充些什么？（选填）" />
        </el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="isSaving" @click="submit">{{ expense ? '保存修改' : '记下这笔' }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.expense-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.full-line { grid-column: 1 / -1; }
.expense-form-grid :deep(.el-select), .expense-form-grid :deep(.el-input-number), .expense-form-grid :deep(.el-date-editor) { width: 100%; }
.category-option { display: inline-flex; align-items: center; gap: 9px; }
@media (max-width: 520px) { .expense-form-grid { grid-template-columns: 1fr; } .full-line { grid-column: auto; } }
</style>
