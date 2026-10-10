<script setup lang="ts">
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createWeightRecord,
  deleteWeightRecord,
  getWeightRecords,
  updateWeightRecord,
} from '../api/weightRecords'
import type { WeightRecord, WeightRecordPayload } from '../api/weightRecords'
import { getApiErrorMessage } from '../api/errors'
import { formatWeightChange, latestWeightChange, weightChartPoints } from '../domain/weightMetrics'

const WeightTrendChart = defineAsyncComponent(() => import('./WeightTrendChart.vue'))

const props = defineProps<{ petId: number }>()
const emit = defineEmits<{
  (event: 'current-weight-changed', weight: number | null): void
}>()

const records = ref<WeightRecord[]>([])
const isLoading = ref(true)
const hasLoadError = ref(false)
const isDialogOpen = ref(false)
const isSaving = ref(false)
const editingRecordId = ref<number | null>(null)
const formRef = ref<FormInstance>()

interface WeightForm {
  recordDate: string
  weight: string
  remark: string
}

function localToday(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function blankForm(): WeightForm {
  return { recordDate: localToday(), weight: '', remark: '' }
}

const form = ref<WeightForm>(blankForm())
const dialogTitle = computed(() => editingRecordId.value ? '修改体重记录' : '记录体重')
const points = computed(() => weightChartPoints(records.value))
const currentWeight = computed(() => points.value.at(-1)?.weight ?? null)
const weightChange = computed(() => latestWeightChange(records.value))
const newestFirst = computed(() => [...records.value].reverse())

const validateWeight = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  const normalized = value.trim()
  const weight = Number(normalized)
  const decimals = normalized.includes('.') ? normalized.split('.')[1]?.length ?? 0 : 0
  if (!normalized || !Number.isFinite(weight) || weight < 0.001 || weight > 500 || decimals > 3) {
    callback(new Error('体重范围为 0.001–500 kg，最多三位小数'))
    return
  }
  callback()
}

