<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMedicalRecord,
  deleteMedicalRecord,
  getMedicalRecords,
  updateMedicalRecord,
} from '../api/medicalRecords'
import type { MedicalRecord, MedicalRecordPayload } from '../api/medicalRecords'
import { getApiErrorMessage } from '../api/errors'
import { localDateString } from '../domain/recordDates'

const props = defineProps<{ petId: number }>()
interface MedicalForm {
  visitDate: string
  hospital: string
  reason: string
  examination: string
  result: string
  medicine: string
  doctorAdvice: string
  cost: string
  nextVisitDate: string
  remark: string
}

const records = ref<MedicalRecord[]>([])
const isLoading = ref(true)
const hasLoadError = ref(false)
const isDialogOpen = ref(false)
const isSaving = ref(false)
const editingRecordId = ref<number | null>(null)
const form = ref<MedicalForm>(blankForm())
const dialogTitle = computed(() => editingRecordId.value ? '修改就医记录' : '添加就医记录')

function blankForm(): MedicalForm {
  return {
    visitDate: localDateString(), hospital: '', reason: '', examination: '', result: '', medicine: '',
    doctorAdvice: '', cost: '0.00', nextVisitDate: '', remark: '',
  }
}

function disableFutureVisitDate(date: Date): boolean {
  return localDateString(date) > localDateString()
}

function formatCost(cost: number): string {
  return `¥${Number(cost).toFixed(2)}`
}

async function loadRecords(): Promise<void> {
  isLoading.value = true
  hasLoadError.value = false
  try {
    records.value = await getMedicalRecords(props.petId)
  } catch (error) {
    hasLoadError.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取就医记录。'))
  } finally {
    isLoading.value = false
  }
}

function openCreate(): void {
  editingRecordId.value = null
  form.value = blankForm()
  isDialogOpen.value = true
}

function openEdit(record: MedicalRecord): void {
  editingRecordId.value = record.id
  form.value = {
    visitDate: record.visitDate,
    hospital: record.hospital ?? '',
    reason: record.reason,
    examination: record.examination ?? '',
    result: record.result ?? '',
    medicine: record.medicine ?? '',
    doctorAdvice: record.doctorAdvice ?? '',
    cost: Number(record.cost).toFixed(2),
    nextVisitDate: record.nextVisitDate ?? '',
    remark: record.remark ?? '',
  }
  isDialogOpen.value = true
}

function parseCost(value: string): number | null {
  const normalized = value.trim()
  if (!/^\d{1,8}(\.\d{1,2})?$/.test(normalized)) return null
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : null
}

async function saveRecord(): Promise<void> {
  if (isSaving.value) return
  if (!form.value.visitDate || !form.value.reason.trim()) {
    ElMessage.warning('请填写就诊日期和就诊原因。')
    return
  }
  if (form.value.visitDate > localDateString()) {
    ElMessage.warning('就诊日期不能晚于今天。')
    return
  }
  if (form.value.nextVisitDate && form.value.nextVisitDate < form.value.visitDate) {
    ElMessage.warning('复诊日期不能早于就诊日期。')
    return
  }
  const cost = parseCost(form.value.cost)
  if (cost === null) {
    ElMessage.warning('费用请输入非负金额，最多两位小数。')
    return
  }

  const payload: MedicalRecordPayload = {
    visitDate: form.value.visitDate,
    hospital: form.value.hospital.trim() || undefined,
    reason: form.value.reason.trim(),
    examination: form.value.examination.trim() || undefined,
    result: form.value.result.trim() || undefined,
    medicine: form.value.medicine.trim() || undefined,
    doctorAdvice: form.value.doctorAdvice.trim() || undefined,
    cost,
    nextVisitDate: form.value.nextVisitDate || undefined,
    remark: form.value.remark.trim() || undefined,
  }
  isSaving.value = true
  try {
    if (editingRecordId.value) {
      await updateMedicalRecord(props.petId, editingRecordId.value, payload)
      ElMessage.success('就医记录已更新。')
    } else {
      await createMedicalRecord(props.petId, payload)
      ElMessage.success('就医记录已保存。')
    }
    isDialogOpen.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法保存就医记录。'))
  } finally {
    isSaving.value = false
  }
}

