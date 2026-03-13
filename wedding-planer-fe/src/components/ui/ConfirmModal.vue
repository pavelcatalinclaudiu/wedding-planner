<script setup lang="ts">
import { watch, onUnmounted } from "vue";
import { useConfirm } from "@/composables/useConfirm";

const { visible, message, title, confirmText, cancelText, danger, resolve } =
  useConfirm();

function onKeyDown(e: KeyboardEvent) {
  if (e.key === "Escape") resolve(false);
}

watch(visible, (v) => {
  if (v) document.addEventListener("keydown", onKeyDown);
  else document.removeEventListener("keydown", onKeyDown);
});
onUnmounted(() => document.removeEventListener("keydown", onKeyDown));
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm-fade">
      <div
        v-if="visible"
        class="confirm-backdrop"
        @click.self="resolve(false)"
        role="dialog"
        aria-modal="true"
      >
        <div class="confirm-modal">
          <h3 class="confirm-title">{{ title }}</h3>
          <p class="confirm-message">{{ message }}</p>
          <div class="confirm-actions">
            <button class="btn-cancel" @click="resolve(false)">
              {{ cancelText }}
            </button>
            <button
              class="btn-confirm"
              :class="{ 'btn-danger': danger }"
              @click="resolve(true)"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9000;
}
.confirm-modal {
  background: var(--color-white, #fff);
  border-radius: 14px;
  padding: 28px 32px;
  min-width: 320px;
  max-width: 440px;
  width: 90%;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.confirm-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text, #1a1a1a);
}
.confirm-message {
  margin: 0;
  font-size: 0.95rem;
  color: var(--color-muted, #666);
  line-height: 1.5;
}
.confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}
.btn-cancel {
  padding: 8px 18px;
  background: none;
  border: 1.5px solid var(--color-border, #e5e5e5);
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--color-muted, #666);
  cursor: pointer;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.btn-cancel:hover {
  border-color: var(--color-text, #1a1a1a);
  color: var(--color-text, #1a1a1a);
}
.btn-confirm {
  padding: 8px 18px;
  background: var(--color-gold, #c9a84c);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-confirm:hover {
  opacity: 0.88;
}
.btn-confirm.btn-danger {
  background: var(--color-error, #e53e3e);
}
/* Transition */
.confirm-fade-enter-active,
.confirm-fade-leave-active {
  transition: opacity 0.18s ease;
}
.confirm-fade-enter-from,
.confirm-fade-leave-to {
  opacity: 0;
}
.confirm-fade-enter-active .confirm-modal,
.confirm-fade-leave-active .confirm-modal {
  transition: transform 0.18s ease;
}
.confirm-fade-enter-from .confirm-modal {
  transform: scale(0.95);
}
.confirm-fade-leave-to .confirm-modal {
  transform: scale(0.95);
}
</style>
