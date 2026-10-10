<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createVaccination, deleteVaccination, getVaccinations, updateVaccination } from '../api/vaccinations'
import type { VaccinationPayload, VaccinationRecord } from '../api/vaccinations'
import { getApiErrorMessage } from '../api/errors'
import { dateAfter, isDueWithinDays, localDateString } from '../domain/recordDates'

const props = defineProps<{ petId: number }>()
interface VaccinationForm {
  vaccineName: string
  vaccinationDate: string
  nextDate: string
  hospital: string
  batchNumber: string
  remark: string
}

const records = ref<VaccinationRecord[]>([])
const isLoading = ref(true)
const hasLoadError = ref(false)
const isDialogOpen = ref(false)
const isSaving = ref(false)
const editingRecordId = ref<number | null>(null)
const form = ref<VaccinationForm>(blankForm())
const dialogTitle = computed(() => editingRecordId.value ? '修改疫苗记录' : '添加疫苗记录')

function blankForm(): VaccinationForm {
  return { vaccineName: '', vaccinationDate: localDateString(), nextDate: '', hospital: '', batchNumber: '', remark: '' }
}

function dateLabel(date: string | null): string {
  return date || '未安排'
}

function isOverdue(date: string | null): boolean {
  return date !== null && date < localDateString()
}

function disableUnreasonableFutureDate(date: Date): boolean {
  return localDateString(date) > dateAfter(365)
}

async function loadRecords(): Promise<void> {
  isLoading.value = true
  hasLoadError.value = false
  try {
    records.value = await getVaccinations(props.petId)
  } catch (error) {
    hasLoadError.value = true
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取疫苗记录。'))
  } finally {
    isLoading.value = false
  }
}

function openCreate(): void {
  editingRecordId.value = null
  form.value = blankForm()
  isDialogOpen.value = true
}

function openEdit(record: VaccinationRecord): void {
  editingRecordId.value = record.id
  form.value = {
    vaccineName: record.vaccineName,
    vaccinationDate: record.vaccinationDate,
    nextDate: record.nextDate ?? '',
    hospital: record.hospital ?? '',
    batchNumber: record.batchNumber ?? '',
    remark: record.remark ?? '',
  }
  isDialogOpen.value = true
}

async function saveRecord(): Promise<void> {
  if (isSaving.value) return
  const name = form.value.vaccineName.trim()
  if (!name || !form.value.vaccinationDate) {
    ElMessage.warning('请填写疫苗名称和接种日期。')
    return
  }
  if (form.value.vaccinationDate > dateAfter(365)) {
    ElMessage.warning('接种日期不能超过未来一年。')
    return
  }
  if (form.value.nextDate && form.value.nextDate < form.value.vaccinationDate) {
    ElMessage.warning('下次接种日期不能早于本次接种日期。')
    return
  }

  const payload: VaccinationPayload = {
    vaccineName: name,
    vaccinationDate: form.value.vaccinationDate,
    nextDate: form.value.nextDate || undefined,
    hospital: form.value.hospital.trim() || undefined,
    batchNumber: form.value.batchNumber.trim() || undefined,
    remark: form.value.remark.trim() || undefined,
  }
  isSaving.value = true
  try {
    if (editingRecordId.value) {
      await updateVaccination(props.petId, editingRecordId.value, payload)
      ElMessage.success('疫苗记录已更新。')
    } else {
      await createVaccination(props.petId, payload)
      ElMessage.success('疫苗记录已保存。')
    }
    isDialogOpen.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法保存疫苗记录。'))
  } finally {
    isSaving.value = false
  }
}

async function removeRecord(record: VaccinationRecord): Promise<void> {
  const confirmed = await ElMessageBox.confirm(
    `确定删除“${record.vaccineName}”这条接种记录吗？`,
    '删除疫苗记录',
    { confirmButtonText: '删除记录', cancelButtonText: '保留记录', type: 'warning' },
  ).then(() => true).catch(() => false)
  if (!confirmed) return
  try {
    await deleteVaccination(props.petId, record.id)
    ElMessage.success('疫苗记录已删除。')
    await loadRecords()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法删除疫苗记录。'))
  }
}

watch(() => props.petId, () => void loadRecords())
onMounted(loadRecords)
</script>

