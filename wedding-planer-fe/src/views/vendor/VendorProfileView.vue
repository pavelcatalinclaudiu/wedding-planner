<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useVendorStore } from "@/stores/vendor.store";
import { useFileUpload } from "@/composables/useFileUpload";
import { Camera } from "lucide-vue-next";
import type { VendorCategory } from "@/types/vendor.types";

const vendorStore = useVendorStore();
const { upload } = useFileUpload();
const { t } = useI18n();
const saving = ref(false);
const saved = ref(false);

const form = ref({
  businessName: "",
  category: undefined as VendorCategory | undefined,
  city: "",
  description: "",
  basePrice: 0,
  website: "",
  instagram: "",
  facebook: "",
});

const categories = [
  "PHOTOGRAPHER",
  "VIDEOGRAPHER",
  "FLORIST",
  "CATERER",
  "VENUE",
  "BAND",
  "DJ",
  "CAKE",
  "MAKEUP_ARTIST",
  "HAIR_STYLIST",
  "OFFICIANT",
  "PLANNER",
  "TRANSPORTATION",
  "LIGHTING",
  "INVITATION_STATIONERY",
  "JEWELRY",
  "OTHER",
];

onMounted(async () => {
  await vendorStore.fetchMyProfile();
  const p = vendorStore.profile;
  if (p) {
    form.value.businessName = p.businessName ?? "";
    form.value.category = p.category ?? "";
    form.value.city = p.city ?? "";
    form.value.description = p.description ?? "";
    form.value.basePrice = p.basePrice ?? 0;
    form.value.website = p.website ?? "";
    form.value.instagram = p.instagram ?? "";
    form.value.facebook = p.facebook ?? "";
  }
});

async function save() {
  saving.value = true;
  try {
    await vendorStore.updateMyProfile(form.value);
    saved.value = true;
    setTimeout(() => (saved.value = false), 2000);
  } finally {
    saving.value = false;
  }
}

async function uploadCover(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (!files?.length) return;
  await upload("/vendors/me/cover", Array.from(files));
  await vendorStore.fetchMyProfile();
}
</script>

<template>
  <div class="profile-view">
    <div class="pv-header">
      <h2 class="pv-title">{{ t("vendor.profile.vendorTitle") }}</h2>
      <p class="pv-sub">{{ t("vendor.profile.subtitle") }}</p>
    </div>

    <div class="form-card">
      <!-- Cover photo section -->
      <p class="section-label">{{ t("vendor.profile.coverSection") }}</p>
      <div class="cover-section">
        <div class="cover-img-wrap">
          <img
            v-if="vendorStore.profile?.coverPhoto"
            :src="vendorStore.profile.coverPhoto"
            class="cover-img"
            alt="Cover"
          />
          <div v-else class="cover-placeholder">
            {{ t("vendor.profile.noCover") }}
          </div>
        </div>
        <div class="cover-actions">
          <label class="btn-upload">
            <Camera :size="14" />
            {{ t("vendor.profile.changeCover") }}
            <input
              type="file"
              accept="image/*"
              style="display: none"
              @change="uploadCover"
            />
          </label>
        </div>
      </div>

      <p class="section-label" style="margin-top: 24px">
        {{ t("vendor.profile.businessName") }}
      </p>
      <div class="form-grid">
        <div class="field">
          <label>{{ t("vendor.profile.businessName") }}</label>
          <input v-model="form.businessName" class="input" />
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.category") }}</label>
          <select v-model="form.category" class="input">
            <option v-for="c in categories" :key="c" :value="c">
              {{ c.replace("_", " ") }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.city") }}</label>
          <input v-model="form.city" class="input" />
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.basePrice") }}</label>
          <input v-model.number="form.basePrice" type="number" class="input" />
        </div>
        <div class="field full">
          <label>{{ t("vendor.profile.description") }}</label>
          <textarea
            v-model="form.description"
            class="input textarea"
            rows="5"
          />
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.website") }}</label>
          <input v-model="form.website" class="input" placeholder="https://" />
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.instagram") }}</label>
          <input v-model="form.instagram" class="input" placeholder="@handle" />
        </div>
        <div class="field">
          <label>{{ t("vendor.profile.facebook") }}</label>
          <input v-model="form.facebook" class="input" placeholder="Page URL" />
        </div>
      </div>

      <div class="form-actions">
        <button class="save-btn" :disabled="saving" @click="save">
          {{
            saving
              ? t("vendor.profile.saving")
              : saved
                ? t("vendor.profile.saved")
                : t("vendor.profile.save")
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

/* Cover photo */
.cover-section {
  margin-bottom: 8px;
}
.cover-img-wrap {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  margin-bottom: 12px;
}
.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--color-muted);
  font-size: 0.9rem;
}
.cover-actions {
  display: flex;
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
.btn-upload:hover {
  opacity: 0.88;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-bottom: 24px;
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
.field.full {
  grid-column: 1 / -1;
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
.textarea {
  resize: vertical;
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
