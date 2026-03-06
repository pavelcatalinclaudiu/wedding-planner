<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

// Forward ?intent=enquiry through to couple registration
const intentQuery = computed(() =>
  route.query.intent === "enquiry" ? "?intent=enquiry" : "",
);
</script>

<template>
  <div class="auth-page">
    <div class="auth-card register-choice">
      <div class="auth-logo">Eternelle</div>
      <h1 class="auth-title">{{ t("auth.register.title") }}</h1>
      <p class="auth-subtitle">{{ t("auth.register.subtitle") }}</p>

      <div class="role-cards">
        <button
          class="role-card"
          @click="router.push(`/register/couple${intentQuery}`)"
        >
          <span class="role-icon">💍</span>
          <h2>{{ t("auth.register.coupleCard.title") }}</h2>
          <p>{{ t("auth.register.coupleCard.description") }}</p>
        </button>
        <button class="role-card" @click="router.push('/register/vendor')">
          <span class="role-icon">🎉</span>
          <h2>{{ t("auth.register.vendorCard.title") }}</h2>
          <p>{{ t("auth.register.vendorCard.description") }}</p>
        </button>
      </div>

      <p class="auth-links">
        {{ t("auth.register.alreadyHaveAccount") }}
        <RouterLink to="/login">{{ t("auth.register.signIn") }}</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface);
}
.auth-card {
  background: var(--color-white);
  border-radius: 16px;
  padding: 48px;
  width: 100%;
  max-width: 560px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
  text-align: center;
}
.auth-logo {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 24px;
}
.auth-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin: 0 0 8px;
}
.auth-subtitle {
  color: var(--color-muted);
  margin: 0 0 40px;
}
.role-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 32px;
}
.role-card {
  background: var(--color-surface);
  border: 2px solid var(--color-border);
  border-radius: 12px;
  padding: 32px 24px;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s,
    background 0.2s;
}
.role-card:hover {
  border-color: var(--color-gold);
  background: var(--color-gold-light, #fdf8ee);
}
.role-icon {
  font-size: 2rem;
  display: block;
  margin-bottom: 12px;
}
.role-card h2 {
  font-size: 1rem;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--color-text);
}
.role-card p {
  font-size: 0.875rem;
  color: var(--color-muted);
  margin: 0;
  line-height: 1.5;
}
.auth-links {
  font-size: 0.875rem;
  color: var(--color-muted);
}
.auth-links a {
  color: var(--color-gold);
  text-decoration: none;
}
</style>
