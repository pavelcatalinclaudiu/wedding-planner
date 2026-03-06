<script setup lang="ts">
import { computed } from "vue";
import { useCountdown } from "@/composables/useCountdown";

const props = defineProps<{ date: string }>();
const { days, hours, minutes } = useCountdown(computed(() => props.date));
</script>

<template>
  <div class="countdown">
    <div class="unit">
      <span class="value">{{ days }}</span>
      <span class="label">days</span>
    </div>
    <span class="sep">:</span>
    <div class="unit">
      <span class="value">{{ hours }}</span>
      <span class="label">hrs</span>
    </div>
    <span class="sep">:</span>
    <div class="unit">
      <span class="value">{{ minutes }}</span>
      <span class="label">min</span>
    </div>
  </div>
</template>

<style scoped>
.countdown {
  display: flex;
  align-items: center;
  gap: 4px;
}
.unit {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.value {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-gold);
  line-height: 1;
}
.label {
  font-size: 0.65rem;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.sep {
  font-size: 1.2rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 12px;
}
</style>
