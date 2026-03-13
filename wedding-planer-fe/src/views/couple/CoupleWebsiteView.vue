<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import type { WeddingWebsite } from "@/types/couple.types";
import {
  Link,
  Camera,
  Eye,
  ExternalLink,
  Share2,
  Copy,
  Check,
} from "lucide-vue-next";

const { t } = useI18n();
const website = ref<WeddingWebsite | null>(null);
const saving = ref(false);
const saved = ref(false);
const photoUploading = ref(false);
const copied = ref(false);

const form = ref({
  subdomain: "",
  heroMessage: "",
  story: "",
  ceremonyDate: "",
  ceremonyLocation: "",
  receptionDate: "",
  receptionLocation: "",
  published: false,
});

const photoInputEl = ref<HTMLInputElement | null>(null);
const photoPreview = ref<string | null>(null);

onMounted(async () => {
  try {
    const res = await coupleApi.getWebsite();
    website.value = res.data;
    if (website.value) {
      form.value.subdomain = website.value.subdomain ?? "";
      form.value.heroMessage = website.value.heroMessage ?? "";
      form.value.story = website.value.story ?? "";
      form.value.ceremonyDate = website.value.ceremonyDate ?? "";
      form.value.ceremonyLocation = website.value.ceremonyLocation ?? "";
      form.value.receptionDate = website.value.receptionDate ?? "";
      form.value.receptionLocation = website.value.receptionLocation ?? "";
      form.value.published = website.value.published ?? false;
    }
  } catch {
    /* not yet created */
  }
});

async function save() {
  saving.value = true;
  try {
    const res = await coupleApi.updateWebsite(form.value);
    website.value = res.data;
    saved.value = true;
    setTimeout(() => (saved.value = false), 2500);
  } finally {
    saving.value = false;
  }
}

async function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  photoPreview.value = URL.createObjectURL(file);
  photoUploading.value = true;
  try {
    const res = await coupleApi.uploadCoverPhoto(file);
    website.value = res.data;
  } finally {
    photoUploading.value = false;
    input.value = "";
  }
}

const publicUrl = computed(() => {
  const base = window.location.origin;
  const sub = form.value.subdomain || "your-names";
  return `${base}/w/${sub}`;
});

const origin = window.location.origin;

async function copyLink() {
  await navigator.clipboard.writeText(publicUrl.value);
  copied.value = true;
  setTimeout(() => (copied.value = false), 2000);
}

function shareWhatsApp() {
  window.open(
    `https://wa.me/?text=${encodeURIComponent("You're invited to our wedding! " + publicUrl.value)}`,
    "_blank",
  );
}

function shareEmail() {
  window.open(
    `mailto:?subject=You're Invited!&body=Please RSVP here: ${encodeURIComponent(publicUrl.value)}`,
    "_blank",
  );
}

const coverPhoto = computed(
  () => photoPreview.value ?? website.value?.coverPhotoUrl ?? null,
);
</script>

