<script setup lang="ts">
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { useAuthStore } from "@/stores/auth.store";
import { vendorApi } from "@/api/vendor.api";
import type { VendorCategory } from "@/types/vendor.types";

const { t } = useI18n();
const router = useRouter();
const authStore = useAuthStore();

const step = ref(1);
const loading = ref(false);
const error = ref("");
const pendingVerification = ref(false);
const showPassword = ref(false);

// Step 1
const email = ref("");
const password = ref("");
const confirmPassword = ref("");

// Step 2
const businessName = ref("");
const category = ref<VendorCategory>("PHOTOGRAPHER");
const city = ref("");
const description = ref("");
const basePrice = ref<number | undefined>(undefined);

const categories = computed<{ value: VendorCategory; label: string }[]>(() => [
  { value: "PHOTOGRAPHER", label: t("categories.PHOTOGRAPHER") },
  { value: "VIDEOGRAPHER", label: t("categories.VIDEOGRAPHER") },
  { value: "VENUE", label: t("categories.VENUE") },
  { value: "CATERER", label: t("categories.CATERER") },
  { value: "FLORIST", label: t("categories.FLORIST") },
  { value: "BAND", label: t("categories.BAND") },
  { value: "DJ", label: t("categories.DJ") },
  { value: "CAKE", label: t("categories.CAKE") },
  { value: "MAKEUP_ARTIST", label: t("categories.MAKEUP_ARTIST") },
  { value: "HAIR_STYLIST", label: t("categories.HAIR_STYLIST") },
  { value: "OFFICIANT", label: t("categories.OFFICIANT") },
  { value: "PLANNER", label: t("categories.PLANNER") },
  { value: "TRANSPORTATION", label: t("categories.TRANSPORTATION") },
  { value: "LIGHTING", label: t("categories.LIGHTING") },
  {
    value: "INVITATION_STATIONERY",
    label: t("categories.INVITATION_STATIONERY"),
  },
  { value: "JEWELRY", label: t("categories.JEWELRY") },
  { value: "OTHER", label: t("categories.OTHER") },
]);

function nextStep() {
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
  if (!businessName.value || !city.value) {
    error.value = "Business name and city are required.";
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    await authStore.register({
      email: email.value,
      password: password.value,
      role: "VENDOR",
    });
    await vendorApi.createProfile({
      businessName: businessName.value,
      category: category.value,
      city: city.value,
      description: description.value,
      basePrice: basePrice.value,
    });
    // Profile created — log out so the user must verify before accessing the app
    authStore.logout();
    pendingVerification.value = true;
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } };
    error.value =
      err.response?.data?.message ?? t("auth.registerVendor.errorFailed");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-logo">Eternelle</div>

      <!-- ✅ Email verification pending -->
      <div v-if="pendingVerification" class="pending-verification">
        <div class="pv-icon">✉️</div>
        <h2 class="pv-title">{{ t("auth.checkEmail.title") }}</h2>
        <p class="pv-sub">{{ t("auth.checkEmail.subtitle", { email }) }}</p>
        <p class="pv-note">{{ t("auth.checkEmail.note") }}</p>
        <RouterLink to="/login" class="btn-primary">{{ t("auth.checkEmail.goToLogin") }}</RouterLink>
      </div>

      <template v-else>
        <div class="step-indicator">
          <span :class="{ active: step >= 1 }">1</span>
          <span class="sep">—</span>
          <span :class="{ active: step >= 2 }">2</span>
        </div>

        <template v-if="step === 1">
        <h1 class="auth-title">{{ t("auth.registerVendor.title") }}</h1>
        <form @submit.prevent="nextStep" class="auth-form">
          <div class="field">
            <label>{{ t("auth.registerVendor.emailLabel") }}</label>
            <input
              v-model="email"
              type="email"
              placeholder="you@business.com"
              required
            />
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.passwordLabel") }}</label>
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
                {{ showPassword ? t("auth.login.hide") : t("auth.login.show") }}
              </button>
            </div>
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.confirmPasswordLabel") }}</label>
            <input
              v-model="confirmPassword"
              :type="showPassword ? 'text' : 'password'"
              placeholder="••••••••"
              required
            />
          </div>
          <p v-if="error" class="error-msg">{{ error }}</p>
          <button type="submit" class="btn-primary">
            {{ t("auth.registerVendor.next") }}
          </button>
        </form>
      </template>

      <template v-else>
        <h1 class="auth-title">{{ t("auth.registerVendor.stepBusiness") }}</h1>
        <form @submit.prevent="handleSubmit" class="auth-form">
          <div class="field">
            <label>{{ t("auth.registerVendor.businessNameLabel") }}</label>
            <input
              v-model="businessName"
              type="text"
              placeholder="Studio Bloom"
              required
            />
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.categoryLabel") }}</label>
            <select v-model="category">
              <option v-for="c in categories" :key="c.value" :value="c.value">
                {{ c.label }}
              </option>
            </select>
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.cityLabel") }}</label>
            <input
              v-model="city"
              type="text"
              placeholder="București"
              required
            />
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.descriptionLabel") }}</label>
            <textarea
              v-model="description"
              rows="3"
              :placeholder="t('vendor.profile.description')"
            />
          </div>
          <div class="field">
            <label>{{ t("auth.registerVendor.basePriceLabel") }}</label>
            <input
              v-model.number="basePrice"
              type="number"
              min="0"
              placeholder="500"
            />
          </div>
          <p v-if="error" class="error-msg">{{ error }}</p>
          <div class="btn-row">
            <button type="button" class="btn-secondary" @click="step = 1">
              {{ t("auth.registerVendor.back") }}
            </button>
            <button type="submit" class="btn-primary" :disabled="loading">
              {{
                loading ? t("common.loading") : t("auth.registerVendor.submit")
              }}
            </button>
          </div>
        </form>
      </template>

      <p class="auth-links">
        {{ t("auth.registerVendor.alreadyHaveAccount") }}
        <RouterLink to="/login">{{
          t("auth.registerVendor.signIn")
        }}</RouterLink>
      </p>
      </template>
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
  max-width: 440px;
  box-shadow: 0 4px 32px rgba(0, 0, 0, 0.08);
}
.auth-logo {
  font-size: 2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 16px;
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
.field input,
.field select,
.field textarea {
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box;
  font-family: inherit;
}
.field input:focus,
.field select:focus,
.field textarea:focus {
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
</style>
