<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import type { WeddingWebsite } from "@/types/couple.types";
import { useCoupleStore } from "@/stores/couple.store";

const { t } = useI18n();
const coupleStore = useCoupleStore();
const website = ref<WeddingWebsite | null>(null);
const saving = ref(false);
const saved = ref(false);

const form = ref({
  slug: "",
  heroMessage: "",
  story: "",
  published: false,
});

onMounted(async () => {
  website.value = await coupleApi.getWeddingWebsite();
  if (website.value) {
    form.value.slug = website.value.slug ?? "";
    form.value.heroMessage = website.value.heroMessage ?? "";
    form.value.story = website.value.story ?? "";
    form.value.published = website.value.published ?? false;
  }
});

async function save() {
  saving.value = true;
  try {
    await coupleApi.updateWeddingWebsite(form.value);
    saved.value = true;
    setTimeout(() => (saved.value = false), 2000);
  } finally {
    saving.value = false;
  }
}

const previewUrl = () =>
  `${import.meta.env.VITE_APP_URL}/wedding/${form.value.slug || "your-names"}`;
</script>

<template>
  <div class="website-view">
    <h2>{{ t("website.title") }}</h2>
    <p class="subtitle">{{ t("website.subtitle") }}</p>

    <div class="form-card">
      <div class="field">
        <label>{{ t("website.urlSlug") }}</label>
        <div class="slug-input-group">
          <span class="slug-prefix">eternelle.ro/wedding/</span>
          <input v-model="form.slug" class="input" placeholder="your-names" />
        </div>
        <p class="preview-link">
          <a :href="previewUrl()" target="_blank">{{ previewUrl() }}</a>
        </p>
      </div>

      <div class="field">
        <label>{{ t("website.heroMessage") }}</label>
        <input
          v-model="form.heroMessage"
          class="input"
          placeholder="We're getting married! 💍"
        />
      </div>

      <div class="field">
        <label>{{ t("website.ourStory") }}</label>
        <textarea
          v-model="form.story"
          class="input textarea"
          rows="6"
          placeholder="Tell your love story…"
        />
      </div>

      <label class="toggle-field">
        <input type="checkbox" v-model="form.published" />
        <span>{{ t("website.publishedLabel") }}</span>
      </label>

      <div class="form-actions">
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
</template>

<style scoped>

h2 { margin: 0 0 6px; font-size: 1.4rem; }
.subtitle { color: var(--color-muted); margin: 0 0 24px; font-size: 0.9rem; }
.form-card { background: var(--color-white); border: 1px solid var(--color-border); border-radius: 14px; padding: 28px; display: flex; flex-direction: column; gap: 20px; }
.field { display: flex; flex-direction: column; gap: 6px; }
label { font-size: 0.85rem; font-weight: 600; color: var(--color-text); }
.input { padding: 10px 14px; border: 1.5px solid var(--color-border); border-radius: 8px; font-size: 0.9rem; outline: none; font-family: inherit; }
.input:focus { border-color: var(--color-gold); }
.textarea { resize: vertical; }
.slug-input-group { display: flex; align-items: center; border: 1.5px solid var(--color-border); border-radius: 8px; overflow: hidden; focus-within:border-color: var(--color-gold); }
.slug-prefix { padding: 10px 12px; background: var(--color-surface); font-size: 0.85rem; color: var(--color-muted); border-right: 1px solid var(--color-border); white-space: nowrap; }
.slug-input-group .input { border: none; border-radius: 0; flex: 1; }
.preview-link { margin: 0; font-size: 0.8rem; }
.preview-link a { color: var(--color-gold); }
.toggle-field { display: flex; align-items: center; gap: 10px; cursor: pointer; font-size: 0.9rem; }
.form-actions { display: flex; }
.save-btn { background: var(--color-gold); color: #fff; border: none; border-radius: 8px; padding: 10px 28px; font-weight: 700; cursor: pointer; font-size: 0.95rem; }
.save-btn:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
