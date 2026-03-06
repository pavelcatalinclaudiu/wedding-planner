<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();

const token = computed(() => route.query.token as string | undefined);
const isResetMode = computed(() => !!token.value);

const email = ref("");
const password = ref("");
const confirmPassword = ref("");
const loading = ref(false);
const success = ref(false);
const error = ref("");
const showPassword = ref(false);

async function requestReset() {
  if (!email.value) return;
  loading.value = true;
  error.value = "";
  try {
    await authStore.requestPasswordReset(email.value);
    success.value = true;
  } catch {
    error.value = t("auth.resetPassword.errorRequest");
  } finally {
    loading.value = false;
  }
}

async function setNewPassword() {
  if (!password.value || !confirmPassword.value) {
    error.value = t("auth.resetPassword.errorFillFields");
    return;
  }
  if (password.value !== confirmPassword.value) {
    error.value = t("auth.resetPassword.errorMismatch");
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    await authStore.resetPassword(token.value!, password.value);
    success.value = true;
  } catch {
    error.value = t("auth.resetPassword.errorExpired");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">Eternelle</div>

      <!-- Request mode -->
      <template v-if="!isResetMode">
        <h1 class="auth-title">{{ t("auth.resetPassword.titleRequest") }}</h1>
        <template v-if="!success">
          <p class="auth-subtitle">
            {{ t("auth.resetPassword.subtitleRequest") }}
          </p>
          <form @submit.prevent="requestReset" class="auth-form">
            <div class="field">
              <label>{{ t("auth.resetPassword.emailLabel") }}</label>
              <input
                v-model="email"
                type="email"
                placeholder="you@example.com"
                required
              />
            </div>
            <p v-if="error" class="error-msg">{{ error }}</p>
            <button type="submit" class="btn-primary" :disabled="loading">
              {{
                loading
                  ? t("common.loading")
                  : t("auth.resetPassword.submitRequest")
              }}
            </button>
          </form>
        </template>
        <div v-else class="success-state">
          <span class="icon">✉️</span>
          <p>{{ t("auth.resetPassword.successRequest") }}</p>
        </div>
      </template>

      <!-- Set new password mode -->
      <template v-else>
        <h1 class="auth-title">{{ t("auth.resetPassword.titleReset") }}</h1>
        <template v-if="!success">
          <form @submit.prevent="setNewPassword" class="auth-form">
            <div class="field">
              <label>{{ t("auth.resetPassword.passwordLabel") }}</label>
              <div class="password-wrapper">
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="Min. 8 characters"
                  required
                />
                <button
                  type="button"
                  class="toggle-password"
                  @click="showPassword = !showPassword"
                >
                  {{
                    showPassword ? t("auth.login.hide") : t("auth.login.show")
                  }}
                </button>
              </div>
            </div>
            <div class="field">
              <label>{{ t("auth.resetPassword.confirmPasswordLabel") }}</label>
              <input
                v-model="confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                required
              />
            </div>
            <p v-if="error" class="error-msg">{{ error }}</p>
            <button type="submit" class="btn-primary" :disabled="loading">
              {{
                loading
                  ? t("common.loading")
                  : t("auth.resetPassword.submitReset")
              }}
            </button>
          </form>
        </template>
        <div v-else class="success-state">
          <span class="icon">✓</span>
          <p>
            {{ t("auth.resetPassword.successReset") }}
            <RouterLink to="/login">{{ t("common.signIn") }}</RouterLink>
          </p>
        </div>
      </template>

      <p class="auth-links">
        <RouterLink to="/login"
          >← {{ t("auth.resetPassword.backToLogin") }}</RouterLink
        >
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
  max-width: 420px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
}
.auth-logo {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 24px;
}
.auth-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 8px;
}
.auth-subtitle {
  color: var(--color-muted);
  margin: 0 0 28px;
  font-size: 0.9rem;
}
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field label {
  font-size: 0.875rem;
  font-weight: 600;
}
.field input {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.95rem;
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
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.success-state {
  text-align: center;
  padding: 24px 0;
}
.icon {
  font-size: 2.5rem;
  display: block;
  margin-bottom: 16px;
}
.auth-links {
  margin-top: 28px;
  font-size: 0.875rem;
  text-align: center;
}
.auth-links a {
  color: var(--color-gold);
  text-decoration: none;
}
</style>
