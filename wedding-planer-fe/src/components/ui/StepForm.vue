<script setup lang="ts">
import { ref, computed } from "vue";
import { Check } from "lucide-vue-next";

const props = defineProps<{ steps: string[] }>();
const emit = defineEmits<{ complete: [] }>();

const currentStep = ref(0);
const isLast = computed(() => currentStep.value === props.steps.length - 1);

function next() {
  if (!isLast.value) currentStep.value++;
  else emit("complete");
}

function prev() {
  if (currentStep.value > 0) currentStep.value--;
}

defineExpose({ currentStep, next, prev });
</script>

<template>
  <div class="step-form">
    <div class="step-indicators">
      <div
        v-for="(step, i) in steps"
        :key="i"
        class="step-indicator"
        :class="{ active: i === currentStep, passed: i < currentStep }"
      >
        <div class="step-dot">
          <Check v-if="i < currentStep" :size="14" /><span v-else>{{
            i + 1
          }}</span>
        </div>
        <span class="step-label">{{ step }}</span>
      </div>
    </div>

    <div class="step-content">
      <slot :current-step="currentStep" />
    </div>

    <div class="step-nav">
      <button v-if="currentStep > 0" class="prev-btn" @click="prev">
        ← Back
      </button>
      <div class="spacer"></div>
      <button class="next-btn" @click="next">
        {{ isLast ? "Complete" : "Next →" }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.step-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.step-indicators {
  display: flex;
  gap: 0;
}
.step-indicator {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
}
.step-indicator:not(:last-child)::after {
  content: "";
  position: absolute;
  top: 14px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--color-border);
  z-index: 0;
}
.step-indicator.passed:not(:last-child)::after {
  background: var(--color-gold);
}
.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-surface);
  border: 2px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-muted);
  position: relative;
  z-index: 1;
}
.step-indicator.active .step-dot {
  border-color: var(--color-gold);
  color: var(--color-gold);
  background: var(--color-gold-light, #fdf8ee);
}
.step-indicator.passed .step-dot {
  border-color: var(--color-gold);
  background: var(--color-gold);
  color: #fff;
}
.step-label {
  font-size: 0.75rem;
  color: var(--color-muted);
  font-weight: 500;
  text-align: center;
}
.step-indicator.active .step-label {
  color: var(--color-gold);
  font-weight: 700;
}
.step-content {
  min-height: 200px;
}
.step-nav {
  display: flex;
  align-items: center;
}
.spacer {
  flex: 1;
}
.prev-btn {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  font-size: 0.9rem;
}
.next-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 22px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.9rem;
}
</style>
