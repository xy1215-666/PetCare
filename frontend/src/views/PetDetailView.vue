<script setup lang="ts">
import axios from 'axios'
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPet } from '../api/pets'
import type { PetGender, PetProfile, PetType } from '../api/pets'
import HealthRecordsPanel from '../components/HealthRecordsPanel.vue'

const WeightRecordsPanel = defineAsyncComponent(() => import('../components/WeightRecordsPanel.vue'))
const VaccinationPanel = defineAsyncComponent(() => import('../components/VaccinationPanel.vue'))
const DewormingPanel = defineAsyncComponent(() => import('../components/DewormingPanel.vue'))
const MedicalRecordsPanel = defineAsyncComponent(() => import('../components/MedicalRecordsPanel.vue'))
const PetExpensesPanel = defineAsyncComponent(() => import('../components/PetExpensesPanel.vue'))

const route = useRoute()
const pet = ref<PetProfile | null>(null)
const isLoading = ref(true)
const isNotFound = ref(false)
const hasLoadError = ref(false)
const allowedTabs = ['overview', 'health', 'weight', 'vaccinations', 'dewormings', 'medical', 'expenses']
const activeTab = ref(allowedTabs.includes(String(route.query.tab)) ? String(route.query.tab) : 'overview')

watch(() => route.query.tab, (tab) => {
  activeTab.value = allowedTabs.includes(String(tab)) ? String(tab) : 'overview'
})

const petTypeNames: Record<PetType, string> = {
  CAT: '猫咪',
  DOG: '狗狗',
  OTHER: '小伙伴',
}

const genderNames: Record<PetGender, string> = {
  MALE: '男孩',
  FEMALE: '女孩',
  UNKNOWN: '未填写',
}

const petTypeName = computed(() => pet.value ? petTypeNames[pet.value.type] : '')
const genderName = computed(() => pet.value ? genderNames[pet.value.gender] : '未填写')

function updateCurrentWeight(weight: number | null): void {
  if (pet.value) pet.value.weight = weight
}

watch(
  () => route.params.petId,
  async (rawId) => {
    pet.value = null
    isNotFound.value = false
    hasLoadError.value = false
    isLoading.value = true

    const petId = Number(rawId)
    if (!Number.isSafeInteger(petId) || petId <= 0) {
      isNotFound.value = true
      isLoading.value = false
      return
    }

    try {
      pet.value = await getPet(petId)
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        isNotFound.value = true
      } else {
        hasLoadError.value = true
      }
    } finally {
      isLoading.value = false
    }
  },
  { immediate: true },
)
</script>

<template>
  <div class="pet-detail-page">
    <RouterLink class="back-link" :to="{ name: 'pets' }">← 返回我的宠物</RouterLink>

    <el-skeleton v-if="isLoading" :rows="6" animated />

    <section v-else-if="isNotFound" class="state-card">
      <span class="state-mark" aria-hidden="true">♡</span>
      <h1>没有找到这份宠物档案</h1>
      <p>它可能已经被移除，或者不属于当前账号。</p>
      <RouterLink class="primary-link" :to="{ name: 'pets' }">回到宠物列表</RouterLink>
    </section>

    <section v-else-if="hasLoadError" class="state-card">
      <span class="state-mark" aria-hidden="true">…</span>
      <h1>暂时无法读取档案</h1>
      <p>请检查网络连接后重试。</p>
      <button class="primary-link" type="button" @click="$router.go(0)">重新加载</button>
    </section>

    <template v-else-if="pet">
      <header class="profile-hero">
        <div class="pet-avatar" :class="`pet-avatar--${pet.type.toLowerCase()}`">
          {{ pet.name.slice(0, 1) }}
        </div>
        <div class="profile-copy">
          <p class="eyebrow">A LITTLE MEMBER OF YOUR FAMILY</p>
          <div class="title-line">
            <h1>{{ pet.name }}</h1>
            <span class="pet-kind">{{ petTypeName }}</span>
          </div>
          <p class="profile-subtitle">{{ pet.breed || '品种还没有填写' }} · {{ genderName }}</p>
        </div>
      </header>

      <el-tabs v-model="activeTab" class="pet-tabs">
        <el-tab-pane label="概览" name="overview" lazy>
          <section class="detail-card" aria-labelledby="detail-title">
            <div class="card-heading">
              <span class="card-mark" aria-hidden="true">♡</span>
              <div>
                <p class="eyebrow">THE LITTLE DETAILS</p>
                <h2 id="detail-title">基本档案</h2>
              </div>
            </div>

            <dl class="detail-grid">
              <div class="detail-item">
                <dt>出生日期</dt>
                <dd>{{ pet.birthday || '暂未填写' }}</dd>
              </div>
              <div class="detail-item">
                <dt>到家日期</dt>
                <dd>{{ pet.homeDate || '暂未填写' }}</dd>
              </div>
              <div class="detail-item">
                <dt>当前体重</dt>
                <dd>{{ pet.weight == null ? '暂未填写' : `${pet.weight} kg` }}</dd>
              </div>
              <div class="detail-item">
                <dt>绝育情况</dt>
                <dd>{{ pet.neutered == null ? '暂未填写' : pet.neutered ? '已绝育' : '未绝育' }}</dd>
              </div>
              <div class="detail-item detail-item--wide">
                <dt>备注</dt>
                <dd class="remark-value">{{ pet.remark || '还没有添加备注。' }}</dd>
              </div>
            </dl>
          </section>
        </el-tab-pane>
        <el-tab-pane label="健康记录" name="health" lazy>
          <HealthRecordsPanel :pet-id="pet.id" />
        </el-tab-pane>
        <el-tab-pane label="体重记录" name="weight" lazy>
          <WeightRecordsPanel :pet-id="pet.id" @current-weight-changed="updateCurrentWeight" />
        </el-tab-pane>
        <el-tab-pane label="疫苗" name="vaccinations" lazy>
          <VaccinationPanel :pet-id="pet.id" />
        </el-tab-pane>
        <el-tab-pane label="驱虫" name="dewormings" lazy>
          <DewormingPanel :pet-id="pet.id" />
        </el-tab-pane>
        <el-tab-pane label="就医" name="medical" lazy>
          <MedicalRecordsPanel :pet-id="pet.id" />
        </el-tab-pane>
        <el-tab-pane label="消费" name="expenses" lazy>
          <PetExpensesPanel :pet-id="pet.id" />
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<style scoped>
.pet-detail-page {
  display: grid;
  gap: 24px;
}

