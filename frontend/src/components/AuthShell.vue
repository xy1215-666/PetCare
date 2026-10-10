<script setup lang="ts">
defineProps<{
  eyebrow: string
  title: string
  intro: string
}>()
</script>

<template>
  <main class="auth-page">
    <section class="auth-story" aria-label="PetCare AI">
      <RouterLink class="brand" to="/" aria-label="PetCare AI 首页">
        <span class="brand-icon" aria-hidden="true">P</span>
        <span>PetCare <strong>AI</strong></span>
      </RouterLink>
      <div class="story-copy">
        <p class="story-eyebrow">A LITTLE MORE CARE, EVERY DAY</p>
        <h1>把每一天的<br />陪伴，都好好珍藏。</h1>
        <p>从建立专属档案开始，让照护这件事轻松一点、安心一点。</p>
      </div>
      <div class="story-note">
        <span class="note-dot" />
        <span>为你和毛孩子，留一处温柔的日常。</span>
      </div>
    </section>

    <section class="auth-panel">
      <div class="auth-card">
        <p class="auth-eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p class="form-intro">{{ intro }}</p>
        <slot />
        <p class="auth-footer"><slot name="footer" /></p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100vh;
  grid-template-columns: minmax(320px, 0.9fr) minmax(480px, 1.1fr);
  background: #f6f7f2;
}

.auth-story {
  display: flex;
  min-height: 100%;
  flex-direction: column;
  justify-content: space-between;
  padding: clamp(32px, 6vw, 72px);
  background:
    radial-gradient(ellipse at 12% 85%, rgb(228 205 169 / 48%), transparent 34%),
    radial-gradient(ellipse at 90% 20%, rgb(175 203 176 / 64%), transparent 40%),
    #e8efe5;
}

.brand {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 12px;
  color: #2b3d31;
  font-size: 19px;
  font-weight: 500;
}

.brand strong {
  color: #65856c;
  font-weight: 700;
}

.brand-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 14px;
  background: #55856d;
  color: #fff;
  font-size: 19px;
  font-weight: 700;
}

.story-copy {
  max-width: 520px;
  margin: 72px 0;
}

.story-eyebrow,
.auth-eyebrow {
  margin: 0 0 16px;
  color: #65856c;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.17em;
}

.story-copy h1 {
  margin: 0;
  color: #29392f;
  font-size: clamp(34px, 4.2vw, 56px);
  font-weight: 600;
  letter-spacing: -0.055em;
  line-height: 1.28;
}

.story-copy > p:last-child {
  max-width: 360px;
  margin: 24px 0 0;
  color: #65756a;
  font-size: 15px;
  line-height: 1.9;
}

.story-note {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #65756a;
  font-size: 13px;
}

.note-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #7d9f80;
}

.auth-panel {
  display: grid;
  padding: 48px 24px;
  place-items: center;
}

.auth-card {
  width: min(100%, 440px);
  padding: 24px 0;
}

.auth-eyebrow {
  margin-bottom: 16px;
}

.auth-card h2 {
  margin: 0;
  color: #29392f;
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.04em;
}

.form-intro {
  margin: 10px 0 28px;
  color: #7a867d;
  font-size: 14px;
}

.auth-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.auth-card :deep(.el-form-item__label) {
  padding-bottom: 7px;
  color: #48574d;
  font-size: 13px;
  font-weight: 600;
}

.auth-card :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 13px;
  background: #fff;
  box-shadow: 0 0 0 1px #e1e7df inset;
}

.auth-card :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #6f9a78 inset;
}

.auth-card :deep(.submit-button) {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border: 0;
  border-radius: 14px;
  background: #55856d;
  font-weight: 600;
}

.auth-card :deep(.submit-button:hover) {
  background: #466f5a;
}

.auth-footer {
  margin: 24px 0 0;
  color: #7a867d;
  font-size: 13px;
  text-align: center;
}

.auth-footer :deep(a) {
  color: #4e7d63;
  font-weight: 600;
}

@media (max-width: 760px) {
  .auth-page {
    grid-template-columns: 1fr;
  }

  .auth-story {
    min-height: 270px;
    padding: 26px 24px;
  }

  .story-copy {
    margin: 44px 0 24px;
  }

  .story-copy h1 {
    font-size: clamp(30px, 8vw, 42px);
  }

  .story-copy > p:last-child {
    margin-top: 14px;
  }

  .story-note {
    display: none;
  }

  .auth-panel {
    padding: 28px 24px 42px;
    background: #f9faf6;
  }

  .auth-card {
    padding: 8px 0;
  }
}
</style>
