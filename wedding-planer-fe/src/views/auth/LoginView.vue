<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { extractApiError } from "@/api/client";
import { useAuthRedirect } from "@/composables/useAuthRedirect";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const { getEnquiryIntent, redirectAfterAuth } = useAuthRedirect();

const email = ref("");
const password = ref("");
const showPassword = ref(false);
const loading = ref(false);
const error = ref("");

const intent = computed(() =>
  route.query.intent === "enquiry" ? getEnquiryIntent() : null,
);

// Link to couple registration page — preserves the intent query param
const registerLink = computed(() =>
  intent.value ? "/register/couple?intent=enquiry" : "/register",
);

async function handleLogin() {
  if (!email.value || !password.value) return;
  loading.value = true;
  error.value = "";
  try {
    const data = await authStore.login({
      email: email.value,
      password: password.value,
    });
    redirectAfterAuth(data.user.role as "COUPLE" | "VENDOR");
  } catch (e: unknown) {
    error.value = extractApiError(e, t("auth.login.errorInvalid"));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <RouterLink to="/" class="auth-logo">Eternelle</RouterLink>

      <!-- Intent banner: shown when couple was mid-enquiry before auth -->
      <div v-if="intent" class="intent-banner">
        <div
          class="intent-avatar"
          :style="
            intent.vendorPhotoUrl
              ? { backgroundImage: `url('${intent.vendorPhotoUrl}')` }
              : {}
          "
        >
          {{ !intent.vendorPhotoUrl ? intent.vendorName[0] : "" }}
        </div>
        <div class="intent-text">
          <span class="intent-label">{{ t("auth.login.aboutToContact") }}</span>
          <span class="intent-name">{{ intent.vendorName }}</span>
          <span class="intent-meta">
            {{ intent.vendorCategory?.replace(/_/g, " ") }} ·
            {{ intent.vendorCity }}
          </span>
        </div>
        <span class="intent-badge">{{ t("auth.login.enquirySaved") }}</span>
      </div>

      <h1 class="auth-title">
        {{ intent ? t("auth.login.titleWithIntent") : t("auth.login.title") }}
      </h1>
      <p class="auth-subtitle">
        {{
          intent ? t("auth.login.subtitleWithIntent") : t("auth.login.subtitle")
        }}
      </p>

      <form @submit.prevent="handleLogin" class="auth-form">
        <div class="field">
          <label for="email">{{ t("auth.login.email") }}</label>
          <input
            id="email"
            v-model="email"
            type="email"
            :placeholder="t('auth.login.emailPlaceholder')"
            autocomplete="email"
            required
          />
        </div>

        <div class="field">
          <label for="password">{{ t("auth.login.password") }}</label>
          <div class="password-wrapper">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="••••••••"
              autocomplete="current-password"
              required
            />
            <button
              type="button"
              class="toggle-password"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? t("auth.login.hide") : t("auth.login.show") }}
            </button>
          </div>
        </div>

        <p v-if="error" class="error-msg">{{ error }}</p>

        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? t("common.loading") : t("auth.login.submit") }}
        </button>
      </form>

      <div class="auth-links">
        <RouterLink to="/reset-password">{{
          t("auth.login.forgotPassword")
        }}</RouterLink>
        <span>·</span>
        <RouterLink :to="registerLink"
          >{{ t("auth.login.noAccount") }}
          {{ t("auth.login.register") }}</RouterLink
        >
      </div>
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
  max-width: 420px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
}
.auth-logo {
  text-decoration: none;
  cursor: pointer;
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 24px;
  letter-spacing: -0.5px;
}

/* Intent banner */
.intent-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #1c1c1c;
  border-left: 3px solid var(--color-gold);
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 24px;
  position: relative;
}
.intent-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 1.1rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}
.intent-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.intent-label {
  font-size: 0.7rem;
  color: #aaa;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.intent-name {
  font-size: 0.9rem;
  font-weight: 700;
  color: #fff;
}
.intent-meta {
  font-size: 0.75rem;
  color: #888;
  text-transform: capitalize;
}
.intent-badge {
  position: absolute;
  top: -8px;
  right: 12px;
  background: var(--color-gold);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 10px;
}

.auth-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 4px;
  color: var(--color-text);
}
.auth-subtitle {
  color: var(--color-muted);
  margin: 0 0 32px;
  font-size: 0.9rem;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
}
.field input {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.95rem;
  transition: border-color 0.2s;
  width: 100%;
  box-sizing: border-box;
}
.field input:focus {
  outline: none;
  border-color: var(--color-gold);
}
.password-wrapper {
  position: relative;
}
.toggle-password {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.8rem;
  color: var(--color-muted);
}
.error-msg {
  color: var(--color-error);
  font-size: 0.875rem;
  margin: 0;
}
.btn-primary {
  padding: 12px;
  background: var(--color-gold);
  color: var(--color-white);
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-links {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 8px;
  justify-content: center;
  margin-top: 24px;
  font-size: 0.875rem;
  color: var(--color-muted);
  white-space: nowrap;
}
.auth-links a {
  color: var(--color-gold);
  text-decoration: none;
}
</style>