async function removeRecord(record: MedicalRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除 ${record.visitDate} 的就医记录吗？`,
    '删除就医记录',
    { confirmButtonText: '删除记录', cancelButtonText: '保留记录', type: 'warning' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  try {
    await deleteMedicalRecord(props.petId, record.id)
    ElMessage.success('就医记录已删除。')
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法删除就医记录。'))
  }
}

watch(() => props.petId, () => void loadRecords())
onMounted(loadRecords)
</script>

<template>
  <section class="medical-panel" aria-label="宠物就医记录">
    <header class="panel-heading">
      <div>
        <p class="eyebrow">VISIT HISTORY</p>
        <h2>就医记录</h2>
        <p class="panel-copy">保存医院、检查与医生给出的信息，方便日后查看。</p>
      </div>
      <el-button type="primary" round @click="openCreate">＋ 添加就医记录</el-button>
    </header>
    <p class="medical-note">这里记录你填写的真实就医信息，PetCare 不提供医疗诊断。</p>

    <el-skeleton v-if="isLoading" :rows="4" animated />
    <section v-else-if="hasLoadError" class="empty-panel">
      <h3>暂时没有读取到就医记录</h3>
      <p>请检查网络后重新试一次。</p>
      <el-button round @click="loadRecords">重新加载</el-button>
    </section>
    <section v-else-if="records.length === 0" class="empty-panel">
      <div class="empty-icon" aria-hidden="true">♡</div>
      <h3>还没有就医记录</h3>
      <p>记录一次就诊，检查结果和复诊日期会保存在这里。</p>
      <el-button type="primary" round @click="openCreate">添加第一条记录</el-button>
    </section>
    <ol v-else class="medical-timeline">
      <li v-for="record in records" :key="record.id" class="timeline-entry">
        <time class="timeline-date">{{ record.visitDate }}</time>
        <article class="medical-card">
          <header class="medical-card-heading">
            <div>
              <p class="record-kicker">{{ record.hospital || '一次就诊记录' }}</p>
              <h3>{{ record.reason }}</h3>
            </div>
            <strong class="cost-pill">{{ formatCost(record.cost) }}</strong>
          </header>
          <dl class="medical-details">
            <div v-if="record.examination"><dt>检查项目</dt><dd>{{ record.examination }}</dd></div>
            <div v-if="record.result"><dt>检查结果</dt><dd>{{ record.result }}</dd></div>
            <div v-if="record.medicine"><dt>用药</dt><dd>{{ record.medicine }}</dd></div>
            <div v-if="record.doctorAdvice"><dt>医生建议</dt><dd>{{ record.doctorAdvice }}</dd></div>
            <div v-if="record.nextVisitDate"><dt>下次复诊</dt><dd>{{ record.nextVisitDate }}</dd></div>
          </dl>
          <p v-if="record.remark" class="care-note">{{ record.remark }}</p>
          <footer class="record-actions">
            <el-button text type="primary" @click="openEdit(record)">修改</el-button>
            <el-button text type="danger" @click="removeRecord(record)">删除</el-button>
          </footer>
        </article>
      </li>
    </ol>

    <el-dialog v-model="isDialogOpen" :title="dialogTitle" width="min(680px, calc(100vw - 32px))" destroy-on-close>
      <el-form label-position="top" class="medical-form" @submit.prevent="saveRecord">
        <div class="form-grid">
          <el-form-item label="就诊日期" required>
            <el-date-picker v-model="form.visitDate" type="date" value-format="YYYY-MM-DD" format="YYYY 年 MM 月 DD 日" :disabled-date="disableFutureVisitDate" placeholder="选择就诊日期" />
          </el-form-item>
          <el-form-item label="医院"><el-input v-model="form.hospital" maxlength="160" placeholder="宠物医院名称" /></el-form-item>
        </div>
        <el-form-item label="就诊原因" required><el-input v-model="form.reason" maxlength="500" placeholder="例如：食欲下降" /></el-form-item>
        <el-form-item label="检查项目"><el-input v-model="form.examination" type="textarea" :rows="2" maxlength="2000" /></el-form-item>
        <el-form-item label="检查结果"><el-input v-model="form.result" type="textarea" :rows="2" maxlength="2000" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="用药"><el-input v-model="form.medicine" maxlength="1000" placeholder="选填" /></el-form-item>
          <el-form-item label="费用"><el-input v-model="form.cost" inputmode="decimal" placeholder="例如：320.00"><template #prepend>¥</template></el-input></el-form-item>
        </div>
        <el-form-item label="医生建议"><el-input v-model="form.doctorAdvice" type="textarea" :rows="2" maxlength="2000" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="下次复诊"><el-date-picker v-model="form.nextVisitDate" type="date" value-format="YYYY-MM-DD" format="YYYY 年 MM 月 DD 日" placeholder="可稍后安排" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="form.remark" maxlength="500" placeholder="选填" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button round @click="isDialogOpen = false">取消</el-button>
        <el-button type="primary" round :loading="isSaving" @click="saveRecord">保存记录</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.medical-panel { display: grid; gap: 18px; }
.panel-heading, .medical-card-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.panel-heading h2 { margin: 0; color: #35443a; font-size: 19px; }
.eyebrow, .record-kicker { margin: 0 0 6px; color: #78927c; font-size: 10px; font-weight: 700; letter-spacing: .15em; }
.panel-copy { margin: 8px 0 0; color: #79867c; font-size: 13px; line-height: 1.7; }
.medical-note { margin: 0; padding: 12px 15px; border-radius: 13px; background: #f4f6ef; color: #718075; font-size: 12px; line-height: 1.7; }
.medical-timeline { display: grid; gap: 18px; margin: 0; padding: 0; list-style: none; }
.timeline-entry { display: grid; grid-template-columns: 100px minmax(0, 1fr); align-items: start; gap: 16px; }
.timeline-date { padding-top: 21px; color: #718775; font-size: 12px; font-weight: 700; }
.medical-card, .empty-panel { padding: 22px; border: 1px solid #e5ebe2; border-radius: 22px; background: #fff; box-shadow: 0 12px 32px rgb(47 67 54 / 4%); }
.medical-card h3 { margin: 0; color: #35443a; font-size: 16px; }
.record-kicker { color: #879188; letter-spacing: .02em; font-weight: 500; }
.cost-pill { flex: 0 0 auto; padding: 8px 11px; border-radius: 999px; background: #f4f0e7; color: #8e704d; font-size: 12px; }
.medical-details { display: grid; gap: 14px; margin: 18px 0 0; }
.medical-details dt { margin-bottom: 5px; color: #879188; font-size: 11px; }
.medical-details dd { margin: 0; color: #46564a; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.care-note { margin: 15px 0 0; color: #718075; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.record-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.empty-panel { display: grid; min-height: 230px; justify-items: center; align-content: center; text-align: center; }
.empty-panel h3 { margin: 8px 0 0; color: #3f4d43; font-size: 16px; }
.empty-panel p { margin: 8px 0 16px; color: #839087; font-size: 13px; }
.empty-icon { color: #7e9a81; font-size: 30px; }
.medical-form { padding: 4px 8px 0; }
.medical-form :deep(.el-date-editor) { width: 100%; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
@media (max-width: 640px) { .panel-heading, .medical-card-heading { align-items: flex-start; flex-direction: column; } .timeline-entry { grid-template-columns: 1fr; gap: 5px; } .timeline-date { padding-top: 0; } .form-grid { grid-template-columns: 1fr; } }
</style>
