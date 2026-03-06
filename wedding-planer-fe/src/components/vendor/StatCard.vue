<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";

const props = defineProps<{
  emoji: string;
  primary: string;
  label: string;
  trend?: number | null;
  trendFormat?: "currency" | "number" | "percent" | "percent-points";
  trendLabel?: string;
  sub?: string;
  loading?: boolean;
  colorByValue?: boolean;
  value?: number | null;
  thresholds?: { green: number; amber: number };
}>();

const { t } = useI18n();

const trendUp = computed(() => props.trend != null && props.trend > 0);
const trendDown = computed(() => props.trend != null && props.trend < 0);
const trendFlat = computed(() => props.trend != null && props.trend === 0);

function formatTrend(v: number): string {
  const abs = Math.abs(v);
  switch (props.trendFormat) {
    case "currency":
      return `€${abs.toLocaleString("en")}`;
    case "percent":
      return `${abs}%`;
    case "percent-points":
      return `${abs}pp`;
    default:
      return String(abs);
  }
}

const primaryClass = computed(() => {
  if (!props.colorByValue || props.value == null || !props.thresholds)
    return "";
  if (props.value >= props.thresholds.green) return "val-green";
  if (props.value >= props.thresholds.amber) return "val-amber";
  return "val-red";
});
</script>

<template>
  <div class="stat-card" :class="{ loading }">
    <template v-if="loading">
      <div class="sk sk-label" />
      <div class="sk sk-primary" />
      <div class="sk sk-sub" />
    </template>
    <template v-else>
      <div class="card-top">
        <span class="card-emoji">{{ emoji }}</span>
        <span class="card-label">{{ label }}</span>
      </div>
      <p class="card-primary" :class="primaryClass">{{ primary }}</p>
      <div class="card-trend" v-if="trend != null">
        <span v-if="trendUp" class="trend-up">
          ↑ {{ formatTrend(trend) }} {{ trendLabel }}
        </span>
        <span v-else-if="trendDown" class="trend-down">
          ↓ {{ formatTrend(trend) }} {{ trendLabel }}
        </span>
        <span v-else-if="trendFlat" class="trend-flat">{{
          t("vendor.stats.noChange")
        }}</span>
      </div>
      <p class="card-sub" v-if="sub">{{ sub }}</p>
    </template>
  </div>
</template>

<style scoped>
.stat-card {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-border, #e8e0d5);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}
.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.card-emoji {
  font-size: 1.1rem;
}
.card-label {
  font-size: 0.78rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-muted, #8a7f76);
}
.card-primary {
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-text, #1a1a2e);
  margin: 4px 0 0;
  line-height: 1.1;
}
.val-green {
  color: var(--color-green);
}
.val-amber {
  color: var(--color-amber);
}
.val-red {
  color: var(--color-error);
}

.card-trend {
  font-size: 0.8rem;
  font-weight: 500;
}
.trend-up {
  color: var(--color-green);
}
.trend-down {
  color: var(--color-error);
}
.trend-flat {
  color: var(--color-muted, #8a7f76);
}

.card-sub {
  font-size: 0.78rem;
  color: var(--color-muted, #8a7f76);
  margin: 0;
}

/* Skeleton states */
.stat-card.loading {
  pointer-events: none;
}
.sk {
  background: linear-gradient(
    90deg,
    var(--sk-base) 25%,
    var(--sk-highlight) 50%,
    var(--sk-base) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 6px;
}
.sk-label {
  height: 12px;
  width: 60%;
}
.sk-primary {
  height: 36px;
  width: 80%;
  margin-top: 8px;
}
.sk-sub {
  height: 10px;
  width: 50%;
  margin-top: 4px;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