<template>
  <div class="website-view">
    <div class="page-header">
      <div>
        <h2>{{ t("website.title") }}</h2>
        <p class="subtitle">{{ t("website.subtitle") }}</p>
      </div>
      <div class="header-actions">
        <a
          v-if="form.subdomain"
          :href="`${publicUrl}?preview=true`"
          target="_blank"
          class="preview-btn"
        >
          <Eye :size="15" /> {{ t("website.preview") }}
        </a>
        <a
          v-if="form.subdomain && website?.published"
          :href="publicUrl"
          target="_blank"
          class="preview-btn live"
        >
          <ExternalLink :size="15" /> {{ t("website.viewLive") }}
        </a>
      </div>
    </div>

    <!-- Stats strip -->
    <div v-if="website" class="stats-strip">
      <div class="stat">
        <span class="stat-value">{{ website.visitorCount }}</span>
        <span class="stat-label">Page views</span>
      </div>
      <div class="stat">
        <span class="stat-value">{{ website.rsvpsSubmitted }}</span>
        <span class="stat-label">RSVPs received</span>
      </div>
      <div class="stat">
        <span class="stat-value" :class="website.published ? 'live' : 'draft'">
          {{ website.published ? "Live" : "Draft" }}
        </span>
        <span class="stat-label">Status</span>
      </div>
    </div>

    <div class="form-grid">
      <!-- Left column -->
      <div class="col">
        <!-- Cover photo -->
        <div class="card">
          <h3 class="card-title">
            <Camera :size="15" /> {{ t("website.coverPhoto") }}
          </h3>
          <div class="photo-upload-area" @click="photoInputEl?.click()">
            <img
              v-if="coverPhoto"
              :src="coverPhoto"
              class="cover-preview"
              alt="cover"
            />
            <div v-else class="photo-placeholder">
              <Camera :size="28" />
              <span>{{ t("website.uploadCover") }}</span>
            </div>
            <div v-if="photoUploading" class="upload-overlay">Uploading…</div>
          </div>
          <input
            ref="photoInputEl"
            type="file"
            accept="image/*"
            hidden
            @change="onPhotoChange"
          />
        </div>

        <!-- URL -->
        <div class="card">
          <h3 class="card-title">
            <Link :size="15" /> {{ t("website.urlSlug") }}
          </h3>
          <div class="slug-row">
            <span class="slug-pre">{{ origin }}/w/</span>
            <input
              v-model="form.subdomain"
              class="input slug-input"
              placeholder="your-names"
            />
          </div>
          <p class="url-hint">{{ publicUrl }}</p>
        </div>

        <!-- Hero message -->
        <div class="card">
          <h3 class="card-title">{{ t("website.heroMessage") }}</h3>
          <input
            v-model="form.heroMessage"
            class="input"
            placeholder="We're getting married! 💍"
          />
        </div>

        <!-- Our Story -->
        <div class="card">
          <h3 class="card-title">{{ t("website.ourStory") }}</h3>
          <textarea
            v-model="form.story"
            class="input textarea"
            rows="6"
            placeholder="Tell your love story…"
          />
        </div>
      </div>

      <!-- Right column -->
      <div class="col">
        <!-- Ceremony details -->
        <div class="card">
          <h3 class="card-title">{{ t("website.ceremony") }}</h3>
          <div class="field-group">
            <label class="field-label">{{ t("website.date") }}</label>
            <input
              v-model="form.ceremonyDate"
              class="input"
              placeholder="e.g. Saturday, 14 June 2025 at 14:00"
            />
          </div>
          <div class="field-group mt">
            <label class="field-label">{{ t("website.location") }}</label>
            <input
              v-model="form.ceremonyLocation"
              class="input"
              placeholder="St. Mary's Church, Bucharest"
            />
          </div>
        </div>

        <!-- Reception details -->
        <div class="card">
          <h3 class="card-title">{{ t("website.reception") }}</h3>
          <div class="field-group">
            <label class="field-label">{{ t("website.date") }}</label>
            <input
              v-model="form.receptionDate"
              class="input"
              placeholder="e.g. Saturday, 14 June 2025 at 17:00"
            />
          </div>
          <div class="field-group mt">
            <label class="field-label">{{ t("website.location") }}</label>
            <input
              v-model="form.receptionLocation"
              class="input"
              placeholder="Grand Ballroom, Intercontinental"
            />
          </div>
        </div>

        <!-- Published toggle -->
        <div class="card">
          <label class="toggle-row">
            <div class="toggle-wrap">
              <input
                type="checkbox"
                v-model="form.published"
                class="hidden-check"
              />
              <span class="toggle-track" :class="{ on: form.published }"></span>
            </div>
            <div>
              <span class="toggle-label">{{
                form.published
                  ? t("website.publishedLabel")
                  : t("website.unpublishedLabel")
              }}</span>
              <p class="toggle-hint">
                {{
                  form.published
                    ? t("website.publishedHint")
                    : t("website.unpublishedHint")
                }}
              </p>
            </div>
          </label>
        </div>

        <!-- Share -->
        <div class="card share-card" v-if="form.subdomain">
          <h3 class="card-title">
            <Share2 :size="15" /> {{ t("website.shareTitle") }}
          </h3>
          <div class="share-btns">
            <button class="share-btn copy" @click="copyLink">
              <component :is="copied ? Check : Copy" :size="14" />
              {{ copied ? t("website.copied") : t("website.copy") }}
            </button>
            <button class="share-btn whatsapp" @click="shareWhatsApp">
              WhatsApp
            </button>
            <button class="share-btn email" @click="shareEmail">Email</button>
          </div>
        </div>

        <!-- Save -->
        <div class="card">
          <button class="save-btn" :disabled="saving" @click="save">
            {{
              saving
                ? t("website.saving")
                : saved
                  ? t("website.savedMsg")
                  : t("common.save")
            }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.subtitle {
  color: var(--color-muted);
  margin: 0;
  font-size: 0.9rem;
}
.header-actions {
  display: flex;
  gap: 10px;
}
.preview-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.85rem;
  text-decoration: none;
  color: var(--color-text);
}
.preview-btn:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}
.preview-btn.live {
  background: var(--color-gold);
  border-color: var(--color-gold);
  color: #fff;
}
.preview-btn.live:hover {
  opacity: 0.88;
  color: #fff;
}

