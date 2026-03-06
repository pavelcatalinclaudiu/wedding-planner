<script setup lang="ts">
import type { ProfilePerformanceDTO } from "@/types/vendor.types";

const props = defineProps<{
  data: ProfilePerformanceDTO | null;
  loading?: boolean;
}>();

function rankColour(rank: number): string {
  if (rank <= 3) return "#c8a951";
  if (rank <= 10) return "#2e7d32";
  return "var(--color-muted, #8a7f76)";
}

function responseLabel(hours: number): { text: string; colour: string } {
  if (hours < 0)
    return { text: "No data", colour: "var(--color-muted, #8a7f76)" };
  if (hours < 2) return { text: "Excellent", colour: "#2e7d32" };
  if (hours < 12) return { text: "Good", colour: "#e65100" };
  return { text: "Slow", colour: "#c62828" };
}

function formatHours(h: number): string {
  if (h < 0) return "—";
  if (h < 0.1) return "< 6 min";
  if (h < 1) return "< 1 hour";
  if (h < 2) return "~1 hour";
  return `~${Math.round(h)} hours`;
}

function conversionColour(rate: number, avg: number): string {
  return rate >= avg ? "#2e7d32" : "var(--color-muted, #8a7f76)";
}
</script>

<template>
  <div class="widget">
    <h3 class="widget-title">Profile Performance</h3>

    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="sk-row">
        <div class="sk sk-label" />
        <div class="sk sk-val" />
      </div>
    </template>

    <template v-else-if="data">
      <!-- Search Rank -->
      <div class="perf-row">
        <span class="perf-label">Search rank</span>
        <div class="perf-val-group">
          <span
            class="perf-val"
            :style="{ color: rankColour(data.searchRank) }"
          >
            #{{ data.searchRank }}
          </span>
          <span class="perf-sub">{{ data.searchRankCategory }}</span>
        </div>
      </div>
      <div class="divider" />

      <!-- Avg Response Time -->
      <div class="perf-row">
        <span class="perf-label">Avg response time</span>
        <div class="perf-val-group">
          <span class="perf-val">{{
            formatHours(data.avgResponseTimeHours)
          }}</span>
          <span
            class="perf-sub"
            :style="{ color: responseLabel(data.avgResponseTimeHours).colour }"
          >
            {{ responseLabel(data.avgResponseTimeHours).text }}
          </span>
        </div>
      </div>
      <div class="divider" />

      <!-- Conversion Rate -->
      <div class="perf-row">
        <span class="perf-label">Conversion rate</span>
        <div class="perf-val-group">
          <span
            class="perf-val"
            :style="{
              color: conversionColour(
                data.conversionRate,
                data.platformAvgConversion,
              ),
            }"
          >
            {{ data.conversionRate }}%
          </span>
          <span class="perf-sub"
            >Platform avg: {{ data.platformAvgConversion }}%</span
          >
        </div>
      </div>
    </template>

    <div v-else class="empty">No performance data yet</div>
  </div>
</template>

<style scoped>
.widget {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-border, #e8e0d5);
  border-radius: 12px;
  padding: 20px;
}
.widget-title {
  font-size: 0.95rem;
  font-weight: 600;
  margin: 0 0 16px;
}
.perf-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 10px 0;
  gap: 12px;
}
.perf-label {
  font-size: 0.82rem;
  color: var(--color-muted, #8a7f76);
  padding-top: 2px;
}
.perf-val-group {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.perf-val {
  font-size: 1rem;
  font-weight: 700;
}
.perf-sub {
  font-size: 0.72rem;
  color: var(--color-muted, #8a7f76);
  text-align: right;
}

.divider {
  height: 1px;
  background: var(--color-border, #e8e0d5);
  margin: 0;
}
.empty {
  text-align: center;
  color: var(--color-muted, #8a7f76);
  font-size: 0.85rem;
  padding: 24px 0;
}

/* Skeleton */
.sk-row {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
}
.sk {
  background: linear-gradient(
    90deg,
    var(--sk-base) 25%,
    var(--sk-highlight) 50%,
    var(--sk-base) 75%
  );
  background-size: 200%;
  animation: shimmer 1.4s infinite;
  border-radius: 6px;
}
.sk-label {
  height: 11px;
  width: 40%;
}
.sk-val {
  height: 11px;
  width: 30%;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
