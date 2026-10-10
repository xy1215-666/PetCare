<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { getApiErrorMessage } from '../api/errors'
import {
  deleteExpense,
  getExpenseCategoryStatistics,
  getExpenseMonthlyTrend,
  getExpensePetStatistics,
  getExpenses,
  getMonthlyExpenseSummary,
} from '../api/expenses'
import type {
  ExpenseCategory,
  ExpenseCategoryTotal,
  ExpenseMonthTotal,
  ExpensePage,
  ExpensePetTotal,
  ExpenseRecord,
  ExpenseSummary,
} from '../api/expenses'
import { getMyPets } from '../api/pets'
import type { PetProfile } from '../api/pets'
import ExpenseFormDialog from '../components/ExpenseFormDialog.vue'
import {
  categoryIcon,
  expenseCategories,
  expenseCategoryLabel,
  formatCny,
} from '../domain/expenseMetrics'

const ExpenseStatistics = defineAsyncComponent(() => import('../components/ExpenseStatistics.vue'))

const route = useRoute()
const router = useRouter()
const month = currentMonthText()
const pets = ref<PetProfile[]>([])
const expensePage = ref<ExpensePage>({ records: [], total: 0, page: 1, size: 20, totalPages: 0 })
const summary = ref<ExpenseSummary | null>(null)
const categories = ref<ExpenseCategoryTotal[]>([])
const trend = ref<ExpenseMonthTotal[]>([])
const petStatistics = ref<ExpensePetTotal[]>([])
const isLoading = ref(true)
const loadFailed = ref(false)
const isDialogOpen = ref(false)
const selectedExpense = ref<ExpenseRecord | null>(null)
const selectedPet = ref<number | undefined>(readPetQuery(route.query.petId))
const selectedCategory = ref<ExpenseCategory | ''>('')
const selectedDates = ref<[string, string] | null>(null)
const page = ref(1)
const activeTab = ref(['overview', 'records', 'statistics'].includes(String(route.query.tab))
  ? String(route.query.tab)
  : 'overview')

const period = computed(() => {
  if (selectedDates.value) return selectedDates.value
  const [year, monthNumber] = month.split('-').map(Number)
  const lastDay = new Date(year, monthNumber, 0).getDate()
  return [`${month}-01`, `${month}-${String(lastDay).padStart(2, '0')}`] as [string, string]
})
const showPetChart = computed(() => pets.value.length > 1)
const changeLabel = computed(() => {
  const rate = summary.value?.changeRate
  if (rate == null) return '上月暂无数据'
  if (rate === 0) return '与上月持平'
  return `${rate > 0 ? '较上月增加' : '较上月减少'} ${Math.abs(rate).toFixed(1)}%`
})
const changeClass = computed(() => {
  const rate = summary.value?.changeRate
  return rate == null || rate === 0 ? 'neutral' : rate > 0 ? 'up' : 'down'
})

function currentMonthText(): string {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

function readPetQuery(value: unknown): number | undefined {
  const parsed = Number(Array.isArray(value) ? value[0] : value)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined
}

function emptyExpensePage(): ExpensePage {
  return { records: [], total: 0, page: 1, size: 20, totalPages: 0 }
}

async function loadPage(): Promise<void> {
  isLoading.value = true
  loadFailed.value = false
  const filters = {
    ...(selectedPet.value === undefined ? {} : { petId: selectedPet.value }),
    ...(!selectedCategory.value ? {} : { category: selectedCategory.value }),
    ...(selectedDates.value ? { startDate: selectedDates.value[0], endDate: selectedDates.value[1] } : {}),
    page: page.value,
    size: 20,
  }
  const [pageResult, summaryResult, categoriesResult, trendResult, petResult] = await Promise.allSettled([
    getExpenses(filters),
    getMonthlyExpenseSummary(month, selectedPet.value),
    getExpenseCategoryStatistics(period.value[0], period.value[1], selectedPet.value),
    getExpenseMonthlyTrend(6, selectedPet.value),
    getExpensePetStatistics(),
  ])
  if (pageResult.status === 'fulfilled') expensePage.value = pageResult.value
  else {
    expensePage.value = emptyExpensePage()
    loadFailed.value = true
  }
  if (summaryResult.status === 'fulfilled') summary.value = summaryResult.value
  if (categoriesResult.status === 'fulfilled') categories.value = categoriesResult.value
  if (trendResult.status === 'fulfilled') trend.value = trendResult.value
  if (petResult.status === 'fulfilled') petStatistics.value = petResult.value
  if ([summaryResult, categoriesResult, trendResult, petResult].some((result) => result.status === 'rejected')) {
    loadFailed.value = true
  }
  if (loadFailed.value) ElMessage.error('有些消费数据暂时无法读取，请检查网络后重试。')
  isLoading.value = false
}

async function loadPets(): Promise<void> {
  try {
    pets.value = await getMyPets()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取宠物档案。'))
  }
}

function openCreate(): void {
  selectedExpense.value = null
  isDialogOpen.value = true
}

function openEdit(expense: ExpenseRecord): void {
  selectedExpense.value = expense
  isDialogOpen.value = true
}

async function removeExpense(expense: ExpenseRecord): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后，这笔支出将不再计入消费列表和统计。', '删除这笔消费？', {
      confirmButtonText: '删除记录',
      cancelButtonText: '再想想',
      type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    await deleteExpense(expense.id)
    ElMessage.success('消费记录已删除。')
    await loadPage()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(getApiErrorMessage(error, '暂时无法删除这笔消费。'))
  }
}

