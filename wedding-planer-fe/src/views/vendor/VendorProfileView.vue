<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useVendorStore } from "@/stores/vendor.store";
import { useFileUpload } from "@/composables/useFileUpload";

const vendorStore = useVendorStore();
const { upload, uploading } = useFileUpload();
const saving = ref(false);
const saved = ref(false);
const coverInputRef = ref<HTMLInputElement | null>(null);

const form = ref({
  businessName: "",
  category: "",
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
    <h2>Vendor Profile</h2>

    <div class="cover-section">
      <div class="cover-img-wrap">
        <img
          v-if="vendorStore.profile?.coverPhoto"
          :src="vendorStore.profile.coverPhoto"
          class="cover-img"
          alt="Cover"
        />
        <div v-else class="cover-placeholder">No cover image</div>
      </div>

      <div class="cover-actions">
        <button class="change-cover-btn" @click="coverInputRef?.click()">
          Change Profile Picture
        </button>
        <input
          ref="coverInputRef"
          type="file"
          accept="image/*"
          hidden
          @change="uploadCover"
        />
      </div>
    </div>

    <div class="form-card">
      <div class="form-grid">
        <div class="field">
          <label>Business Name</label>
          <input v-model="form.businessName" class="input" />
        </div>
        <div class="field">
          <label>Category</label>
          <select v-model="form.category" class="input">
            <option v-for="c in categories" :key="c" :value="c">
              {{ c.replace("_", " ") }}
            </option>
          </select>
        </div>
        <div class="field">
          <label>City</label>
          <input v-model="form.city" class="input" />
        </div>
        <div class="field">
          <label>Base Price (RON)</label>
          <input v-model.number="form.basePrice" type="number" class="input" />
        </div>
        <div class="field full">
          <label>Description</label>
          <textarea
            v-model="form.description"
            class="input textarea"
            rows="5"
          />
        </div>
        <div class="field">
          <label>Website</label>
          <input v-model="form.website" class="input" placeholder="https://" />
        </div>
        <div class="field">
          <label>Instagram</label>
          <input v-model="form.instagram" class="input" placeholder="@handle" />
        </div>
        <div class="field">
          <label>Facebook</label>
          <input v-model="form.facebook" class="input" placeholder="Page URL" />
        </div>
      </div>

      <div class="form-actions">
        <button class="save-btn" :disabled="saving" @click="save">
          {{ saving ? "Saving…" : saved ? "✓ Saved!" : "Save Profile" }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0 0 20px;
  font-size: 1.4rem;
}
.cover-section {
  margin-bottom: 24px;
}
.cover-img-wrap {
  width: 100%;
  height: 180px;
  border-radius: 12px;
  overflow: hidden;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  margin-bottom: 0;
}
.cover-actions {
  margin-top: 10px;
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
.change-cover-btn {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 7px;
  padding: 6px 16px;
  font-size: 0.85rem;
  cursor: pointer;
}
.form-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 28px;
  margin-top: 16px;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin-bottom: 24px;
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
  outline: none;
}
.input:focus {
  border-color: var(--color-gold);
}
.textarea {
  resize: vertical;
}
.form-actions {
  display: flex;
}
.save-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 10px 28px;
  font-weight: 700;
  cursor: pointer;
}
.save-btn:disabled {
  opacity: 0.6;
}
</style>