<template>
  <section class="care-panel" aria-label="宠物疫苗记录">
    <header class="panel-heading">
      <div>
        <p class="eyebrow">VACCINATION NOTES</p>
        <h2>疫苗记录</h2>
        <p class="panel-copy">保存接种时间和下一次计划，照护安排更安心。</p>
      </div>
      <el-button type="primary" round @click="openCreate">＋ 添加记录</el-button>
    </header>

    <el-skeleton v-if="isLoading" :rows="3" animated />
    <section v-else-if="hasLoadError" class="empty-panel">
      <h3>暂时没有读取到疫苗记录</h3>
      <p>请检查网络后重新试一次。</p>
      <el-button round @click="loadRecords">重新加载</el-button>
    </section>
    <section v-else-if="records.length === 0" class="empty-panel">
      <div class="empty-icon" aria-hidden="true">✳</div>
      <h3>还没有疫苗记录</h3>
      <p>记录每一次接种，下一次计划也能一起保存。</p>
      <el-button type="primary" round @click="openCreate">添加第一条记录</el-button>
    </section>
    <div v-else class="care-list">
      <article v-for="record in records" :key="record.id" class="care-card">
        <header class="care-card-heading">
          <div>
            <p class="record-kicker">VACCINATION</p>
            <h3>{{ record.vaccineName }}</h3>
          </div>
          <el-tag v-if="isOverdue(record.nextDate)" type="danger" effect="light" round>已超过计划日期</el-tag>
          <el-tag v-else-if="isDueWithinDays(record.nextDate, 30)" type="warning" effect="light" round>即将到期</el-tag>
        </header>
        <dl class="care-details">
          <div><dt>本次接种</dt><dd>{{ record.vaccinationDate }}</dd></div>
          <div><dt>下次接种</dt><dd>{{ dateLabel(record.nextDate) }}</dd></div>
          <div v-if="record.hospital"><dt>医院</dt><dd>{{ record.hospital }}</dd></div>
          <div v-if="record.batchNumber"><dt>批次</dt><dd>{{ record.batchNumber }}</dd></div>
        </dl>
        <p v-if="record.remark" class="care-note">{{ record.remark }}</p>
        <footer class="record-actions">
          <el-button text type="primary" @click="openEdit(record)">修改</el-button>
          <el-button text type="danger" @click="removeRecord(record)">删除</el-button>
        </footer>
      </article>
    </div>

    <el-dialog v-model="isDialogOpen" :title="dialogTitle" width="min(560px, calc(100vw - 32px))" destroy-on-close>
      <el-form label-position="top" class="care-form" @submit.prevent="saveRecord">
        <el-form-item label="疫苗名称" required>
          <el-input v-model="form.vaccineName" maxlength="120" placeholder="例如：狂犬疫苗" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="接种日期" required>
            <el-date-picker v-model="form.vaccinationDate" type="date" value-format="YYYY-MM-DD" format="YYYY 年 MM 月 DD 日" :disabled-date="disableUnreasonableFutureDate" placeholder="选择接种日期" />
          </el-form-item>
          <el-form-item label="下次接种">
            <el-date-picker v-model="form.nextDate" type="date" value-format="YYYY-MM-DD" format="YYYY 年 MM 月 DD 日" placeholder="可稍后安排" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="医院"><el-input v-model="form.hospital" maxlength="160" placeholder="宠物医院名称" /></el-form-item>
          <el-form-item label="疫苗批次"><el-input v-model="form.batchNumber" maxlength="100" placeholder="选填" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="isDialogOpen = false">取消</el-button>
        <el-button type="primary" round :loading="isSaving" @click="saveRecord">保存记录</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.care-panel { display: grid; gap: 22px; }
.panel-heading, .care-card-heading { display: flex; align-items: center; justify-content: space-between; gap: 18px; }
.panel-heading h2 { margin: 0; color: #35443a; font-size: 19px; }
.eyebrow, .record-kicker { margin: 0 0 6px; color: #78927c; font-size: 10px; font-weight: 700; letter-spacing: .16em; }
.panel-copy { margin: 8px 0 0; color: #79867c; font-size: 13px; line-height: 1.7; }
.care-list { display: grid; gap: 14px; }
.care-card, .empty-panel { padding: 22px; border: 1px solid #e5ebe2; border-radius: 22px; background: #fff; box-shadow: 0 12px 32px rgb(47 67 54 / 4%); }
.care-card h3 { margin: 0; color: #35443a; font-size: 17px; }
.care-details { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 15px; margin: 20px 0 0; }
.care-details dt { margin-bottom: 6px; color: #879188; font-size: 11px; }
.care-details dd { margin: 0; color: #46564a; font-size: 13px; font-weight: 600; overflow-wrap: anywhere; }
.care-note { margin: 16px 0 0; color: #718075; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.record-actions { display: flex; justify-content: flex-end; margin-top: 12px; }
.empty-panel { display: grid; min-height: 230px; justify-items: center; align-content: center; text-align: center; }
.empty-panel h3 { margin: 8px 0 0; color: #3f4d43; font-size: 16px; }
.empty-panel p { margin: 8px 0 16px; color: #839087; font-size: 13px; }
.empty-icon { color: #7e9a81; font-size: 30px; }
.care-form { padding: 4px 8px 0; }
.care-form :deep(.el-date-editor) { width: 100%; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
@media (max-width: 560px) { .panel-heading { align-items: flex-start; flex-direction: column; } .care-card-heading { align-items: flex-start; } .care-details, .form-grid { grid-template-columns: 1fr; } }
</style>