const formRules: FormRules<WeightForm> = {
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
  weight: [{ validator: validateWeight, trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function disableFutureDates(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

async function loadRecords(): Promise<void> {
  isLoading.value = true
  hasLoadError.value = false
  try {
    records.value = await getWeightRecords(props.petId)
    emit('current-weight-changed', currentWeight.value)
  } catch (error) {
    hasLoadError.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取体重记录。'))
  } finally {
    isLoading.value = false
  }
}

function openCreate(): void {
  editingRecordId.value = null
  form.value = blankForm()
  isDialogOpen.value = true
}

function openEdit(record: WeightRecord): void {
  editingRecordId.value = record.id
  form.value = {
    recordDate: record.recordDate,
    weight: String(record.weight),
    remark: record.remark ?? '',
  }
  isDialogOpen.value = true
}

async function saveRecord(): Promise<void> {
  if (!formRef.value || isSaving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const payload: WeightRecordPayload = {
    recordDate: form.value.recordDate,
    weight: Number(form.value.weight),
    remark: form.value.remark.trim() || undefined,
  }
  isSaving.value = true
  try {
    if (editingRecordId.value) {
      await updateWeightRecord(props.petId, editingRecordId.value, payload)
      ElMessage.success('体重记录已更新。')
    } else {
      await createWeightRecord(props.petId, payload)
      ElMessage.success('体重记录已保存。')
    }
    isDialogOpen.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法保存体重记录。'))
  } finally {
    isSaving.value = false
  }
}

async function removeRecord(record: WeightRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除 ${record.recordDate} 的 ${record.weight} kg 记录吗？`,
    '删除体重记录',
    { confirmButtonText: '删除记录', cancelButtonText: '保留记录', type: 'warning' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  try {
    await deleteWeightRecord(props.petId, record.id)
    ElMessage.success('体重记录已删除。')
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法删除体重记录。'))
  }
}

function formatWeight(value: number): string {
  return Number(value.toFixed(3)).toString()
}

watch(() => props.petId, () => void loadRecords())
onMounted(loadRecords)
</script>

<template>
  <section class="weight-panel" aria-label="宠物体重记录">
    <header class="panel-heading">
      <div>
        <p class="eyebrow">GROWING, ONE DAY AT A TIME</p>
        <h2>体重记录</h2>
        <p class="panel-copy">每一次称重，都是了解成长的一小步。</p>
      </div>
      <el-button type="primary" round @click="openCreate">＋ 记录体重</el-button>
    </header>

    <el-skeleton v-if="isLoading" :rows="4" animated />
    <section v-else-if="hasLoadError" class="empty-panel">
      <div class="empty-icon" aria-hidden="true">⌁</div>
      <h3>暂时没有读取到体重记录</h3>
      <p>请检查网络后重新试一次。</p>
      <el-button round @click="loadRecords">重新加载</el-button>
    </section>
    <template v-else>
      <section class="weight-summary">
        <div>
          <p class="summary-label">当前体重</p>
          <div class="summary-number">
            <strong>{{ currentWeight == null ? '—' : formatWeight(currentWeight) }}</strong>
            <span v-if="currentWeight != null">kg</span>
          </div>
        </div>
        <div v-if="weightChange != null" class="change-pill" :class="weightChange > 0 ? 'change-pill--up' : weightChange < 0 ? 'change-pill--down' : ''">
          <span class="change-label">与上次记录相比</span>
          <strong>{{ formatWeightChange(weightChange) }}</strong>
        </div>
        <div v-else-if="records.length === 1" class="change-pill change-pill--quiet">
          <span class="change-label">已记录 1 次</span>
          <strong>继续记录后查看变化</strong>
        </div>
      </section>

      <section v-if="records.length > 0" class="chart-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">WEIGHT JOURNEY</p>
            <h3>体重变化</h3>
          </div>
        </div>
        <WeightTrendChart :points="points" />
      </section>
      <section v-else class="empty-panel empty-panel--short">
        <div class="empty-icon" aria-hidden="true">⌁</div>
        <h3>还没有体重变化曲线</h3>
        <p>记录第一次称重后，这里会出现成长轨迹。</p>
        <el-button type="primary" round @click="openCreate">记录第一次体重</el-button>
      </section>

      <section class="history-section">
        <div class="section-heading">
          <div>
            <p class="eyebrow">PAST CHECK-INS</p>
            <h3>历史记录</h3>
          </div>
          <span class="record-count">{{ records.length }} 次记录</span>
        </div>
        <div v-if="records.length" class="weight-history">
          <article v-for="record in newestFirst" :key="record.id" class="weight-entry">
            <div class="entry-marker" aria-hidden="true">•</div>
            <div class="entry-main">
              <p class="entry-date">{{ record.recordDate }}</p>
              <p v-if="record.remark" class="entry-remark">{{ record.remark }}</p>
            </div>
            <strong class="entry-weight">{{ formatWeight(record.weight) }} <small>kg</small></strong>
            <div class="entry-actions">
              <el-button text type="primary" @click="openEdit(record)">修改</el-button>
              <el-button text type="danger" @click="removeRecord(record)">删除</el-button>
            </div>
          </article>
        </div>
        <p v-else class="history-empty">添加体重后，记录会保存在这里。</p>
      </section>
    </template>

    <el-dialog v-model="isDialogOpen" :title="dialogTitle" width="min(480px, calc(100vw - 32px))" destroy-on-close>
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        @submit.prevent="saveRecord"
      >
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker
            v-model="form.recordDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY 年 MM 月 DD 日"
            :disabled-date="disableFutureDates"
            placeholder="选择记录日期"
            class="weight-input"
          />
        </el-form-item>
        <el-form-item label="体重（kg）" prop="weight">
          <el-input v-model="form.weight" type="number" min="0.001" max="500" step="0.001" placeholder="例如 4.25" />
        </el-form-item>
        <el-form-item label="备注（选填）" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="例如：早饭前称重" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="isDialogOpen = false">取消</el-button>
        <el-button type="primary" round :loading="isSaving" @click="saveRecord">保存体重</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.weight-panel { display: grid; gap: 20px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0 0 7px; color: #78927c; font-size: 10px; font-weight: 700; letter-spacing: .17em; }
.panel-heading h2 { margin: 0; color: #35443a; font-size: 21px; font-weight: 600; }
.panel-copy { margin: 8px 0 0; color: #829087; font-size: 13px; }
.panel-heading :deep(.el-button--primary), .empty-panel :deep(.el-button--primary) { border: 0; background: #55856d; font-weight: 600; }
.weight-summary { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 23px 27px; border: 1px solid #e5ebe2; border-radius: 22px; background: linear-gradient(115deg, #edf4eb, #fbf8f1); }
.summary-label, .change-label { margin: 0 0 6px; color: #7b887e; font-size: 12px; }
.summary-number { display: flex; align-items: baseline; gap: 8px; color: #33443a; }
.summary-number strong { font-size: 36px; font-weight: 650; letter-spacing: -.05em; }
.summary-number span { color: #758278; font-size: 14px; }
.change-pill { display: grid; justify-items: end; padding: 10px 14px; border-radius: 16px; background: rgb(255 255 255 / 68%); color: #526f5d; }
.change-pill strong { font-size: 15px; }
.change-pill--up { color: #a56e47; }
.change-pill--down { color: #55856d; }
.change-pill--quiet strong { color: #77847a; font-size: 12px; font-weight: 500; }
.chart-card, .history-section, .empty-panel { padding: 24px 26px; border: 1px solid #e5ebe2; border-radius: 22px; background: #fff; box-shadow: 0 12px 34px rgb(47 67 54 / 4%); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.section-heading h3 { margin: 0; color: #35443a; font-size: 17px; font-weight: 600; }
.section-heading .eyebrow { margin-bottom: 5px; }
.record-count { color: #829087; font-size: 12px; }
.weight-history { margin-top: 18px; }
.weight-entry { display: grid; grid-template-columns: 16px minmax(0, 1fr) auto auto; align-items: center; gap: 13px; padding: 16px 0; border-top: 1px solid #edf0eb; }
.entry-marker { color: #73a07f; font-size: 28px; line-height: 1; }
.entry-date { margin: 0; color: #45564b; font-size: 13px; font-weight: 600; }
.entry-remark { overflow-wrap: anywhere; margin: 5px 0 0; color: #839087; font-size: 12px; }
.entry-weight { color: #34473b; font-size: 17px; white-space: nowrap; }
.entry-weight small { color: #879188; font-size: 11px; font-weight: 400; }
.entry-actions { display: flex; }
.history-empty { margin: 18px 0 0; color: #829087; font-size: 13px; }
.empty-panel { display: grid; min-height: 230px; align-content: center; justify-items: center; padding: 28px; border-style: dashed; background: rgb(255 255 255 / 66%); text-align: center; box-shadow: none; }
.empty-panel--short { min-height: 200px; }
.empty-icon { display: grid; width: 54px; height: 54px; margin-bottom: 12px; place-items: center; border-radius: 19px; background: #edf4eb; color: #729477; font-size: 28px; }
.empty-panel h3 { margin: 0; color: #35443a; font-size: 16px; font-weight: 600; }
.empty-panel p { margin: 8px 0 17px; color: #829087; font-size: 13px; }
.weight-input { width: 100%; }
@media (max-width: 620px) { .panel-heading { align-items: flex-start; flex-direction: column; } .weight-summary { align-items: flex-start; flex-direction: column; padding: 20px; } .change-pill { justify-items: start; } .chart-card, .history-section { padding: 20px 16px; } .weight-entry { grid-template-columns: 12px minmax(0, 1fr) auto; gap: 9px; } .entry-actions { grid-column: 2 / -1; justify-content: flex-end; } }
</style>
