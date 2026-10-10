<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createPet, type PetGender, type PetType } from '../api/pets'
import { getApiErrorMessage } from '../api/errors'

interface PetForm {
  name: string
  type: PetType | ''
  breed: string
  gender: PetGender
  birthday: string
  weight: string
  neutered: '' | 'true' | 'false'
  homeDate: string
  remark: string
}

const router = useRouter()
const formRef = ref<FormInstance>()
const isSubmitting = ref(false)
const form = reactive<PetForm>({
  name: '',
  type: '',
  breed: '',
  gender: 'UNKNOWN',
  birthday: '',
  weight: '',
  neutered: '',
  homeDate: '',
  remark: '',
})

function disableFutureDates(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

const validateWeight = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value.trim()) {
    callback()
    return
  }
  const weight = Number(value)
  const decimalPlaces = value.includes('.') ? value.split('.')[1]?.length ?? 0 : 0
  if (!Number.isFinite(weight) || weight < 0.001 || weight > 500 || decimalPlaces > 3) {
    callback(new Error('体重范围为 0.001–500 kg，最多三位小数'))
    return
  }
  callback()
}

const validateDate = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value || /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    callback()
    return
  }
  callback(new Error('请选择有效的日期'))
}

const rules: FormRules<PetForm> = {
  name: [
    { required: true, message: '请给宠物起一个名字', trigger: 'blur' },
    { max: 50, message: '名字不能超过 50 个字符', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择宠物类型', trigger: 'change' }],
  breed: [{ max: 80, message: '品种不能超过 80 个字符', trigger: 'blur' }],
  birthday: [{ validator: validateDate, trigger: 'change' }],
  weight: [{ validator: validateWeight, trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

async function submitPet(): Promise<void> {
  if (!formRef.value || isSubmitting.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !form.type) return

  isSubmitting.value = true
  try {
    const pet = await createPet({
      name: form.name.trim(),
      type: form.type,
      breed: form.breed.trim() || undefined,
      gender: form.gender,
      birthday: form.birthday || undefined,
      weight: form.weight.trim() ? Number(form.weight) : undefined,
      neutered: form.neutered === '' ? null : form.neutered === 'true',
      homeDate: form.homeDate || undefined,
      remark: form.remark.trim() || undefined,
    })
    ElMessage.success(`${pet.name} 的档案已创建。`)
    await router.push({ name: 'pets' })
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法创建宠物档案。'))
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="pet-create-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">A HOME FOR THEIR STORY</p>
        <h1>新增宠物</h1>
        <p class="heading-copy">先填写你已经知道的信息，其他细节以后再慢慢补上。</p>
      </div>
      <RouterLink class="back-link" :to="{ name: 'pets' }">返回宠物列表</RouterLink>
    </header>

    <section class="form-card">
      <div class="form-heading">
        <span class="form-mark" aria-hidden="true">♡</span>
        <div>
          <h2>基本信息</h2>
          <p>带星号的内容需要填写，其余都可以留空。</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="submitPet"
      >
        <div class="form-grid">
          <el-form-item label="名字" prop="name" required>
            <el-input v-model="form.name" maxlength="50" placeholder="例如：糯米" size="large" />
          </el-form-item>
          <el-form-item label="宠物类型" prop="type" required>
            <el-select v-model="form.type" placeholder="请选择" size="large">
              <el-option label="猫咪" value="CAT" />
              <el-option label="狗狗" value="DOG" />
              <el-option label="其他小伙伴" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="品种" prop="breed">
            <el-input v-model="form.breed" maxlength="80" placeholder="例如：英短、柯基" size="large" />
          </el-form-item>
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" size="large">
              <el-option label="暂不填写" value="UNKNOWN" />
              <el-option label="男孩" value="MALE" />
              <el-option label="女孩" value="FEMALE" />
            </el-select>
          </el-form-item>
          <el-form-item label="出生日期" prop="birthday">
            <el-date-picker
              v-model="form.birthday"
              :disabled-date="disableFutureDates"
              format="YYYY 年 MM 月 DD 日"
              placeholder="选择日期"
              size="large"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item label="当前体重（kg）" prop="weight">
            <el-input
              v-model="form.weight"
              inputmode="decimal"
              max="500"
              min="0.001"
              placeholder="可稍后补充"
              size="large"
              step="0.001"
              type="number"
            />
          </el-form-item>
          <el-form-item label="是否绝育" prop="neutered">
            <el-select v-model="form.neutered" placeholder="暂不填写" size="large">
              <el-option label="暂不填写" value="" />
              <el-option label="已绝育" value="true" />
              <el-option label="未绝育" value="false" />
            </el-select>
          </el-form-item>
          <el-form-item label="到家日期" prop="homeDate">
            <el-date-picker
              v-model="form.homeDate"
              format="YYYY 年 MM 月 DD 日"
              placeholder="选择日期"
              size="large"
              type="date"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
        </div>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            maxlength="500"
            placeholder="写下它的小习惯、喜欢的玩具……"
            :rows="4"
            show-word-limit
            type="textarea"
          />
        </el-form-item>

        <div class="form-actions">
          <RouterLink class="cancel-link" :to="{ name: 'pets' }">取消</RouterLink>
          <el-button :loading="isSubmitting" native-type="submit" size="large" type="primary">
            保存宠物档案
          </el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.pet-create-page {
  display: grid;
  gap: 27px;
}

.page-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.eyebrow {
  margin: 0 0 9px;
  color: #78927c;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.19em;
}

.page-heading h1 {
  margin: 0;
  color: #29392f;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.045em;
}

.heading-copy {
  margin: 10px 0 0;
  color: #78857c;
  font-size: 14px;
  line-height: 1.7;
}

.back-link,
.cancel-link {
  color: #5d7d64;
  font-size: 13px;
  font-weight: 600;
}

.form-card {
  padding: clamp(22px, 4vw, 38px);
  border: 1px solid #e5ebe2;
  border-radius: 26px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 14px 40px rgb(47 67 54 / 5%);
}

.form-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.form-mark {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 17px;
  background: #edf4eb;
  color: #66896e;
  font-size: 24px;
}

.form-heading h2 {
  margin: 0;
  color: #35443a;
  font-size: 17px;
  font-weight: 600;
}

.form-heading p {
  margin: 5px 0 0;
  color: #879188;
  font-size: 12px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 20px;
}

.form-card :deep(.el-form-item) {
  margin-bottom: 20px;
}

.form-card :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #49584d;
  font-size: 13px;
  font-weight: 600;
}

.form-card :deep(.el-input__wrapper),
.form-card :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 0 1px #e2e8e0 inset;
}

.form-card :deep(.el-date-editor.el-input),
.form-card :deep(.el-date-editor.el-input__wrapper),
.form-card :deep(.el-select) {
  width: 100%;
}

.form-card :deep(.el-textarea__inner) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e2e8e0 inset;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 16px;
  padding-top: 22px;
  border-top: 1px solid #edf0eb;
}

.form-actions :deep(.el-button) {
  min-height: 44px;
  padding: 0 22px;
  border: 0;
  border-radius: 13px;
  background: #55856d;
  font-weight: 600;
}

@media (max-width: 620px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .form-actions {
    justify-content: space-between;
  }
}
</style>
