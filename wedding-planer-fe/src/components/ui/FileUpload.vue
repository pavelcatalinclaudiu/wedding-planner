<script setup lang="ts">
import { ref } from "vue";
import { useFileUpload } from "@/composables/useFileUpload";

const props = defineProps<{
  endpoint: string;
  accept?: string;
  multiple?: boolean;
  label?: string;
}>();
const emit = defineEmits<{ uploaded: [] }>();

const { upload, uploading, progress } = useFileUpload();
const fileInputRef = ref<HTMLInputElement | null>(null);
const dragOver = ref(false);

async function handleFiles(files: FileList | null) {
  if (!files?.length) return;
  await upload(props.endpoint, Array.from(files));
  emit("uploaded");
}

function onDrop(e: DragEvent) {
  dragOver.value = false;
  handleFiles(e.dataTransfer?.files ?? null);
}
</script>

<template>
  <div
    class="file-upload-zone"
    :class="{ dragover: dragOver, uploading }"
    @dragover.prevent="dragOver = true"
    @dragleave="dragOver = false"
    @drop.prevent="onDrop"
    @click="fileInputRef?.click()"
  >
    <span class="upload-icon">↑</span>
    <p class="upload-label">
      {{ label ?? "Click or drag files here to upload" }}
    </p>
    <div v-if="uploading" class="upload-bar">
      <div class="upload-fill" :style="{ width: `${progress}%` }"></div>
    </div>
    <input
      ref="fileInputRef"
      type="file"
      :accept="accept"
      :multiple="multiple"
      hidden
      @change="(e: Event) => handleFiles((e.target as HTMLInputElement).files)"
    />
  </div>
</template>

<style scoped>
.file-upload-zone {
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  cursor: pointer;
  transition:
    border-color 0.2s,
    background 0.2s;
}
.file-upload-zone:hover,
.file-upload-zone.dragover {
  border-color: var(--color-gold);
  background: var(--color-gold-light, #fdf8ee);
}
.file-upload-zone.uploading {
  pointer-events: none;
  opacity: 0.8;
}
.upload-icon {
  font-size: 2rem;
  color: var(--color-gold);
  display: block;
  margin-bottom: 8px;
}
.upload-label {
  margin: 0 0 14px;
  font-size: 0.9rem;
  color: var(--color-muted);
}
.upload-bar {
  height: 4px;
  background: var(--color-surface);
  border-radius: 2px;
  overflow: hidden;
}
.upload-fill {
  height: 100%;
  background: var(--color-gold);
  transition: width 0.3s;
}
</style>
