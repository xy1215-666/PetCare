<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getApiErrorMessage } from '../api/errors'
import { getMyPets } from '../api/pets'
import type { PetProfile } from '../api/pets'
import { getUpcomingReminders } from '../api/reminders'
import type { UpcomingReminder } from '../api/reminders'
import { getExpenses, getMonthlyExpenseSummary } from '../api/expenses'
import type { ExpenseRecord, ExpenseSummary } from '../api/expenses'
import { expenseCategoryLabel, formatCny } from '../domain/expenseMetrics'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const username = computed(() => authStore.currentUser?.username ?? '你')
const pets = ref<PetProfile[]>([])
const isLoadingPets = ref(true)
const reminders = ref<UpcomingReminder[]>([])
const isLoadingReminders = ref(true)
const reminderLoadFailed = ref(false)
const expenseSummary = ref<ExpenseSummary | null>(null)
const recentExpenses = ref<ExpenseRecord[]>([])
const expenseLoadFailed = ref(false)

function currentMonthText(): string {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

const reminderTabs = {
  VACCINATION: 'vaccinations',
  DEWORMING: 'dewormings',
  MEDICAL_REVISIT: 'medical',
} as const

function dueLabel(daysRemaining: number): string {
  if (daysRemaining === 0) return '今天'
  if (daysRemaining === 1) return '明天'
  return `${daysRemaining} 天后`
}

onMounted(() => {
  void loadPets()
  void loadReminders()
  void loadExpenseOverview()
})

async function loadPets(): Promise<void> {
  try {
    pets.value = await getMyPets()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取宠物档案。'))
  } finally {
    isLoadingPets.value = false
  }
}

async function loadReminders(): Promise<void> {
  isLoadingReminders.value = true
  reminderLoadFailed.value = false
  try {
    reminders.value = await getUpcomingReminders()
  } catch (error) {
    reminderLoadFailed.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取照护提醒。'))
  } finally {
    isLoadingReminders.value = false
  }
}

async function loadExpenseOverview(): Promise<void> {
  expenseLoadFailed.value = false
  try {
    const [monthly, page] = await Promise.all([
      getMonthlyExpenseSummary(currentMonthText()),
      getExpenses({ page: 1, size: 4 }),
    ])
    expenseSummary.value = monthly
    recentExpenses.value = page.records
  } catch (error) {
    expenseLoadFailed.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取本月消费。'))
  }
}

const expenseChangeText = computed(() => {
  const rate = expenseSummary.value?.changeRate
  if (rate == null) return '上月暂无数据'
  if (rate === 0) return '与上月持平'
  return `较上月${rate > 0 ? '增加' : '减少'} ${Math.abs(rate).toFixed(1)}%`
})
</script>

<template>
  <div class="dashboard-page">
    <header class="welcome-line">
      <p class="eyebrow">PETCARE · YOUR LITTLE CORNER</p>
      <h1>你好，{{ username }}<span class="wave">，欢迎回家。</span></h1>
      <p>把照护日常慢慢记下来，让每一份陪伴都有迹可循。</p>
    </header>

    <section class="hero-card" aria-labelledby="hero-title">
      <div class="hero-copy">
        <span class="hero-tag">一份档案，一份安心</span>
        <h2 id="hero-title">从认识它开始，<br />把关心放进日常。</h2>
        <p>为家里的小伙伴建立专属档案，生日、体重和生活点滴都能有处安放。</p>
        <RouterLink class="primary-link" to="/pets">看看我的宠物 <span aria-hidden="true">→</span></RouterLink>
      </div>
      <div class="hero-art" aria-hidden="true">
        <div class="sun-orbit" />
        <div class="leaf leaf-one" />
        <div class="leaf leaf-two" />
        <div class="paw-card">
          <svg viewBox="0 0 100 100" role="presentation">
            <ellipse cx="50" cy="61" rx="24" ry="20" />
            <ellipse cx="25" cy="35" rx="10" ry="14" transform="rotate(-22 25 35)" />
            <ellipse cx="45" cy="25" rx="10" ry="14" transform="rotate(-7 45 25)" />
            <ellipse cx="66" cy="28" rx="10" ry="14" transform="rotate(12 66 28)" />
            <ellipse cx="82" cy="43" rx="9" ry="13" transform="rotate(28 82 43)" />
          </svg>
        </div>
        <span class="sparkle sparkle-one">✦</span>
        <span class="sparkle sparkle-two">✧</span>
      </div>
    </section>

    <section class="dashboard-expense" aria-labelledby="expense-title">
      <div class="dashboard-expense-summary">
        <p class="eyebrow">A LITTLE CARE, COUNTED</p>
        <h2 id="expense-title">本月养宠支出</h2>
        <strong v-if="expenseSummary">{{ formatCny(expenseSummary.totalAmount) }}</strong>
        <span v-else-if="expenseLoadFailed" class="expense-loading">暂时无法读取</span>
        <span v-else class="expense-loading">正在整理…</span>
        <p v-if="expenseSummary" class="expense-change">{{ expenseChangeText }} <span>·</span> {{ expenseSummary.recordCount }} 笔记录</p>
        <RouterLink class="expense-all-link" to="/expenses">查看消费记录 <span aria-hidden="true">→</span></RouterLink>
      </div>
      <div class="dashboard-recent-expenses">
        <div class="expense-list-heading"><div><p class="eyebrow">RECENT NOTES</p><h3>最近消费</h3></div><RouterLink to="/expenses">全部</RouterLink></div>
        <div v-if="recentExpenses.length" class="dashboard-expense-list">
          <RouterLink v-for="expense in recentExpenses.slice(0, 3)" :key="expense.id" class="dashboard-expense-item" to="/expenses">
            <span class="expense-dot" aria-hidden="true">·</span>
            <span class="dashboard-expense-copy"><strong>{{ expense.description || expenseCategoryLabel(expense.category) }}</strong><small>{{ expense.expenseDate }} · {{ expense.petName || '公共支出' }}</small></span>
            <b>{{ formatCny(expense.amount) }}</b>
          </RouterLink>
        </div>
        <div v-else class="dashboard-expense-empty">
          <p>{{ expenseLoadFailed ? '消费数据暂时不可用。' : '最近还没有消费记录。' }}</p>
          <RouterLink to="/expenses">记下第一笔 <span aria-hidden="true">→</span></RouterLink>
        </div>
      </div>
    </section>

    <section class="reminder-section" aria-labelledby="reminder-title">
      <div class="section-heading">
        <div>
          <p class="eyebrow">A LITTLE NUDGE FOR THEIR CARE</p>
          <h2 id="reminder-title">待办提醒</h2>
        </div>
        <button v-if="reminderLoadFailed" class="reminder-retry" type="button" @click="loadReminders">重新加载</button>
        <p v-else-if="!isLoadingReminders">未来 30 天内需要留心的事</p>
      </div>
      <div v-if="isLoadingReminders" class="reminder-state">正在整理近期照护安排…</div>
      <div v-else-if="reminderLoadFailed" class="reminder-state">暂时无法读取提醒，请稍后再试。</div>
      <div v-else-if="reminders.length === 0" class="reminder-state">近期没有需要处理的健康事项，愿每一天都轻松安心。</div>
      <div v-else class="reminder-list">
        <RouterLink
          v-for="reminder in reminders.slice(0, 5)"
          :key="`${reminder.type}-${reminder.petId}-${reminder.dueDate}-${reminder.title}`"
          class="reminder-item"
          :to="{ name: 'pet-detail', params: { petId: reminder.petId }, query: { tab: reminderTabs[reminder.type] } }"
        >
          <span class="reminder-icon" aria-hidden="true">{{ reminder.type === 'VACCINATION' ? '✦' : reminder.type === 'DEWORMING' ? '♡' : '+' }}</span>
          <span class="reminder-copy">
            <strong>{{ reminder.petName }} · {{ reminder.title }}</strong>
            <span>{{ reminder.dueDate }}</span>
          </span>
          <span class="reminder-due">{{ dueLabel(reminder.daysRemaining) }}</span>
          <span class="preview-arrow" aria-hidden="true">→</span>
        </RouterLink>
      </div>
    </section>

    <section class="start-section" aria-labelledby="start-title">
      <div class="section-heading">
        <div>
          <p class="eyebrow">A GENTLE PLACE TO BEGIN</p>
          <h2 id="start-title">从一个小小的开始</h2>
        </div>
        <p v-if="isLoadingPets">正在读取你的宠物档案…</p>
        <p v-else>已经记录 {{ pets.length }} 位家庭成员</p>
      </div>
      <div v-if="pets.length === 0 && !isLoadingPets" class="start-card">
        <span class="start-icon" aria-hidden="true">♡</span>
        <div>
          <h3>建立宠物档案</h3>
          <p>记录名字、品种和生日，打造专属的家庭小档案。</p>
        </div>
        <RouterLink class="quiet-link" to="/pets/new">创建第一份档案 <span aria-hidden="true">→</span></RouterLink>
      </div>
      <div v-else-if="!isLoadingPets" class="pet-preview-grid">
        <RouterLink v-for="pet in pets.slice(0, 3)" :key="pet.id" class="pet-preview" :to="`/pets/${pet.id}`">
          <span class="preview-mark" aria-hidden="true">{{ pet.name.slice(0, 1) }}</span>
          <span class="preview-name">{{ pet.name }}</span>
          <span class="preview-type">{{ pet.type === 'CAT' ? '猫咪' : pet.type === 'DOG' ? '狗狗' : '小伙伴' }}</span>
          <span class="preview-arrow" aria-hidden="true">→</span>
        </RouterLink>
        <RouterLink v-if="pets.length > 3" class="more-pets" to="/pets">查看全部宠物 →</RouterLink>
      </div>
      <div v-else class="start-card start-card--loading" aria-hidden="true">
        <span class="start-icon">♡</span>
        <div class="loading-copy" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 42px;
}

