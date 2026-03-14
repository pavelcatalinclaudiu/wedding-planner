<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { useVendorStore } from "@/stores/vendor.store";
import { vendorApi } from "@/api/vendor.api";
import { useFileUpload } from "@/composables/useFileUpload";

const { t } = useI18n();

const vendorStore = useVendorStore();
const { upload, uploading, progress } = useFileUpload();
const fileInputRef = ref<HTMLInputElement | null>(null);

const photos = computed(() => vendorStore.profile?.photos ?? []);

async function handleUpload(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (!files?.length) return;
  await upload("/vendors/me/photos", Array.from(files));
  await vendorStore.fetchMyProfile();
}

async function deletePhoto(photoId: string) {
  await vendorApi.deletePhoto(photoId);
  await vendorStore.fetchMyProfile();
}

onMounted(() => vendorStore.fetchMyProfile());
</script>

<template>
  <div class="portfolio-view">
    <div class="port-header">
      <div>
        <h2>{{ t("vendor.portfolio.title") }}</h2>
        <p class="page-sub">{{ t("vendor.portfolio.subtitle") }}</p>
      </div>
      <button class="upload-btn" @click="fileInputRef?.click()">
        + {{ t("vendor.portfolio.addPhoto") }}
      </button>
      <input
        ref="fileInputRef"
        type="file"
        accept="image/*"
        multiple
        hidden
        @change="handleUpload"
      />
    </div>

    <div v-if="uploading" class="upload-progress">
      {{ t("vendor.portfolio.uploading") }} {{ progress }}%
    </div>

    <div v-if="photos.length === 0" class="empty">
      {{ t("vendor.portfolio.noPhotos") }}
    </div>
    <div v-else class="photo-grid">
      <div v-for="photo in photos" :key="photo.id" class="photo-item">
        <img :src="photo.url" :alt="photo.caption ?? ''" class="photo-img" />
        <div class="photo-overlay">
          <button class="delete-btn" @click="deletePhoto(photo.id)">×</button>
        </div>
        <p v-if="photo.caption" class="photo-caption">{{ photo.caption }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.port-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.page-sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}
.upload-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 18px;
  font-weight: 700;
  cursor: pointer;
}
.upload-progress {
  background: var(--color-gold-light);
  border: 1px solid var(--color-gold);
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 0.9rem;
  color: var(--color-gold);
  font-weight: 600;
  margin-bottom: 16px;
}
.empty {
  color: var(--color-muted);
  text-align: center;
  padding: 48px;
}
.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}
.photo-item {
  position: relative;
  border-radius: 10px;
  overflow: hidden;
  background: var(--color-surface);
}
.photo-img {
  width: 100%;
  aspect-ratio: 4/3;
  object-fit: cover;
  display: block;
}
.photo-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
.photo-item:hover .photo-overlay {
  opacity: 1;
}
.delete-btn {
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  border-radius: 50%;
  width: 26px;
  height: 26px;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.photo-caption {
  margin: 0;
  padding: 6px 10px;
  font-size: 0.78rem;
  color: var(--color-muted);
}
</style>