.back-link {
  width: fit-content;
  color: #66836c;
  font-size: 13px;
  font-weight: 600;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: clamp(24px, 5vw, 42px);
  border: 1px solid #e4ebe1;
  border-radius: 27px;
  background: linear-gradient(118deg, #eaf2e7, #f7f0e7);
}

.pet-avatar {
  display: grid;
  width: 108px;
  height: 108px;
  flex: 0 0 auto;
  place-items: center;
  border: 5px solid rgb(255 255 255 / 78%);
  border-radius: 35px;
  background: #dcebd9;
  box-shadow: 0 12px 28px rgb(57 83 61 / 9%);
  color: #568163;
  font-size: 42px;
  font-weight: 700;
}

.pet-avatar--dog {
  background: #f2e4d4;
  color: #b27d58;
}

.pet-avatar--other {
  background: #e9e4f4;
  color: #8172a2;
}

.profile-copy {
  min-width: 0;
}

.eyebrow {
  margin: 0 0 9px;
  color: #78927c;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.19em;
}

.title-line {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-line h1 {
  overflow-wrap: anywhere;
  margin: 0;
  color: #29392f;
  font-size: clamp(28px, 4vw, 36px);
  font-weight: 600;
  letter-spacing: -0.045em;
}

.pet-kind {
  flex: 0 0 auto;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgb(255 255 255 / 70%);
  color: #6d8171;
  font-size: 11px;
}

.profile-subtitle {
  margin: 10px 0 0;
  color: #758278;
  font-size: 14px;
}

.detail-card,
.state-card {
  padding: clamp(22px, 4vw, 36px);
  border: 1px solid #e5ebe2;
  border-radius: 26px;
  background: rgb(255 255 255 / 92%);
  box-shadow: 0 14px 40px rgb(47 67 54 / 5%);
}

.pet-tabs {
  padding: 0 5px;
}

.pet-tabs :deep(.el-tabs__header) {
  margin-bottom: 22px;
}

.pet-tabs :deep(.el-tabs__item) {
  height: 46px;
  color: #748278;
  font-size: 14px;
}

.pet-tabs :deep(.el-tabs__item.is-active) {
  color: #55856d;
  font-weight: 600;
}

.pet-tabs :deep(.el-tabs__active-bar) {
  background: #55856d;
}

.pet-tabs :deep(.el-tabs__nav-wrap::after) {
  background: #e5ebe2;
}

.card-heading {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 27px;
}

.card-mark,
.state-mark {
  display: grid;
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 17px;
  background: #edf4eb;
  color: #66896e;
  font-size: 24px;
}

.card-heading .eyebrow {
  margin-bottom: 5px;
}

.card-heading h2 {
  margin: 0;
  color: #35443a;
  font-size: 18px;
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 22px;
  margin: 0;
}

.detail-item {
  padding: 19px 0;
  border-top: 1px solid #edf0eb;
}

.detail-item dt {
  margin-bottom: 8px;
  color: #879188;
  font-size: 12px;
}

.detail-item dd {
  overflow-wrap: anywhere;
  margin: 0;
  color: #3c4c41;
  font-size: 14px;
  font-weight: 600;
}

.detail-item--wide {
  grid-column: 1 / -1;
}

.remark-value {
  white-space: pre-wrap;
  font-weight: 400 !important;
  line-height: 1.8;
}

.state-card {
  display: grid;
  min-height: 330px;
  align-content: center;
  justify-items: center;
  text-align: center;
}

.state-mark {
  width: 62px;
  height: 62px;
  margin-bottom: 16px;
  font-size: 31px;
}

.state-card h1 {
  margin: 0;
  color: #35443a;
  font-size: 21px;
}

.state-card p {
  margin: 10px 0 22px;
  color: #829087;
  font-size: 13px;
}

.primary-link {
  display: inline-flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  padding: 0 18px;
  border: 0;
  border-radius: 13px;
  background: #55856d;
  color: #fff;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 560px) {
  .profile-hero {
    align-items: start;
    flex-direction: column;
    gap: 17px;
  }

  .pet-avatar {
    width: 82px;
    height: 82px;
    border-radius: 28px;
    font-size: 32px;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item--wide {
    grid-column: auto;
  }
}
</style>