.welcome-line .eyebrow,
.section-heading .eyebrow {
  margin: 0 0 10px;
  color: #76917a;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.19em;
}

.welcome-line h1 {
  margin: 0;
  color: #29392f;
  font-size: clamp(26px, 3.4vw, 38px);
  font-weight: 600;
  letter-spacing: -0.045em;
}

.wave {
  color: #698572;
}

.welcome-line > p:last-child {
  margin: 12px 0 0;
  color: #78857c;
  font-size: 14px;
  line-height: 1.8;
}

.hero-card {
  position: relative;
  display: grid;
  min-height: 330px;
  overflow: hidden;
  grid-template-columns: 1.2fr 0.8fr;
  border: 1px solid rgb(92 127 99 / 11%);
  border-radius: 30px;
  background:
    radial-gradient(ellipse at 85% 10%, rgb(255 255 255 / 80%), transparent 34%),
    linear-gradient(118deg, #e8f0e5 0%, #eff3e9 58%, #f2eadc 100%);
}

.dashboard-expense {
  display: grid;
  grid-template-columns: minmax(230px, 0.82fr) minmax(0, 1.18fr);
  overflow: hidden;
  border: 1px solid #e5ebe2;
  border-radius: 24px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 10px 32px rgb(47 67 54 / 4%);
}

.dashboard-expense-summary {
  display: flex;
  flex-direction: column;
  align-items: start;
  padding: 25px 28px;
  background: linear-gradient(135deg, #edf4e9, #f7f2e8);
}

.dashboard-expense-summary .eyebrow,
.expense-list-heading .eyebrow {
  margin: 0 0 8px;
  color: #819581;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: .16em;
}

.dashboard-expense-summary h2,
.expense-list-heading h3 {
  margin: 0;
  color: #3b4c40;
  font-size: 15px;
  font-weight: 600;
}

.dashboard-expense-summary > strong {
  margin-top: 16px;
  color: #3e5d45;
  font-size: 30px;
  font-weight: 600;
  letter-spacing: -.045em;
}

.expense-loading {
  margin-top: 18px;
  color: #86948a;
  font-size: 13px;
}

.expense-change {
  margin: 6px 0 0;
  color: #7f8c82;
  font-size: 11px;
}

.expense-change span { padding: 0 5px; color: #c0c9be; }

.expense-all-link {
  margin-top: 17px;
  color: #5b8063;
  font-size: 11px;
  font-weight: 600;
}

.expense-all-link span { margin-left: 6px; }

.dashboard-recent-expenses { min-width: 0; padding: 23px 27px; }
.expense-list-heading { display: flex; align-items: end; justify-content: space-between; gap: 12px; margin-bottom: 11px; }
.expense-list-heading > a { color: #68866e; font-size: 11px; font-weight: 600; }
.dashboard-expense-list { display: grid; }
.dashboard-expense-item { display: flex; min-width: 0; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid #eff1ed; }
.expense-dot { display: grid; width: 31px; height: 31px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: #f6eee5; color: #bf8964; font-size: 21px; }
.dashboard-expense-copy { display: grid; min-width: 0; gap: 4px; }
.dashboard-expense-copy strong { overflow: hidden; color: #455449; font-size: 11px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.dashboard-expense-copy small { color: #8a958c; font-size: 10px; }
.dashboard-expense-item > b { margin-left: auto; color: #536456; font-size: 11px; font-weight: 600; white-space: nowrap; }
.dashboard-expense-empty { display: flex; min-height: 111px; align-items: center; justify-content: space-between; gap: 10px; border-top: 1px solid #eff1ed; }
.dashboard-expense-empty p { color: #89958b; font-size: 11px; }
.dashboard-expense-empty a { color: #61836a; font-size: 11px; font-weight: 600; white-space: nowrap; }

.hero-copy {
  z-index: 1;
  align-self: center;
  padding: clamp(28px, 5vw, 58px);
}

.hero-tag {
  display: inline-flex;
  padding: 7px 12px;
  border: 1px solid rgb(85 133 109 / 15%);
  border-radius: 999px;
  background: rgb(255 255 255 / 60%);
  color: #527660;
  font-size: 11px;
  font-weight: 600;
}

.hero-copy h2 {
  margin: 20px 0 12px;
  color: #2b3b31;
  font-size: clamp(28px, 4vw, 42px);
  font-weight: 600;
  letter-spacing: -0.055em;
  line-height: 1.28;
}

.hero-copy p {
  max-width: 410px;
  margin: 0;
  color: #68776c;
  font-size: 14px;
  line-height: 1.9;
}

.primary-link {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
  padding: 12px 17px;
  border-radius: 13px;
  background: #55856d;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  transition: background 150ms ease, transform 150ms ease;
}

.primary-link:hover {
  transform: translateY(-1px);
  background: #466f5a;
}

.hero-art {
  position: relative;
  min-height: 290px;
}

.sun-orbit {
  position: absolute;
  top: 50%;
  left: 48%;
  width: min(25vw, 240px);
  aspect-ratio: 1;
  transform: translate(-50%, -50%);
  border: 1px solid rgb(111 147 113 / 22%);
  border-radius: 50%;
  background: rgb(255 255 255 / 28%);
}

.sun-orbit::before,
.sun-orbit::after {
  position: absolute;
  border: 1px solid rgb(111 147 113 / 17%);
  border-radius: 50%;
  content: '';
}

.sun-orbit::before {
  inset: 16px;
}

.sun-orbit::after {
  inset: 34px;
}

.paw-card {
  position: absolute;
  top: 50%;
  left: 48%;
  display: grid;
  width: 134px;
  aspect-ratio: 1;
  transform: translate(-50%, -50%) rotate(-7deg);
  place-items: center;
  border: 1px solid rgb(255 255 255 / 80%);
  border-radius: 35px;
  background: linear-gradient(145deg, #fffefa, #f3e8d7);
  box-shadow: 0 20px 50px rgb(82 107 84 / 15%);
}

.paw-card svg {
  width: 76px;
  fill: #d18b66;
}

.leaf {
  position: absolute;
  width: 34px;
  height: 68px;
  border-radius: 100% 0 100% 0;
  background: linear-gradient(145deg, #a9c19e, #66896c);
  opacity: 0.7;
}

.leaf-one {
  top: 21%;
  left: 20%;
  transform: rotate(-30deg);
}

.leaf-two {
  right: 16%;
  bottom: 19%;
  width: 24px;
  height: 48px;
  transform: rotate(35deg);
}

.sparkle {
  position: absolute;
  color: #c68a64;
}

.sparkle-one {
  top: 24%;
  right: 21%;
  font-size: 23px;
}

.sparkle-two {
  bottom: 23%;
  left: 24%;
  font-size: 28px;
}

.start-section {
  display: grid;
  gap: 18px;
}

.reminder-section {
  display: grid;
  gap: 18px;
}

.reminder-list {
  display: grid;
  gap: 10px;
}

.reminder-item {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
  padding: 15px 18px;
  border: 1px solid #e7ebe4;
  border-radius: 18px;
  background: rgb(255 255 255 / 90%);
  transition: transform 150ms ease, box-shadow 150ms ease;
}

.reminder-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 9px 24px rgb(47 67 54 / 7%);
}

.reminder-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 15px;
  background: #edf3ea;
  color: #62826a;
  font-size: 19px;
}

.reminder-copy {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.reminder-copy strong {
  overflow: hidden;
  color: #3b4a40;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reminder-copy span,
.reminder-state {
  color: #849087;
  font-size: 12px;
}

.reminder-due {
  flex: 0 0 auto;
  margin-left: auto;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8efe6;
  color: #a87554;
  font-size: 11px;
  font-weight: 600;
}

.reminder-state {
  padding: 24px;
  border: 1px solid #e7ebe4;
  border-radius: 18px;
  background: rgb(255 255 255 / 88%);
  line-height: 1.8;
  text-align: center;
}

.reminder-retry {
  padding: 7px 12px;
  border: 1px solid #dce7dc;
  border-radius: 10px;
  background: #fff;
  color: #567c61;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 18px;
}

.section-heading h2 {
  margin: 0;
  color: #29392f;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: -0.035em;
}

.section-heading > p {
  margin: 0 0 3px;
  color: #849087;
  font-size: 12px;
}

.start-card {
  display: flex;
  min-height: 106px;
  align-items: center;
  gap: 17px;
  padding: 20px 24px;
  border: 1px solid #e7ebe4;
  border-radius: 22px;
  background: rgb(255 255 255 / 88%);
  box-shadow: 0 10px 32px rgb(47 67 54 / 4%);
}

.start-card--loading {
  pointer-events: none;
}

.loading-copy {
  width: min(250px, 65vw);
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(90deg, #eef1eb, #f7f8f5, #eef1eb);
  background-size: 200% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

.pet-preview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.pet-preview {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 11px;
  padding: 16px;
  border: 1px solid #e7ebe4;
  border-radius: 18px;
  background: rgb(255 255 255 / 88%);
}

.preview-mark {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 14px;
  background: #edf3ea;
  color: #5c8567;
  font-weight: 700;
}

.preview-name {
  overflow: hidden;
  color: #3b4a40;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-type {
  margin-left: auto;
  color: #829087;
  font-size: 11px;
  white-space: nowrap;
}

.preview-arrow,
.more-pets {
  color: #62826a;
  font-size: 12px;
}

.more-pets {
  grid-column: 1 / -1;
  justify-self: end;
  font-weight: 600;
}

.start-icon {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 17px;
  background: #f7eee4;
  color: #bd805e;
  font-size: 24px;
}

.start-card h3 {
  margin: 0;
  color: #39483e;
  font-size: 14px;
  font-weight: 600;
}

.start-card p {
  margin: 6px 0 0;
  color: #849087;
  font-size: 12px;
  line-height: 1.7;
}

.quiet-link {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  margin-left: auto;
  color: #537960;
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 720px) {
  .dashboard-page {
    gap: 32px;
  }

  .hero-card {
    grid-template-columns: 1fr;
  }

  .dashboard-expense { grid-template-columns: 1fr; }

  .hero-copy {
    padding: 32px 28px 0;
  }

  .hero-art {
    min-height: 210px;
  }

  .sun-orbit {
    width: 180px;
  }

  .paw-card {
    width: 110px;
    border-radius: 29px;
  }

  .paw-card svg {
    width: 64px;
  }

  .section-heading {
    align-items: start;
    flex-direction: column;
    gap: 6px;
  }

  .start-card {
    flex-wrap: wrap;
    padding: 18px;
  }

  .pet-preview-grid {
    grid-template-columns: 1fr;
  }

  .reminder-item {
    gap: 10px;
    padding: 13px;
  }

  .reminder-due {
    padding: 5px 7px;
    font-size: 10px;
  }

  .quiet-link {
    width: 100%;
    margin: 0 0 0 65px;
  }
}
</style>
