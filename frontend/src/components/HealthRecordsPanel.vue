<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  createHealthRecord,
  deleteHealthRecord,
  getHealthRecords,
  updateHealthRecord,
} from '../api/healthRecords'
import type { HealthRecord, HealthRecordPayload } from '../api/healthRecords'
import { getApiErrorMessage } from '../api/errors'
import { healthStatusLabel, healthStatusOptions } from '../domain/healthLabels'

const props = defineProps<{ petId: number }>()

const records = ref<HealthRecord[]>([])
const isLoading = ref(true)
const hasLoadError = ref(false)
const isDialogOpen = ref(false)
const isSaving = ref(false)
const editingRecordId = ref<number | null>(null)
const formRef = ref<FormInstance>()

function localToday(): string {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function blankForm(): HealthRecordPayload {
  return {
    recordDate: localToday(),
    appetite: 'NORMAL',
    waterIntake: 'NORMAL',
    energyLevel: 'NORMAL',
    activityLevel: 'NORMAL',
    sleepStatus: 'NORMAL',
    stoolStatus: 'NORMAL',
    urinationStatus: 'NORMAL',
    abnormal: false,
  }
}

const form = ref<HealthRecordPayload>(blankForm())
const dialogTitle = computed(() => editingRecordId.value ? '修改健康记录' : '记录今天的状态')
const formRules: FormRules<HealthRecordPayload> = {
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
  appetite: [{ required: true, message: '请选择食欲状态', trigger: 'change' }],
  waterIntake: [{ required: true, message: '请选择饮水情况', trigger: 'change' }],
  energyLevel: [{ required: true, message: '请选择精神状态', trigger: 'change' }],
  activityLevel: [{ required: true, message: '请选择活动状态', trigger: 'change' }],
  sleepStatus: [{ required: true, message: '请选择睡眠情况', trigger: 'change' }],
  stoolStatus: [{ required: true, message: '请选择排便情况', trigger: 'change' }],
  urinationStatus: [{ required: true, message: '请选择排尿情况', trigger: 'change' }],
  abnormalDescription: [{ max: 500, message: '异常描述不能超过 500 个字符', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

async function loadRecords(): Promise<void> {
  isLoading.value = true
  hasLoadError.value = false
  try {
    records.value = await getHealthRecords(props.petId)
  } catch (error) {
    hasLoadError.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取健康记录。'))
  } finally {
    isLoading.value = false
  }
}

function disableFutureDates(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

function openCreate(): void {
  editingRecordId.value = null
  form.value = blankForm()
  isDialogOpen.value = true
}

function openEdit(record: HealthRecord): void {
  editingRecordId.value = record.id
  form.value = {
    recordDate: record.recordDate,
    appetite: record.appetite,
    waterIntake: record.waterIntake,
    energyLevel: record.energyLevel,
    activityLevel: record.activityLevel,
    sleepStatus: record.sleepStatus,
    stoolStatus: record.stoolStatus,
    urinationStatus: record.urinationStatus,
    abnormal: record.abnormal,
    abnormalDescription: record.abnormalDescription ?? undefined,
    remark: record.remark ?? undefined,
  }
  isDialogOpen.value = true
}

async function saveRecord(): Promise<void> {
  if (!formRef.value || isSaving.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.value.abnormal && !form.value.abnormalDescription?.trim()) {
    ElMessage.warning('请简单描述观察到的异常情况。')
    return
  }

  isSaving.value = true
  try {
    const payload = {
      ...form.value,
      abnormalDescription: form.value.abnormal ? form.value.abnormalDescription?.trim() : undefined,
      remark: form.value.remark?.trim() || undefined,
    }
    if (editingRecordId.value) {
      await updateHealthRecord(props.petId, editingRecordId.value, payload)
      ElMessage.success('健康记录已更新。')
    } else {
      await createHealthRecord(props.petId, payload)
      ElMessage.success('今天的健康记录已保存。')
    }
    isDialogOpen.value = false
    await loadRecords()
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 409) {
      ElMessage.warning('这一天已经有一条记录，请编辑当天的记录。')
    } else {
      ElMessage.error(getApiErrorMessage(error, '暂时无法保存健康记录。'))
    }
  } finally {
    isSaving.value = false
  }
}

async function removeRecord(record: HealthRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除 ${record.recordDate} 的健康记录吗？`,
    '删除健康记录',
    { confirmButtonText: '删除记录', cancelButtonText: '保留记录', type: 'warning' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return

  try {
    await deleteHealthRecord(props.petId, record.id)
    ElMessage.success('健康记录已删除。')
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法删除健康记录。'))
  }
}

watch(() => props.petId, () => void loadRecords())
onMounted(loadRecords)
</script>

<template>
  <section class="health-panel" aria-label="宠物健康记录">
    <header class="panel-heading">
      <div>
        <p class="eyebrow">LITTLE DAILY CHECK-IN</p>
        <h2>健康记录</h2>
        <p class="panel-copy">记下每天的变化，慢慢了解它平时的样子。</p>
      </div>
      <el-button type="primary" round @click="openCreate">＋ 记录今天</el-button>
    </header>

    <el-skeleton v-if="isLoading" :rows="4" animated />
    <section v-else-if="hasLoadError" class="empty-panel">
      <div class="empty-icon" aria-hidden="true">♡</div>
      <h3>暂时没有读取到记录</h3>
      <p>请检查网络后重新试一次。</p>
      <el-button round @click="loadRecords">重新加载</el-button>
    </section>
    <section v-else-if="records.length === 0" class="empty-panel">
      <div class="empty-icon" aria-hidden="true">☼</div>
      <h3>第一条健康记录，从今天开始</h3>
      <p>记录食欲、精神和日常状态，以后查看变化会更方便。</p>
      <el-button type="primary" round @click="openCreate">记录今天的状态</el-button>
    </section>
    <div v-else class="health-list">
      <article v-for="record in records" :key="record.id" class="health-card">
        <div class="health-card-top">
          <div>
            <p class="record-date">{{ record.recordDate }}</p>
            <div class="headline-statuses">
              <span>食欲 {{ healthStatusLabel('appetite', record.appetite) }}</span>
              <span>精神 {{ healthStatusLabel('energyLevel', record.energyLevel) }}</span>
              <span>活动 {{ healthStatusLabel('activityLevel', record.activityLevel) }}</span>
            </div>
          </div>
          <el-tag v-if="record.abnormal" type="warning" effect="light" round>有异常</el-tag>
          <el-tag v-else type="success" effect="light" round>状态平稳</el-tag>
        </div>

        <p v-if="record.abnormalDescription" class="abnormal-note">{{ record.abnormalDescription }}</p>
        <details class="more-details">
          <summary>更多日常状态</summary>
          <div class="detail-pills">
            <span>饮水 {{ healthStatusLabel('waterIntake', record.waterIntake) }}</span>
            <span>睡眠 {{ healthStatusLabel('sleepStatus', record.sleepStatus) }}</span>
            <span>排便 {{ healthStatusLabel('stoolStatus', record.stoolStatus) }}</span>
            <span>排尿 {{ healthStatusLabel('urinationStatus', record.urinationStatus) }}</span>
          </div>
          <p v-if="record.remark" class="remark-note">{{ record.remark }}</p>
        </details>
        <footer class="record-actions">
          <el-button text type="primary" @click="openEdit(record)">修改</el-button>
          <el-button text type="danger" @click="removeRecord(record)">删除</el-button>
        </footer>
      </article>
    </div>

    <el-dialog v-model="isDialogOpen" :title="dialogTitle" width="min(640px, calc(100vw - 32px))" destroy-on-close>
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        class="health-form"
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
            class="date-input"
          />
        </el-form-item>

        <el-form-item label="今天吃饭怎么样？" prop="appetite">
          <el-radio-group v-model="form.appetite" class="choice-row">
            <el-radio-button v-for="option in healthStatusOptions.appetite" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="今天精神怎么样？" prop="energyLevel">
          <el-radio-group v-model="form.energyLevel" class="choice-row">
            <el-radio-button v-for="option in healthStatusOptions.energyLevel" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <div class="status-grid">
          <el-form-item label="饮水情况" prop="waterIntake">
            <el-select v-model="form.waterIntake">
              <el-option v-for="option in healthStatusOptions.waterIntake" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="活动状态" prop="activityLevel">
            <el-select v-model="form.activityLevel">
              <el-option v-for="option in healthStatusOptions.activityLevel" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="睡眠情况" prop="sleepStatus">
            <el-select v-model="form.sleepStatus">
              <el-option v-for="option in healthStatusOptions.sleepStatus" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="排便情况" prop="stoolStatus">
            <el-select v-model="form.stoolStatus">
              <el-option v-for="option in healthStatusOptions.stoolStatus" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="排尿情况" prop="urinationStatus">
            <el-select v-model="form.urinationStatus">
              <el-option v-for="option in healthStatusOptions.urinationStatus" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="今天发现异常了吗？">
          <el-switch v-model="form.abnormal" active-text="有异常" inactive-text="没有" />
        </el-form-item>
        <el-form-item v-if="form.abnormal" label="异常情况" prop="abnormalDescription" required>
          <el-input v-model="form.abnormalDescription" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="描述你观察到的情况" />
        </el-form-item>
        <el-form-item label="补充备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="想补充的内容（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="isDialogOpen = false">取消</el-button>
        <el-button type="primary" round :loading="isSaving" @click="saveRecord">保存记录</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.health-panel { display: grid; gap: 22px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0 0 7px; color: #78927c; font-size: 10px; font-weight: 700; letter-spacing: .17em; }
.panel-heading h2 { margin: 0; color: #35443a; font-size: 21px; font-weight: 600; }
.panel-copy { margin: 8px 0 0; color: #829087; font-size: 13px; }
.panel-heading :deep(.el-button--primary), .empty-panel :deep(.el-button--primary) { border: 0; background: #55856d; font-weight: 600; }
.empty-panel { display: grid; min-height: 250px; align-content: center; justify-items: center; padding: 28px; border: 1px dashed #d8e3d7; border-radius: 24px; background: rgb(255 255 255 / 66%); text-align: center; }
.empty-icon { display: grid; width: 55px; height: 55px; margin-bottom: 13px; place-items: center; border-radius: 19px; background: #edf4eb; color: #729477; font-size: 26px; }
.empty-panel h3 { margin: 0; color: #35443a; font-size: 17px; font-weight: 600; }
.empty-panel p { margin: 8px 0 18px; color: #829087; font-size: 13px; }
.health-list { display: grid; gap: 14px; }
.health-card { padding: 22px 24px 16px; border: 1px solid #e5ebe2; border-radius: 22px; background: #fff; box-shadow: 0 12px 34px rgb(47 67 54 / 4%); }
.health-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.record-date { margin: 0 0 12px; color: #33443a; font-size: 14px; font-weight: 700; }
.headline-statuses, .detail-pills { display: flex; flex-wrap: wrap; gap: 8px; }
.headline-statuses span, .detail-pills span { padding: 7px 10px; border-radius: 999px; background: #f1f5ef; color: #63776a; font-size: 12px; }
.abnormal-note { margin: 16px 0 0; padding: 12px 14px; border-radius: 13px; background: #fff7e9; color: #946b36; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.more-details { margin-top: 16px; color: #718077; font-size: 12px; }
.more-details summary { width: fit-content; cursor: pointer; }
.detail-pills { margin-top: 12px; }
.remark-note { margin: 12px 0 0; color: #758278; line-height: 1.7; white-space: pre-wrap; }
.record-actions { display: flex; justify-content: flex-end; margin-top: 10px; }
.health-form { max-height: min(66vh, 700px); overflow: auto; padding: 2px 5px 0 0; }
.date-input { width: 100%; }
.choice-row { display: flex; flex-wrap: wrap; gap: 8px; }
.choice-row :deep(.el-radio-button__inner) { border: 1px solid #dfe8df; border-radius: 999px !important; box-shadow: none !important; color: #66756c; }
.choice-row :deep(.el-radio-button:first-child .el-radio-button__inner), .choice-row :deep(.el-radio-button:last-child .el-radio-button__inner) { border-radius: 999px; }
.choice-row :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) { border-color: #55856d; background: #55856d; box-shadow: none; }
.status-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
@media (max-width: 600px) { .panel-heading { align-items: flex-start; flex-direction: column; } .health-card { padding: 18px 16px 12px; } .status-grid { grid-template-columns: 1fr; } }
</style>