.stats-strip {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}
.stat {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 14px 20px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.stat-value {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
}
.stat-value.live {
  color: #22c55e;
}
.stat-value.draft {
  color: var(--color-muted);
}
.stat-label {
  font-size: 0.75rem;
  color: var(--color-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
.col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
}
.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text);
}

.photo-upload-area {
  border: 2px dashed var(--color-border);
  border-radius: 10px;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  background: var(--color-surface);
  transition: border-color 0.2s;
}
.photo-upload-area:hover {
  border-color: var(--color-gold);
}
.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  min-height: 180px;
}
.photo-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--color-muted);
  font-size: 0.85rem;
}
.upload-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.9rem;
}

.slug-row {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}
.slug-pre {
  padding: 10px 10px;
  background: var(--color-surface);
  font-size: 0.8rem;
  color: var(--color-muted);
  border-right: 1px solid var(--color-border);
  white-space: nowrap;
}
.slug-input {
  border: none !important;
  border-radius: 0 !important;
  flex: 1;
}
.url-hint {
  margin: 6px 0 0;
  font-size: 0.78rem;
  color: var(--color-gold);
  word-break: break-all;
}

.input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  background: var(--color-white);
}
.input:focus {
  border-color: var(--color-gold);
}
.textarea {
  resize: vertical;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.field-group.mt {
  margin-top: 12px;
}
.field-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-muted);
}

.toggle-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  cursor: pointer;
}
.toggle-wrap {
  flex-shrink: 0;
  position: relative;
}
.hidden-check {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}
.toggle-track {
  display: block;
  width: 42px;
  height: 24px;
  border-radius: 99px;
  background: var(--color-border);
  transition: background 0.2s;
  position: relative;
  cursor: pointer;
}
.toggle-track::after {
  content: "";
  position: absolute;
  top: 3px;
  left: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.2s;
}
.toggle-track.on {
  background: var(--color-gold);
}
.toggle-track.on::after {
  transform: translateX(18px);
}
.toggle-label {
  font-size: 0.9rem;
  font-weight: 600;
}
.toggle-hint {
  font-size: 0.8rem;
  color: var(--color-muted);
  margin: 2px 0 0;
}

.share-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.share-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 9px 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: opacity 0.15s;
}
.share-btn:hover {
  opacity: 0.85;
}
.share-btn.copy {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  color: var(--color-text);
}
.share-btn.whatsapp {
  background: #25d366;
  color: #fff;
}
.share-btn.email {
  background: #4f46e5;
  color: #fff;
}

.save-btn {
  width: 100%;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 12px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.95rem;
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
