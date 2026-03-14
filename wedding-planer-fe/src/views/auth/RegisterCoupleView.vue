<script setup lang="ts">
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { coupleApi } from "@/api/couple.api";
import { extractApiError } from "@/api/client";
import { useAuthRedirect } from "@/composables/useAuthRedirect";
import { Mail } from "lucide-vue-next";

const { t } = useI18n();
const route = useRoute();
const authStore = useAuthStore();
const { getEnquiryIntent } = useAuthRedirect();

const step = ref(1);
const loading = ref(false);
const error = ref("");
const pendingVerification = ref(false);

// Step 1
const email = ref("");
const password = ref("");
const confirmPassword = ref("");
const showPassword = ref(false);

// Step 2
const partner1Name = ref("");
const partner2Name = ref("");
const knowsWeddingDate = ref<boolean | null>(null);
const weddingDate = ref("");
const weddingLocation = ref("");
const estimatedGuestCount = ref<number>(100);
const totalBudget = ref<number>(10000);

const intent = computed(() =>
  route.query.intent === "enquiry" ? getEnquiryIntent() : null,
);

async function nextStep() {
  error.value = "";
  if (!email.value || !password.value || !confirmPassword.value) {
    error.value = "Please fill in all fields.";
    return;
  }
  if (password.value !== confirmPassword.value) {
    error.value = "Passwords do not match.";
    return;
  }
  step.value = 2;
}

