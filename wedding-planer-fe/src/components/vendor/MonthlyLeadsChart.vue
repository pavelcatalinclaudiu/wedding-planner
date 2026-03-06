<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { MonthlyLeadDTO } from "@/types/vendor.types";

const props = defineProps<{
  data: MonthlyLeadDTO[];
  loading?: boolean;
}>(); 

const { t } = useI18n();

const maxCount = computed(() =>
  props.data.length ? Math.max(...props.data.map((d) => d.count), 1) : 1,
);

function barHeight(count: number): number {
  if (maxCount.value === 0) return 4;
  return Math.max(4, Math.round((count / maxCount.value) * 96));
}

// Skeleton mock months
const skeletonHeights = [30, 55, 40, 75, 50, 90, 65];
</script>

<template>
  <div class="chart-card">
    <h3 class="chart-title">{{ t("vendor.overview.monthlyLeads") }}</h3>

    <div class="bar-row" v-if="!loading && data.length">
      <div
        v-for="item in data"
        :key="item.month"
        class="bar-col"
        :title="`${item.month}: ${item.count} leads`"
      >
        <span class="bar-count" v-if="item.count > 0">{{ item.count }}</span>
        <div
          class="bar"
          :class="{ current: item.isCurrent }"
          :style="{ height: barHeight(item.count) + 'px' }"
        />
        <span class="bar-month">{{ item.month }}</span>
      </div>
    </div>

    <!-- Skeleton -->
    <div class="bar-row" v-else-if="loading">
      <div v-for="(h, i) in skeletonHeights" :key="i" class="bar-col">
        <div class="bar sk-bar" :style="{ height: h + 'px' }" />
        <div class="sk-label-bar" />
      </div>
    </div>

    <div v-else class="chart-empty">{{ t("vendor.overview.noLeadData") }}</div>
  </div>
</template>

<style scoped>
.chart-card {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-border, #e8e0d5);
  border-radius: 12px;
  padding: 24px;
}
.chart-title {
  font-size: 0.9rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-muted, #8a7f76);
  margin: 0 0 20px;
}
.bar-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 120px;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  height: 120px;
  justify-content: flex-end;
  cursor: default;
  position: relative;
}
.bar-count {
  font-size: 0.7rem;
  font-weight: 600;
  color: var(--color-text, #1a1a2e);
  margin-bottom: 2px;
}
.bar {
  width: 100%;
  min-height: 4px;
  background: var(--gold, #c8a951);
  opacity: 0.4;
  border-radius: 4px 4px 0 0;
  transition: opacity 0.15s;
}
.bar.current {
  opacity: 1;
}
.bar-col:hover .bar {
  opacity: 1;
}
.bar-month {
  font-size: 0.7rem;
  color: var(--color-muted, #8a7f76);
}
/* Skeleton */
.sk-bar {
  background: linear-gradient(
    90deg,
    var(--sk-base) 25%,
    var(--sk-highlight) 50%,
    var(--sk-base) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  opacity: 1;
}
.sk-label-bar {
  height: 8px;
  width: 70%;
  background: var(--sk-base);
  border-radius: 4px;
}
.chart-empty {
  text-align: center;
  color: var(--color-muted, #8a7f76);
  font-size: 0.85rem;
  padding: 24px 0;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
