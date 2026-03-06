<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useCoupleStore } from "@/stores/couple.store";

const { t } = useI18n();
const coupleStore = useCoupleStore();
const saving = ref(false);
const saved = ref(false);
const error = ref("");

const form = ref({
  partner1Name: "",
  partner2Name: "",
  weddingDate: "",
  weddingLocation: "",
  estimatedGuestCount: 0,
  totalBudget: 0,
});

onMounted(async () => {
  await coupleStore.fetchProfile();
  const p = coupleStore.profile;
  if (p) {
    form.value.partner1Name = p.partner1Name ?? "";
    form.value.partner2Name = p.partner2Name ?? "";
    form.value.weddingDate = p.weddingDate ?? "";
    form.value.weddingLocation = p.weddingLocation ?? "";
    form.value.estimatedGuestCount = p.estimatedGuestCount ?? 0;
    form.value.totalBudget = p.totalBudget ?? 0;
  }
});

async function save() {
  saving.value = true;
  error.value = "";
  try {
    await coupleStore.updateProfile(form.value);
    saved.value = true;
    setTimeout(() => (saved.value = false), 2500);
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? t("profile.couple.saveError");
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="profile-view">
    <div class="pv-header">
      <h2 class="pv-title">{{ t("profile.couple.title") }}</h2>
      <p class="pv-sub">
        {{ t("profile.couple.subtitle") }}
      </p>
    </div>

    <div class="form-card">
      <p class="section-label">{{ t("profile.couple.partnersSection") }}</p>
      <div class="form-grid">
        <div class="field">
          <label>{{ t("profile.couple.partner1") }}</label>
          <input
            v-model="form.partner1Name"
            class="input"
            placeholder="e.g. Ana"
          />
        </div>
        <div class="field">
          <label>{{ t("profile.couple.partner2") }}</label>
          <input
            v-model="form.partner2Name"
            class="input"
            placeholder="e.g. Mihai"
          />
        </div>
      </div>

      <p class="section-label" style="margin-top: 24px">
        {{ t("profile.couple.weddingDetailsSection") }}
      </p>
      <div class="form-grid">
        <div class="field">
          <label>{{ t("profile.couple.weddingDate") }}</label>
          <input v-model="form.weddingDate" type="date" class="input" />
        </div>
        <div class="field">
          <label>{{ t("profile.couple.location") }}</label>
          <input
            v-model="form.weddingLocation"
            class="input"
            :placeholder="t('profile.couple.locationPlaceholder')"
          />
        </div>
        <div class="field">
          <label>{{ t("profile.couple.guestCount") }}</label>
          <input
            v-model.number="form.estimatedGuestCount"
            type="number"
            min="0"
            class="input"
          />
        </div>
        <div class="field">
          <label>{{ t("profile.couple.budget") }}</label>
          <input
            v-model.number="form.totalBudget"
            type="number"
            min="0"
            step="100"
            class="input"
          />
        </div>
      </div>

      <p v-if="error" class="form-error">{{ error }}</p>

      <div class="form-actions">
        <button class="save-btn" :disabled="saving" @click="save">
          {{
            saving
              ? t("profile.couple.saving")
              : saved
                ? t("profile.couple.savedMsg")
                : t("profile.couple.saveChanges")
          }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pv-header {
  margin-bottom: 24px;
}
.pv-title {
  margin: 0 0 4px;
  font-size: 1.4rem;
  font-weight: 700;
}
.pv-sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}

.form-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 28px;
}

.section-label {
  font-size: 0.78rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-muted);
  margin: 0 0 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

@media (max-width: 600px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

label {
  font-size: 0.84rem;
  font-weight: 600;
  color: var(--color-text);
}

.input {
  padding: 9px 13px;
  border: 1.5px solid var(--color-border);
  border-radius: 7px;
  font-size: 0.9rem;
  font-family: inherit;
  background: var(--color-surface);
  color: var(--color-text);
  outline: none;
  transition: border-color 0.15s;
}
.input:focus {
  border-color: var(--color-gold);
}

.form-error {
  margin: 16px 0 0;
  color: var(--color-error);
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  margin-top: 24px;
}

.save-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 28px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s;
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: default;
}
.save-btn:not(:disabled):hover {
  opacity: 0.88;
}
</style>