function clearFilters(): void {
  selectedPet.value = undefined
  selectedCategory.value = ''
  selectedDates.value = null
  if (route.query.petId || route.query.tab) void router.replace({ name: 'expenses' })
}

function setTab(value: string | number): void {
  activeTab.value = String(value)
  void router.replace({ name: 'expenses', query: { ...(selectedPet.value ? { petId: String(selectedPet.value) } : {}), tab: String(value) } })
}

function money(value: number | undefined): string {
  return formatCny(value ?? 0)
}

watch(() => route.query.petId, (value) => {
  selectedPet.value = readPetQuery(value)
})
watch(() => route.query.tab, (value) => {
  if (['overview', 'records', 'statistics'].includes(String(value))) activeTab.value = String(value)
})
watch(() => [selectedPet.value, selectedCategory.value, selectedDates.value] as const, () => {
  page.value = 1
  void loadPage()
}, { deep: true })
watch(page, () => void loadPage())

onMounted(() => {
  void loadPets()
  void loadPage()
})
</script>

<template>
  <div class="expenses-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">LITTLE THINGS, ALL ADD UP</p>
        <h1>消费记录</h1>
        <p>把日常花销轻轻记下，照顾它的每一份心意都看得见。</p>
      </div>
      <el-button class="add-expense-button" type="primary" size="large" @click="openCreate"><span aria-hidden="true">＋</span> 记一笔</el-button>
    </header>

    <section class="expense-summary" aria-label="本月养宠支出">
      <div class="summary-copy">
        <p class="eyebrow">{{ month }} · 家庭养宠支出</p>
        <h2>{{ money(summary?.totalAmount) }}</h2>
        <p class="summary-note"><span :class="`change-${changeClass}`">{{ changeLabel }}</span><span class="summary-dot">·</span>{{ summary?.recordCount ?? 0 }} 笔记录</p>
      </div>
      <div class="summary-decoration" aria-hidden="true"><span>♡</span><i>✦</i><b>·</b></div>
      <div class="summary-action">
        <span>每一笔小小的照料<br />都值得被好好记住。</span>
        <button type="button" @click="openCreate">开始记录 <span aria-hidden="true">→</span></button>
      </div>
    </section>

    <el-tabs :model-value="activeTab" class="expense-tabs" @update:model-value="setTab">
      <el-tab-pane label="概览" name="overview">
        <section class="overview-layout">
          <div class="content-card recent-card">
            <div class="card-title-row"><div><p class="eyebrow">YOUR RECENT NOTES</p><h2>最近的消费</h2></div><button type="button" class="text-link" @click="setTab('records')">查看全部 <span>→</span></button></div>
            <el-skeleton v-if="isLoading" :rows="4" animated />
            <div v-else-if="loadFailed && expensePage.records.length === 0" class="empty-state"><p>暂时无法读取记录。</p><button type="button" @click="loadPage">重新加载</button></div>
            <div v-else-if="expensePage.records.length === 0" class="empty-state"><span class="empty-mark">♡</span><h3>还没有消费记录</h3><p>记下第一笔日常开销，之后就能看到每月变化。</p><button type="button" @click="openCreate">记录第一笔消费</button></div>
            <div v-else class="expense-list">
              <article v-for="expense in expensePage.records.slice(0, 5)" :key="expense.id" class="expense-row">
                <span class="category-mark" :class="`category-mark--${expense.category.toLowerCase()}`">{{ categoryIcon(expense.category) }}</span>
                <div class="expense-copy"><strong>{{ expense.description || expenseCategoryLabel(expense.category) }}</strong><span>{{ expense.expenseDate }}<template v-if="expense.petName"> · {{ expense.petName }}</template><template v-else> · 公共支出</template></span></div>
                <div class="expense-amount"><strong>{{ money(expense.amount) }}</strong><span>{{ expenseCategoryLabel(expense.category) }}</span></div>
              </article>
            </div>
          </div>
          <aside class="content-card gentle-note"><span class="note-icon" aria-hidden="true">✧</span><p class="eyebrow">A LITTLE REMINDER</p><h2>健康档案里的费用<br />不会自动记入消费</h2><p>就医档案用于记录诊疗信息。若想纳入支出统计，请在这里单独记一笔，避免重复计算。</p><button type="button" @click="setTab('statistics')">看看支出统计 <span>→</span></button></aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="记录" name="records">
        <section class="content-card records-card">
          <div class="records-heading"><div><p class="eyebrow">A SIMPLE RECORD, AT YOUR PACE</p><h2>所有消费</h2></div><span>共 {{ expensePage.total }} 笔</span></div>
          <div class="filter-bar">
            <el-select v-model="selectedPet" clearable placeholder="所有宠物" class="filter-pet"><el-option v-for="pet in pets" :key="pet.id" :label="pet.name" :value="pet.id" /></el-select>
            <el-select v-model="selectedCategory" clearable placeholder="所有分类" class="filter-category"><el-option v-for="category in expenseCategories" :key="category.value" :label="category.label" :value="category.value" /></el-select>
            <el-date-picker v-model="selectedDates" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" class="filter-dates" />
            <button v-if="selectedPet || selectedCategory || selectedDates" type="button" class="clear-filters" @click="clearFilters">清除筛选</button>
          </div>
          <el-skeleton v-if="isLoading" :rows="5" animated />
          <div v-else-if="loadFailed && expensePage.records.length === 0" class="empty-state"><p>暂时无法读取记录。</p><button type="button" @click="loadPage">重新加载</button></div>
          <div v-else-if="expensePage.records.length === 0" class="empty-state"><span class="empty-mark">♡</span><h3>没有找到消费记录</h3><p>试试调整筛选条件，或记下今天的第一笔开销。</p><button type="button" @click="openCreate">记一笔</button></div>
          <div v-else class="expense-list">
            <article v-for="expense in expensePage.records" :key="expense.id" class="expense-row expense-row--actions">
              <span class="category-mark" :class="`category-mark--${expense.category.toLowerCase()}`">{{ categoryIcon(expense.category) }}</span>
              <div class="expense-copy"><strong>{{ expense.description || expenseCategoryLabel(expense.category) }}</strong><span>{{ expense.expenseDate }} · {{ expense.petName || '公共支出' }}<template v-if="expense.merchant"> · {{ expense.merchant }}</template></span></div>
              <div class="expense-amount"><strong>{{ money(expense.amount) }}</strong><span>{{ expenseCategoryLabel(expense.category) }}</span></div>
              <div class="row-actions"><button type="button" :aria-label="`编辑${expense.description || '消费记录'}`" @click="openEdit(expense)">编辑</button><button type="button" class="delete-action" :aria-label="`删除${expense.description || '消费记录'}`" @click="removeExpense(expense)">删除</button></div>
            </article>
          </div>
          <el-pagination v-if="expensePage.totalPages > 1" v-model:current-page="page" class="expense-pagination" layout="prev, pager, next" :total="expensePage.total" :page-size="expensePage.size" />
        </section>
      </el-tab-pane>

      <el-tab-pane label="统计" name="statistics" lazy>
        <section class="statistics-intro"><div><p class="eyebrow">A CLEARER VIEW OF CARE</p><h2>花销慢慢有了轮廓</h2></div><span>{{ period[0] }} 至 {{ period[1] }}</span></section>
        <div class="statistics-filters">
          <el-select v-model="selectedPet" clearable placeholder="全部宠物" class="filter-pet"><el-option v-for="pet in pets" :key="pet.id" :label="pet.name" :value="pet.id" /></el-select>
          <el-date-picker v-model="selectedDates" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" class="filter-dates" />
          <button v-if="selectedPet || selectedDates" type="button" class="clear-filters" @click="clearFilters">清除统计筛选</button>
        </div>
        <ExpenseStatistics :categories="categories" :trend="trend" :pets="petStatistics" :show-pet-chart="showPetChart" />
      </el-tab-pane>
    </el-tabs>

    <ExpenseFormDialog v-model="isDialogOpen" :pets="pets" :expense="selectedExpense" @saved="loadPage" />
  </div>
