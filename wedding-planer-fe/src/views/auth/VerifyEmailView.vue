<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";

const route = useRoute();
const authStore = useAuthStore();
const { t } = useI18n();

const status = ref<"loading" | "success" | "error">("loading");
const error = ref("");

onMounted(async () => {
  const token = route.query.token as string;
  if (!token) {
    status.value = "error";
    error.value = t("auth.verifyEmail.noToken");
    return;
  }
  try {
    await authStore.verifyEmail(token);
    status.value = "success";
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } };
    status.value = "error";
    error.value =
      err.response?.data?.message ?? t("auth.verifyEmail.verificationFailed");
  }
});

async function resend() {
  if (!authStore.user?.email) return;
  try {
    await import("@/api/auth.api").then(({ authApi }) =>
      authApi.resendVerification(authStore.user!.email),
    );
  } catch {
    // silent
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <RouterLink to="/" class="auth-logo">Eternelle</RouterLink>

      <div v-if="status === 'loading'" class="state-msg">
        <div class="spinner" />
        <p>{{ t("auth.verifyEmail.verifying") }}</p>
      </div>

      <div v-else-if="status === 'success'" class="state-msg success">
        <span class="icon">✓</span>
        <h2>{{ t("auth.verifyEmail.verified") }}</h2>
        <p>{{ t("auth.verifyEmail.active") }}</p>
        <RouterLink to="/login" class="btn-primary">{{
          t("auth.verifyEmail.goToLogin")
        }}</RouterLink>
      </div>

      <div v-else class="state-msg error">
        <span class="icon">✕</span>
        <h2>{{ t("auth.verifyEmail.failed") }}</h2>
        <p>{{ error }}</p>
        <button class="btn-secondary" @click="resend">
          {{ t("auth.verifyEmail.resendBtn") }}
        </button>
        <RouterLink to="/login" class="btn-link">Back to login</RouterLink>
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
  max-width: 400px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
  text-align: center;
}
.auth-logo {
  text-decoration: none;
  cursor: pointer;
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 32px;
}
.state-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.icon {
  font-size: 3rem;
}
.success .icon {
  color: var(--color-green, #27ae60);
}
.error .icon {
  color: var(--color-error);
}
h2 {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0;
}
p {
  color: var(--color-muted);
  margin: 0;
}
.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.btn-primary {
  padding: 10px 24px;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
}
.btn-secondary {
  padding: 10px 24px;
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-link {
  font-size: 0.875rem;
  color: var(--color-gold);
  text-decoration: none;
}
</style>
