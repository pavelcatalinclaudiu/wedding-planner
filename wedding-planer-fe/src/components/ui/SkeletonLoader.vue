<script setup lang="ts">
defineProps<{
  lines?: number; // number of text line skeletons
  height?: string; // for block-level skeletons
  width?: string; // for inline/partial width
  circle?: boolean; // round skeleton (avatar)
}>();
</script>

<template>
  <div
    v-if="circle"
    class="skeleton skeleton-block"
    :style="{
      width: width ?? '40px',
      height: height ?? width ?? '40px',
      borderRadius: '50%',
    }"
  />
  <div
    v-else-if="height"
    class="skeleton skeleton-block"
    :style="{ height, width: width ?? '100%' }"
  />
  <div v-else class="skeleton-lines">
    <div
      v-for="i in lines ?? 3"
      :key="i"
      class="skeleton skeleton-line"
      :style="{ width: i === (lines ?? 3) ? '65%' : '100%' }"
    />
  </div>
</template>

<style scoped>
.skeleton-block,
.skeleton-line {
  border-radius: var(--radius-sm);
  background: linear-gradient(
    90deg,
    var(--sk-base, #f0ebe6) 25%,
    var(--sk-highlight, #e8e0d8) 50%,
    var(--sk-base, #f0ebe6) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton-lines {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.skeleton-line {
  height: 14px;
}
@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