</template>

<style scoped>
.expenses-page { display: grid; gap: 27px; }
.page-heading { display: flex; align-items: end; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0 0 9px; color: #819581; font-size: 9px; font-weight: 700; letter-spacing: .18em; }
.page-heading h1 { margin: 0; color: #2d3e33; font-size: clamp(28px, 4vw, 36px); font-weight: 600; letter-spacing: -.045em; }
.page-heading p:last-child { margin: 10px 0 0; color: #819087; font-size: 13px; line-height: 1.8; }
.add-expense-button { min-height: 44px; border: 0; border-radius: 13px; background: #56866c; box-shadow: 0 8px 18px rgb(79 127 95 / 15%); font-size: 13px; font-weight: 600; }
.add-expense-button span { margin-right: 5px; font-size: 19px; font-weight: 400; }
.expense-summary { position: relative; display: flex; min-height: 205px; align-items: center; overflow: hidden; padding: 30px clamp(24px, 5vw, 52px); border: 1px solid #dfeadf; border-radius: 27px; background: radial-gradient(circle at 59% 42%, rgb(255 255 255 / 75%), transparent 22%), linear-gradient(113deg, #e8f1e6 0%, #eef3e9 54%, #f4eddf 100%); }
.summary-copy { position: relative; z-index: 1; }
.summary-copy .eyebrow { margin-bottom: 12px; color: #698771; letter-spacing: .1em; }
.summary-copy h2 { margin: 0; color: #2d4635; font-size: clamp(34px, 5vw, 48px); font-weight: 600; letter-spacing: -.055em; }
.summary-note { display: flex; align-items: center; gap: 10px; margin: 10px 0 0; color: #76867a; font-size: 12px; }
.summary-dot { color: #c1c9bf; }
.change-up { color: #b77859; }.change-down { color: #578365; }.change-neutral { color: #839087; }
.summary-decoration { position: absolute; top: 50%; left: 55%; width: 130px; height: 130px; transform: translate(-50%, -50%); border: 1px solid rgb(111 147 113 / 20%); border-radius: 50%; color: #80a083; }
.summary-decoration::before,.summary-decoration::after { position: absolute; inset: 14px; border: 1px solid rgb(111 147 113 / 17%); border-radius: 50%; content: ''; }
.summary-decoration::after { inset: 31px; }
.summary-decoration span { position: absolute; top: 33px; left: 42px; z-index: 1; color: #d0926d; font-size: 42px; }
.summary-decoration i { position: absolute; top: 3px; right: 14px; color: #c68b66; font-size: 18px; font-style: normal; }
.summary-decoration b { position: absolute; bottom: 8px; left: 16px; font-size: 22px; }
.summary-action { display: grid; gap: 16px; margin-left: auto; color: #718273; font-size: 12px; line-height: 1.8; }
.summary-action button,.gentle-note button { width: fit-content; padding: 9px 13px; border: 1px solid rgb(88 128 95 / 18%); border-radius: 11px; background: rgb(255 255 255 / 66%); color: #557a60; cursor: pointer; font: inherit; font-size: 12px; font-weight: 600; }
.summary-action button span,.gentle-note button span,.text-link span { margin-left: 7px; }
.expense-tabs :deep(.el-tabs__header) { margin-bottom: 20px; }
.expense-tabs :deep(.el-tabs__item) { height: 44px; color: #77847a; font-size: 13px; }
.expense-tabs :deep(.el-tabs__item.is-active) { color: #55856d; font-weight: 600; }
.expense-tabs :deep(.el-tabs__active-bar) { background: #55856d; }
.expense-tabs :deep(.el-tabs__nav-wrap::after) { background: #e4ebe2; }
.overview-layout { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(240px, .8fr); gap: 17px; }
.content-card { min-width: 0; padding: 25px; border: 1px solid #e7ebe4; border-radius: 23px; background: rgb(255 255 255 / 92%); box-shadow: 0 10px 30px rgb(47 67 54 / 3%); }
.card-title-row,.records-heading { display: flex; align-items: end; justify-content: space-between; gap: 15px; margin-bottom: 18px; }
.card-title-row .eyebrow,.records-heading .eyebrow { margin-bottom: 6px; }
.card-title-row h2,.records-heading h2,.statistics-intro h2 { margin: 0; color: #34443a; font-size: 18px; font-weight: 600; }
.text-link { padding: 4px 0; border: 0; background: none; color: #61836a; cursor: pointer; font: inherit; font-size: 12px; font-weight: 600; }
.expense-list { display: grid; }
.expense-row { display: flex; min-width: 0; align-items: center; gap: 13px; padding: 14px 0; border-top: 1px solid #eff1ed; }
.expense-row:first-child { border-top: 0; }
.category-mark { display: grid; width: 43px; height: 43px; flex: 0 0 auto; place-items: center; border-radius: 15px; background: #f7efe7; color: #bd8965; font-size: 19px; }
.category-mark--medical { background: #f8ece9; color: #c87872; }.category-mark--vaccine { background: #eef0f8; color: #7888b9; }.category-mark--deworming { background: #edf3e9; color: #688b6b; }.category-mark--cleaning { background: #eaf3f3; color: #639296; }.category-mark--toy { background: #f4edf4; color: #a77ca2; }.category-mark--daily_supplies { background: #f5f1e6; color: #ad9458; }
.expense-copy { display: grid; min-width: 0; gap: 6px; }
.expense-copy strong { overflow: hidden; color: #3c4b40; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.expense-copy span,.expense-amount span { overflow: hidden; color: #89938b; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }
.expense-amount { display: grid; flex: 0 0 auto; gap: 5px; margin-left: auto; text-align: right; }
.expense-amount strong { color: #46584a; font-size: 14px; font-weight: 600; }
.gentle-note { display: flex; flex-direction: column; align-items: start; justify-content: center; background: linear-gradient(145deg, #fffefa, #f7f2e9); }
.note-icon { display: grid; width: 43px; height: 43px; margin-bottom: 20px; place-items: center; border-radius: 15px; background: #f7eae0; color: #c48763; font-size: 22px; }
.gentle-note h2 { margin: 0; color: #3c4b40; font-size: 17px; font-weight: 600; line-height: 1.7; }
.gentle-note > p:not(.eyebrow) { margin: 10px 0 18px; color: #829087; font-size: 12px; line-height: 1.9; }
.empty-state { display: grid; min-height: 200px; align-content: center; justify-items: center; text-align: center; }
.empty-state h3 { margin: 3px 0 0; color: #47564b; font-size: 14px; }.empty-state p { margin: 8px 0 14px; color: #8b958d; font-size: 12px; }
.empty-state button { padding: 9px 13px; border: 0; border-radius: 10px; background: #edf3ea; color: #568064; cursor: pointer; font: inherit; font-size: 12px; font-weight: 600; }
.empty-mark { display: grid; width: 48px; height: 48px; place-items: center; border-radius: 17px; background: #f7eee5; color: #bf8965; font-size: 24px; }
.records-card { padding: clamp(19px, 3vw, 27px); }
.records-heading { margin-bottom: 19px; }.records-heading > span { color: #89938b; font-size: 12px; }
.filter-bar { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 13px; padding: 13px; border-radius: 15px; background: #f8f9f6; }
.filter-pet { width: 150px; }.filter-category { width: 145px; }.filter-dates { width: 280px; }.clear-filters { border: 0; background: transparent; color: #66836b; cursor: pointer; font: inherit; font-size: 12px; }
.row-actions { display: flex; flex: 0 0 auto; gap: 7px; margin-left: 5px; }.row-actions button { padding: 6px 8px; border: 1px solid #e6ebe4; border-radius: 8px; background: #fff; color: #698170; cursor: pointer; font: inherit; font-size: 11px; }.row-actions .delete-action { color: #a97769; }
.expense-pagination { justify-content: center; margin-top: 17px; }
.statistics-intro { display: flex; align-items: end; justify-content: space-between; gap: 12px; margin: 4px 0 18px; }.statistics-intro .eyebrow { margin-bottom: 7px; }.statistics-intro > span { color: #89938b; font-size: 11px; }
.statistics-filters { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; margin-bottom: 17px; }.statistics-filters .filter-pet { width: 180px; }.statistics-filters .filter-dates { width: 280px; }
@media (max-width: 760px) { .overview-layout { grid-template-columns: 1fr; }.summary-decoration { left: 70%; opacity: .55; }.summary-action { display: none; }.content-card { padding: 19px; } }
@media (max-width: 560px) { .page-heading { align-items: start; }.page-heading > div > p:last-child { max-width: 250px; }.add-expense-button { min-height: 40px; padding: 0 12px; font-size: 12px; }.expense-summary { min-height: 180px; padding: 24px 21px; }.summary-decoration { left: 84%; transform: translate(-50%, -50%) scale(.78); }.summary-copy h2 { font-size: 36px; }.expense-row { gap: 9px; }.category-mark { width: 38px; height: 38px; border-radius: 13px; }.expense-copy strong { max-width: 130px; }.row-actions { flex-direction: column; gap: 4px; }.filter-pet,.filter-category,.filter-dates,.statistics-filters .filter-pet,.statistics-filters .filter-dates { width: 100%; }.statistics-intro { align-items: start; flex-direction: column; }.statistics-intro > span { font-size: 10px; } }
</style>
