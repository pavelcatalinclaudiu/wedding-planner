<script setup lang="ts">
defineProps<{ value: number; color?: string }>();
</script>

<template>
  <div class="progress-bar">
    <div
      class="progress-fill"
      :style="{
        width: `${Math.min(100, Math.max(0, value))}%`,
        background: color,
      }"
    ></div>
  </div>
</template>

<style scoped>
.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--color-surface);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--color-gold);
  border-radius: 4px;
  transition: width 0.4s ease;
}
</style>
