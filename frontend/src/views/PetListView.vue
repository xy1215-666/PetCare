<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getApiErrorMessage } from '../api/errors'
import { getMyPets } from '../api/pets'
import type { PetGender, PetProfile, PetType } from '../api/pets'

const pets = ref<PetProfile[]>([])
const isLoading = ref(true)

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

async function loadPets(): Promise<void> {
  isLoading.value = true
  try {
    pets.value = await getMyPets()
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '暂时无法读取宠物档案。'))
  } finally {
    isLoading.value = false
  }
}

function petInitial(pet: PetProfile): string {
  return pet.name.slice(0, 1).toUpperCase()
}

onMounted(loadPets)
</script>

<template>
  <div class="pets-page">
    <header class="page-heading">
      <div>
        <p class="eyebrow">YOUR COMPANIONS</p>
        <h1>我的宠物</h1>
        <p class="heading-copy">每一位家庭成员，都值得拥有自己的小档案。</p>
      </div>
      <RouterLink class="add-link" to="/pets/new">
        <el-button type="primary" round>＋ 新增宠物</el-button>
      </RouterLink>
    </header>

    <el-skeleton v-if="isLoading" :rows="4" animated />

    <section v-else-if="pets.length === 0" class="empty-card">
      <div class="empty-mark" aria-hidden="true">♡</div>
      <p class="eyebrow">A NEW LITTLE STORY</p>
      <h2>你的宠物档案还在等一个名字</h2>
      <p>添加名字、生日和日常信息，让重要的点滴从这里开始。</p>
      <RouterLink to="/pets/new">
        <el-button type="primary" round>创建第一份档案</el-button>
      </RouterLink>
    </section>

    <section v-else class="pet-grid" aria-label="宠物档案列表">
      <RouterLink
        v-for="pet in pets"
        :key="pet.id"
        class="pet-card"
        :to="`/pets/${pet.id}`"
      >
        <div class="pet-card-top">
          <span class="pet-avatar" :class="`pet-avatar--${pet.type.toLowerCase()}`">
            {{ petInitial(pet) }}
          </span>
          <span class="pet-kind">{{ petTypeNames[pet.type] }}</span>
        </div>
        <h2>{{ pet.name }}</h2>
        <p class="pet-breed">{{ pet.breed || '品种未填写' }}</p>
        <div class="pet-meta">
          <span>{{ genderNames[pet.gender] }}</span>
          <span class="meta-divider" />
          <span>{{ pet.birthday ? `生日 ${pet.birthday}` : '生日未填写' }}</span>
        </div>
        <div class="pet-card-footer">
          <span>{{ pet.weight == null ? '体重未填写' : `${pet.weight} kg` }}</span>
          <span class="card-arrow" aria-hidden="true">→</span>
        </div>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.pets-page {
  display: grid;
  gap: 30px;
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
}

.add-link :deep(.el-button),
.empty-card :deep(.el-button) {
  min-height: 42px;
  padding: 0 19px;
  border: 0;
  background: #55856d;
  font-weight: 600;
}

.empty-card {
  display: grid;
  min-height: 340px;
  align-content: center;
  justify-items: center;
  padding: 36px 24px;
  border: 1px dashed #d8e3d7;
  border-radius: 26px;
  background: rgb(255 255 255 / 66%);
  text-align: center;
}

.empty-mark {
  display: grid;
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  place-items: center;
  border-radius: 22px;
  background: #edf4eb;
  color: #729477;
  font-size: 34px;
}

.empty-card h2 {
  margin: 0;
  color: #35443a;
  font-size: 21px;
  font-weight: 600;
}

.empty-card > p:not(.eyebrow) {
  margin: 12px 0 22px;
  color: #829087;
  font-size: 13px;
  line-height: 1.8;
}

.pet-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.pet-card {
  display: flex;
  min-height: 240px;
  flex-direction: column;
  padding: 22px;
  border: 1px solid #e6ebe3;
  border-radius: 23px;
  background: rgb(255 255 255 / 90%);
  box-shadow: 0 12px 36px rgb(50 70 54 / 4%);
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.pet-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 18px 42px rgb(50 70 54 / 9%);
}

.pet-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.pet-avatar {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border: 1px solid rgb(89 131 100 / 13%);
  border-radius: 20px;
  background: #ecf3e9;
  color: #588064;
  font-size: 21px;
  font-weight: 700;
}

.pet-avatar--dog {
  background: #f7eee3;
  color: #b27d58;
}

.pet-avatar--other {
  background: #eeeafa;
  color: #8172a2;
}

.pet-kind {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f3f6f1;
  color: #6d8171;
  font-size: 11px;
}

.pet-card h2 {
  margin: 18px 0 4px;
  color: #303e34;
  font-size: 19px;
  font-weight: 600;
}

.pet-breed {
  margin: 0;
  color: #849087;
  font-size: 12px;
}

.pet-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 17px;
  color: #738077;
  font-size: 11px;
}

.meta-divider {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #aab5ab;
}

.pet-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 15px;
  border-top: 1px solid #edf0eb;
  color: #78907d;
  font-size: 12px;
}

.card-arrow {
  color: #5d8568;
  font-size: 18px;
}

@media (max-width: 900px) {
  .pet-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .page-heading {
    align-items: start;
  }

  .page-heading h1 {
    font-size: 28px;
  }

  .add-link :deep(.el-button) {
    padding: 0 13px;
  }

  .pet-grid {
    grid-template-columns: 1fr;
  }
}
</style>
