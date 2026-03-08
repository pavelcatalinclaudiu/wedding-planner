<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useCoupleStore } from "@/stores/couple.store";
import { Camera, Trash2 } from "lucide-vue-next";

const { t } = useI18n();
const coupleStore = useCoupleStore();
const saving = ref(false);
const saved = ref(false);
const error = ref("");
const pictureUploading = ref(false);
const pictureError = ref("");
const fileInput = ref<HTMLInputElement | null>(null);

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

async function onFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0];
  if (!file) return;
  pictureUploading.value = true;
  pictureError.value = "";
  try {
    await coupleStore.uploadPicture(file);
  } catch (err: any) {
    pictureError.value =
      err?.response?.data?.message ?? t("profile.couple.pictureError");
  } finally {
    pictureUploading.value = false;
    if (fileInput.value) fileInput.value.value = "";
  }
}

async function removePicture() {
  pictureUploading.value = true;
  pictureError.value = "";
  try {
    await coupleStore.deletePicture();
  } catch (err: any) {
    pictureError.value =
      err?.response?.data?.message ?? t("profile.couple.pictureError");
  } finally {
    pictureUploading.value = false;
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
      <!-- Avatar section -->
      <p class="section-label">{{ t("profile.couple.photoSection") }}</p>
      <div class="avatar-section">
        <div class="avatar-wrap">
          <img
            v-if="coupleStore.profile?.profilePicture"
            :src="coupleStore.profile.profilePicture"
            class="avatar-img"
            alt="Profile picture"
          />
          <div v-else class="avatar-placeholder">
            <Camera :size="28" />
          </div>
          <div v-if="pictureUploading" class="avatar-overlay">…</div>
        </div>
        <div class="avatar-actions">
          <label class="btn-upload" :class="{ disabled: pictureUploading }">
            <Camera :size="14" />
            {{
              coupleStore.profile?.profilePicture
                ? t("profile.couple.changePicture")
                : t("profile.couple.uploadPicture")
            }}
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              style="display: none"
              :disabled="pictureUploading"
              @change="onFileChange"
            />
          </label>
          <button
            v-if="coupleStore.profile?.profilePicture"
            class="btn-remove-pic"
            :disabled="pictureUploading"
            @click="removePicture"
          >
            <Trash2 :size="14" /> {{ t("profile.couple.removePicture") }}
          </button>
        </div>
        <p v-if="pictureError" class="form-error">{{ pictureError }}</p>
      </div>

      <p class="section-label" style="margin-top: 24px">
        {{ t("profile.couple.partnersSection") }}
      </p>
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

/* Avatar */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}
.avatar-wrap {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  width: 100%;
  height: 100%;
  background: var(--color-bg-2, #f0ebe3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted);
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 1.4rem;
}
.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.btn-upload {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--color-gold);
  color: #fff;
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: none;
}
.btn-upload.disabled {
  opacity: 0.6;
  cursor: default;
  pointer-events: none;
}
.btn-upload:not(.disabled):hover {
  opacity: 0.88;
}
.btn-remove-pic {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: transparent;
  color: var(--color-error);
  border: 1px solid var(--color-error);
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-remove-pic:disabled {
  opacity: 0.5;
  cursor: default;
}
.btn-remove-pic:not(:disabled):hover {
  background: var(--color-error-light, #fef2f2);
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
