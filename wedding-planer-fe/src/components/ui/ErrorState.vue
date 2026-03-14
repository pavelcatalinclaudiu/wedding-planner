<script setup lang="ts">
import { AlertTriangle, RefreshCw, WifiOff } from "lucide-vue-next";

const props = withDefaults(
  defineProps<{
    /** Main error message shown to the user. */
    message?: string;
    /** Optional secondary description. */
    description?: string;
    /** Show a network-off icon instead of the default warning triangle. */
    network?: boolean;
    /** Pass true while a retry is in progress to spin the icon + disable button. */
    retrying?: boolean;
    /** Hide the retry button entirely. */
    hideRetry?: boolean;
    /** Override the retry button label. */
    retryLabel?: string;
  }>(),
  {
    message: "Something went wrong.",
    retrying: false,
    hideRetry: false,
    retryLabel: "Try again",
    network: false,
  },
);

const emit = defineEmits<{ retry: [] }>();
</script>

<template>
  <div class="error-state" role="alert">
    <div class="error-icon-wrap">
      <WifiOff v-if="props.network" class="error-icon" :size="36" />
      <AlertTriangle v-else class="error-icon" :size="36" />
    </div>

    <p class="error-title">{{ props.message }}</p>

    <p v-if="props.description" class="error-description">
      {{ props.description }}
    </p>

    <button
      v-if="!props.hideRetry"
      class="btn-outline error-retry"
      :disabled="props.retrying"
      @click="emit('retry')"
    >
      <RefreshCw :size="14" :class="{ spin: props.retrying }" />
      {{ props.retrying ? "Retrying…" : props.retryLabel }}
    </button>
  </div>
</template>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 24px;
  text-align: center;
}

.error-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--color-border, #f0ece8);
  display: flex;
  align-items: center;
  justify-content: center;
}

.error-icon {
  color: var(--color-muted, #9b8ea0);
}

.error-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--color-text, #1a1a2e);
  margin: 0;
}

.error-description {
  font-size: 0.85rem;
  color: var(--color-muted, #9b8ea0);
  margin: 0;
  max-width: 320px;
  line-height: 1.5;
}

.error-retry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.spin {
  animation: spin 0.8s linear infinite;
}
</style>
