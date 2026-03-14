<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";

defineProps<{
  title?: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
}>();
const emit = defineEmits<{ confirm: []; cancel: [] }>();

function onKey(e: KeyboardEvent) {
  if (e.key === "Escape") emit("cancel");
  if (e.key === "Enter") emit("confirm");
}

onMounted(() => document.addEventListener("keydown", onKey));
onUnmounted(() => document.removeEventListener("keydown", onKey));
</script>

<template>
  <div class="overlay" @click.self="emit('cancel')">
    <div class="dialog">
      <h3 v-if="title" class="dialog-title">{{ title }}</h3>
      <p class="dialog-message">{{ message }}</p>
      <div class="dialog-actions">
        <button class="cancel-btn" @click="emit('cancel')">Cancel</button>
        <button
          class="confirm-btn"
          :class="{ danger }"
          @click="emit('confirm')"
        >
          {{ confirmLabel ?? "Confirm" }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 600;
}
.dialog {
  background: var(--color-white);
  border-radius: 14px;
  padding: 28px;
  max-width: 360px;
  width: 90%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}
.dialog-title {
  margin: 0 0 10px;
  font-size: 1.05rem;
  font-weight: 700;
}
.dialog-message {
  margin: 0 0 24px;
  font-size: 0.9rem;
  color: var(--color-text);
  line-height: 1.5;
}
.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}
.cancel-btn {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  font-size: 0.9rem;
}
.confirm-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.9rem;
}
.confirm-btn.danger {
  background: var(--color-error);
}
</style>