async function handleSubmit() {
  if (!partner1Name.value || !partner2Name.value) {
    error.value = t("auth.registerCouple.errorFillFields");
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    await authStore.register({
      email: email.value,
      password: password.value,
      role: "COUPLE",
    });
    await coupleApi.createProfile({
      partner1Name: partner1Name.value,
      partner2Name: partner2Name.value,
      weddingDate: knowsWeddingDate.value === true ? weddingDate.value : "",
      weddingLocation:
        knowsWeddingDate.value === true ? weddingLocation.value : "",
      estimatedGuestCount: estimatedGuestCount.value,
      totalBudget: totalBudget.value,
    });
    // Profile created — log out so the user must verify before accessing the app
    authStore.logout();
    pendingVerification.value = true;
  } catch (e: unknown) {
    error.value = extractApiError(e, t("auth.registerCouple.errorFailed"));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div v-if="step !== 2" class="auth-back">
      <RouterLink to="/">← Back to home</RouterLink>
    </div>
    <div class="auth-card">
      <RouterLink to="/" class="auth-logo">Eternelle</RouterLink>

      <!-- ✅ Email verification pending -->
      <div v-if="pendingVerification" class="pending-verification">
        <div class="pv-icon"><Mail :size="40" /></div>
        <h2 class="pv-title">{{ t("auth.checkEmail.title") }}</h2>
        <p class="pv-sub">{{ t("auth.checkEmail.subtitle", { email }) }}</p>
        <p class="pv-note">{{ t("auth.checkEmail.note") }}</p>
        <RouterLink to="/login" class="btn-primary">{{
          t("auth.checkEmail.goToLogin")
        }}</RouterLink>
      </div>

      <template v-else>
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
            <span class="intent-label">{{
              t("auth.login.aboutToContact")
            }}</span>
            <span class="intent-name">{{ intent.vendorName }}</span>
            <span class="intent-meta">
              {{
                intent.vendorCategory
                  ? t(`categories.${intent.vendorCategory}`)
                  : ""
              }}
              ·
              {{ intent.vendorCity }}
            </span>
          </div>
          <span class="intent-badge">{{ t("auth.login.enquirySaved") }}</span>
        </div>

        <div class="step-indicator">
          <span :class="{ active: step >= 1 }">1</span>
          <span class="sep">—</span>
          <span :class="{ active: step >= 2 }">2</span>
        </div>

        <!-- Step 1 -->
        <template v-if="step === 1">
          <h1 class="auth-title">{{ t("auth.registerCouple.title") }}</h1>
          <form @submit.prevent="nextStep" class="auth-form">
            <div class="field">
              <label>{{ t("auth.registerCouple.emailLabel") }}</label>
              <input
                v-model="email"
                type="email"
                placeholder="you@example.com"
                required
              />
            </div>
            <div class="field">
              <label>{{ t("auth.registerCouple.passwordLabel") }}</label>
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
              <label>{{ t("auth.registerCouple.confirmPasswordLabel") }}</label>
              <input
                v-model="confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                placeholder="••••••••"
                required
              />
            </div>
            <p v-if="error" class="error-msg">{{ error }}</p>
            <button type="submit" class="btn-primary">
              {{ t("auth.registerCouple.next") }}
            </button>
          </form>
        </template>

        <!-- Step 2 -->
        <template v-else>
          <h1 class="auth-title">{{ t("auth.registerCouple.stepProfile") }}</h1>
          <form @submit.prevent="handleSubmit" class="auth-form">
            <div class="field">
              <label>{{ t("auth.registerCouple.partner1Label") }}</label>
              <input
                v-model="partner1Name"
                type="text"
                placeholder="Alex"
                required
              />
            </div>
            <div class="field">
              <label>{{ t("auth.registerCouple.partner2Label") }}</label>
              <input
                v-model="partner2Name"
                type="text"
                placeholder="Jordan"
                required
              />
            </div>
            <!-- Do you know your wedding date? -->
            <div class="field knows-date-field">
              <p class="knows-date-label">
                {{ t("auth.registerCouple.knowsDateQuestion") }}
              </p>
              <div class="knows-date-options">
                <button
                  type="button"
                  class="knows-date-btn"
                  :class="{ active: knowsWeddingDate === true }"
                  @click="knowsWeddingDate = true"
                >
                  {{ t("auth.registerCouple.knowsDateYes") }}
                </button>
                <button
                  type="button"
                  class="knows-date-btn"
                  :class="{ active: knowsWeddingDate === false }"
                  @click="knowsWeddingDate = false"
                >
                  {{ t("auth.registerCouple.knowsDateNo") }}
                </button>
              </div>
            </div>

            <template v-if="knowsWeddingDate === true">
              <div class="field">
                <label>{{ t("auth.registerCouple.weddingDateLabel") }}</label>
                <input v-model="weddingDate" type="date" />
              </div>
              <div class="field">
                <label>{{
                  t("auth.registerCouple.weddingLocationLabel")
                }}</label>
                <input
                  v-model="weddingLocation"
                  type="text"
                  placeholder="Oraș, Țară"
                />
              </div>
            </template>
            <div class="field-row">
              <div class="field">
                <label>{{ t("auth.registerCouple.guestCountLabel") }}</label>
                <input
                  v-model.number="estimatedGuestCount"
                  type="number"
                  min="1"
                />
              </div>
              <div class="field">
                <label>{{ t("auth.registerCouple.budgetLabel") }}</label>
                <input v-model.number="totalBudget" type="number" min="0" />
              </div>
            </div>
            <p v-if="error" class="error-msg">{{ error }}</p>
            <div class="btn-row">
              <button type="button" class="btn-secondary" @click="step = 1">
                {{ t("auth.registerCouple.back") }}
              </button>
              <button type="submit" class="btn-primary" :disabled="loading">
                {{
                  loading
                    ? t("common.loading")
                    : t("auth.registerCouple.submit")
                }}
              </button>
            </div>
          </form>
        </template>

        <p class="auth-links">
          {{ t("auth.registerCouple.alreadyHaveAccount") }}
          <RouterLink to="/login">{{
            t("auth.registerCouple.signIn")
          }}</RouterLink>
        </p>
      </template>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  position: relative;
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
  max-width: 440px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
}
.auth-logo {
  text-decoration: none;
  cursor: pointer;
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 16px;
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
  margin-bottom: 20px;
  position: relative;
}
.intent-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 1rem;
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
  font-size: 0.875rem;
  font-weight: 700;
  color: #fff;
}
.intent-meta {
  font-size: 0.72rem;
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
.step-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.875rem;
  color: var(--color-muted);
  margin-bottom: 24px;
}
.step-indicator span.active {
  background: var(--color-gold);
  color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.auth-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin: 0 0 28px;
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
  color: var(--color-text);
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
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
/* Yes / No wedding-date toggle */
.knows-date-field {
  gap: 10px;
}
.knows-date-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}
.knows-date-options {
  display: flex;
  gap: 12px;
}
.knows-date-btn {
  flex: 1;
  padding: 10px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  background: transparent;
  color: var(--color-text);
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition:
    border-color 0.18s,
    background 0.18s,
    color 0.18s;
}
.knows-date-btn:hover:not(.active) {
  border-color: var(--color-gold);
}
.knows-date-btn.active {
  border-color: var(--color-gold);
  background: var(--color-gold);
  color: #fff;
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
.btn-row {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
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
.btn-secondary {
  padding: 12px;
  background: var(--color-surface);
  color: var(--color-text);
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.auth-links {
  margin-top: 24px;
  font-size: 0.875rem;
  color: var(--color-muted);
  text-align: center;
}
.auth-links a {
  color: var(--color-gold);
  text-decoration: none;
}
.pending-verification {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  text-align: center;
}
.pv-icon {
  font-size: 3rem;
}
.pv-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}
.pv-sub {
  font-size: 0.95rem;
  color: var(--color-muted);
  margin: 0;
}
.pv-note {
  font-size: 0.85rem;
  color: var(--color-muted);
  margin: 0;
}
.auth-back {
  position: absolute;
  top: 24px;
  left: 24px;
  font-size: 0.875rem;
  color: var(--color-gold);
  padding: 8px 8px;
  border-radius: 8px;
  transition:
    background 0.2s,
    color 0.2s;
  border: 1px solid var(--color-gold);
}
.auth-back a,
.back-btn {
  color: var(--color-gold);
  text-decoration: none;
  cursor: pointer;
}
.auth-back a:hover,
.back-btn:hover {
  color: var(--color-gold);
}

@media (max-width: 600px) {
  .auth-card {
    background: none;
    box-shadow: none;
    padding: 24px;
  }
}
</style>
