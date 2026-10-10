<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { getApiErrorMessage } from '../api/errors'
import { getExpenses, getMonthlyExpenseSummary } from '../api/expenses'
import type { ExpenseRecord, ExpenseSummary } from '../api/expenses'
import { categoryIcon, expenseCategoryLabel, formatCny } from '../domain/expenseMetrics'

const props = defineProps<{ petId: number }>()
const router = useRouter()
const records = ref<ExpenseRecord[]>([])
const summary = ref<ExpenseSummary | null>(null)
const isLoading = ref(true)

function currentMonthText(): string {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

async function load(): Promise<void> {
  isLoading.value = true
  try {
    const [page, monthly] = await Promise.all([
      getExpenses({ petId: props.petId, page: 1, size: 5 }),
      getMonthlyExpenseSummary(currentMonthText(), props.petId),
    ])
    records.value = page.records
    summary.value = monthly
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取这位家庭成员的消费记录。'))
  } finally {
    isLoading.value = false
  }
}

function viewAll(): void {
  void router.push({ name: 'expenses', query: { petId: String(props.petId) } })
}

watch(() => props.petId, () => void load())
onMounted(() => void load())
</script>

<template>
  <section class="pet-expense-card">
    <header class="panel-heading">
      <div><p class="eyebrow">A LITTLE CARE, RECORDED</p><h2>本月消费</h2></div>
      <button type="button" class="view-link" @click="viewAll">查看全部消费 <span>→</span></button>
    </header>
    <div v-if="isLoading" class="panel-loading">正在读取消费记录…</div>
    <template v-else>
      <div class="month-total"><strong>{{ formatCny(summary?.totalAmount ?? 0) }}</strong><span>本月 · {{ summary?.recordCount ?? 0 }} 笔</span></div>
      <div v-if="records.length" class="pet-expense-list">
        <article v-for="record in records" :key="record.id" class="pet-expense-row">
          <span class="record-icon" aria-hidden="true">{{ categoryIcon(record.category) }}</span>
          <div><strong>{{ record.description || expenseCategoryLabel(record.category) }}</strong><span>{{ record.expenseDate }} · {{ expenseCategoryLabel(record.category) }}</span></div>
          <b>{{ formatCny(record.amount) }}</b>
        </article>
      </div>
      <p v-else class="panel-empty">还没有为它记录消费，日常用品也可以在这里记下。</p>
    </template>
  </section>
</template>

<style scoped>
.pet-expense-card { padding: clamp(22px, 4vw, 34px); border: 1px solid #e5ebe2; border-radius: 25px; background: rgb(255 255 255 / 93%); box-shadow: 0 14px 40px rgb(47 67 54 / 5%); }
.panel-heading { display: flex; align-items: end; justify-content: space-between; gap: 15px; margin-bottom: 20px; }
.eyebrow { margin: 0 0 6px; color: #829682; font-size: 9px; font-weight: 700; letter-spacing: .16em; }
.panel-heading h2 { margin: 0; color: #34443a; font-size: 18px; font-weight: 600; }
.view-link { padding: 4px 0; border: 0; background: transparent; color: #62836a; cursor: pointer; font: inherit; font-size: 12px; font-weight: 600; white-space: nowrap; }
.view-link span { margin-left: 6px; }
.month-total { display: flex; align-items: baseline; gap: 12px; padding: 17px 19px; border-radius: 17px; background: linear-gradient(110deg, #eef4eb, #f6f1e7); }
.month-total strong { color: #3e5d45; font-size: 25px; font-weight: 600; letter-spacing: -.04em; }.month-total span { color: #7b887d; font-size: 11px; }
.pet-expense-list { display: grid; margin-top: 8px; }.pet-expense-row { display: flex; align-items: center; gap: 12px; padding: 13px 0; border-bottom: 1px solid #eff1ed; }.pet-expense-row:last-child { border-bottom: 0; }
.record-icon { display: grid; width: 39px; height: 39px; flex: 0 0 auto; place-items: center; border-radius: 14px; background: #f7efe7; color: #b7805e; }.pet-expense-row div { display: grid; min-width: 0; gap: 5px; }.pet-expense-row div strong { overflow: hidden; color: #46554a; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.pet-expense-row div span { color: #8b958d; font-size: 10px; }.pet-expense-row > b { margin-left: auto; color: #4d5d50; font-size: 12px; font-weight: 600; white-space: nowrap; }
.panel-empty,.panel-loading { padding: 25px 10px; color: #87938a; font-size: 12px; line-height: 1.8; text-align: center; }
@media (max-width: 560px) { .panel-heading { align-items: start; flex-direction: column; }.month-total { align-items: start; flex-direction: column; gap: 5px; } }
</style>
