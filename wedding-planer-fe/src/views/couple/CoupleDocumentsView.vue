<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { coupleApi } from "@/api/couple.api";
import { FileText } from "lucide-vue-next";

const { t } = useI18n();
const apiBase = import.meta.env.VITE_API_BASE_URL as string; // e.g. http://localhost:8080/api

const documents = ref<
  Array<{
    id: string;
    name: string;
    url: string;
    size: number;
    uploadedAt: string;
  }>
>([]);
const loading = ref(false);
const uploading = ref(false);
const fileInputRef = ref<HTMLInputElement | null>(null);

async function fetchDocs() {
  loading.value = true;
  try {
    const res = await coupleApi.getDocuments();
    documents.value = res.data;
  } finally {
    loading.value = false;
  }
}

async function handleUpload(e: Event) {
  const files = (e.target as HTMLInputElement).files;
  if (!files?.length) return;
  uploading.value = true;
  try {
    for (const file of Array.from(files)) {
      await coupleApi.uploadDocument(file);
    }
    await fetchDocs();
  } finally {
    uploading.value = false;
    // Reset input so same file can be re-uploaded
    if (fileInputRef.value) fileInputRef.value.value = "";
  }
}

async function deleteDoc(id: string) {
  await coupleApi.deleteDocument(id);
  documents.value = documents.value.filter((d) => d.id !== id);
}

onMounted(fetchDocs);
</script>

<template>
  <div class="docs-view">
    <div class="docs-header">
      <h2>{{ t("documents.title") }}</h2>
      <button class="upload-btn" @click="fileInputRef?.click()">
        + {{ t("documents.upload") }}
      </button>
      <input
        ref="fileInputRef"
        type="file"
        multiple
        hidden
        @change="handleUpload"
      />
    </div>

    <div v-if="uploading" class="upload-progress">
      {{ t("documents.uploading") }}
    </div>

    <div v-if="loading" class="loading">{{ t("documents.loading") }}</div>
    <div v-else-if="documents.length === 0" class="empty">
      {{ t("documents.noDocuments") }}
    </div>
    <div v-else class="doc-list">
      <div v-for="doc in documents" :key="doc.id" class="doc-row">
        <span class="doc-icon"><FileText :size="20" /></span>
        <a
          :href="apiBase.replace('/api', '') + doc.url"
          target="_blank"
          class="doc-name"
          >{{ doc.name }}</a
        >
        <span class="doc-date">{{
          new Date(doc.uploadedAt).toLocaleDateString()
        }}</span>
        <button class="remove-btn" @click="deleteDoc(doc.id)">×</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.docs-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}
h2 {
  margin: 0;
  font-size: 1.4rem;
  flex: 1;
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
  background: var(--color-gold-light, #fdf8ee);
  border: 1px solid var(--color-gold);
  border-radius: 8px;
  padding: 10px 16px;
  font-size: 0.9rem;
  color: var(--color-gold);
  font-weight: 600;
  margin-bottom: 16px;
}
.loading,
.empty {
  color: var(--color-muted);
  text-align: center;
  padding: 48px 0;
}
.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.doc-row {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px 16px;
}
.doc-icon {
  font-size: 1.1rem;
}
.doc-name {
  flex: 1;
  font-size: 0.9rem;
  color: var(--color-gold);
  text-decoration: none;
  font-weight: 500;
}
.doc-name:hover {
  text-decoration: underline;
}
.doc-date {
  font-size: 0.8rem;
  color: var(--color-muted);
  white-space: nowrap;
}
.remove-btn {
  background: none;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: var(--color-muted);
}
.remove-btn:hover {
  color: var(--color-error);
}
</style>
