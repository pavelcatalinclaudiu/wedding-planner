<script setup lang="ts">
import type { Component } from "vue";
import { useToast } from "@/composables/useToast";
import { Check, X, AlertTriangle, Info } from "lucide-vue-next";
const { toasts, dismiss } = useToast();

const icons: Record<string, Component> = {
  success: Check,
  error: X,
  warn: AlertTriangle,
  info: Info,
};
</script>

<template>
  <Teleport to="body">
    <div class="toast-container" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast" tag="div" class="toast-stack">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="toast"
          :class="`toast-${t.type}`"
          role="alert"
        >
          <span class="toast-icon"
            ><component :is="icons[t.type]" :size="16"
          /></span>
          <span class="toast-message">{{ t.message }}</span>
          <button class="toast-close" @click="dismiss(t.id)" aria-label="Close">
            <X :size="14" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  pointer-events: none;
}

.toast-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-end;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 280px;
  max-width: 380px;
  padding: 13px 16px;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.14);
  font-size: 0.88rem;
  font-weight: 500;
  line-height: 1.4;
  pointer-events: all;
  border-left: 4px solid transparent;
}

.toast-success {
  background: var(--color-green-light);
  color: var(--color-green);
  border-left-color: var(--color-green);
}
.toast-error {
  background: var(--color-error-light);
  color: var(--color-error);
  border-left-color: var(--color-error);
}
.toast-warn {
  background: var(--color-amber-light);
  color: var(--color-amber);
  border-left-color: var(--color-amber);
}
.toast-info {
  background: var(--color-info-light);
  color: var(--color-info);
  border-left-color: var(--color-info);
}

.toast-icon {
  flex-shrink: 0;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.72rem;
  font-weight: 800;
  background: currentColor;
  color: #fff;
  margin-top: 1px;
}

.toast-success .toast-icon {
  background: var(--color-green);
}
.toast-error .toast-icon {
  background: var(--color-error);
}
.toast-warn .toast-icon {
  background: var(--color-amber);
}
.toast-info .toast-icon {
  background: var(--color-info);
}

.toast-message {
  flex: 1;
}

.toast-close {
  flex-shrink: 0;
  background: none;
  border: none;
  cursor: pointer;
  color: currentColor;
  opacity: 0.5;
  font-size: 0.8rem;
  padding: 0;
  line-height: 1;
  margin-top: 2px;
}
.toast-close:hover {
  opacity: 1;
}

/* Transition */
.toast-enter-active {
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toast-leave-active {
  transition: all 0.2s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(60px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(60px);
}
.toast-move {
  transition: transform 0.2s ease;
}

@media (max-width: 480px) {
  .toast-container {
    bottom: 16px;
    right: 12px;
    left: 12px;
  }
  .toast {
    min-width: unset;
    max-width: unset;
    width: 100%;
  }
}
</style>
