<script setup lang="ts">
import { useI18n } from "vue-i18n";
import type { UpcomingCallDTO } from "@/types/vendor.types";

const props = defineProps<{
  calls: UpcomingCallDTO[];
  loading?: boolean;
}>();

const emit = defineEmits<{ join: [callId: string] }>();

const { t } = useI18n();

function formatDateTime(iso: string): string {
  const d = new Date(iso);
  const date = d.toLocaleDateString("en-GB", {
    day: "numeric",
    month: "short",
  });
  const time = d.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });
  return `${date} · ${time}`;
}

function minutesUntil(iso: string): number {
  return Math.floor((new Date(iso).getTime() - Date.now()) / 60_000);
}

function countdownLabel(iso: string): string | null {
  const mins = minutesUntil(iso);
  if (mins < 0) return null;
  if (mins < 60) return t("common.inMins", { m: mins });
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return t("common.inHrs", { h: hrs });
  return null;
}

function isImminent(iso: string): boolean {
  const m = minutesUntil(iso);
  return m >= 0 && m <= 30;
}
</script>

<template>
  <div class="widget">
    <div class="widget-header">
      <h3>{{ t("vendor.overview.upcomingCalls") }}</h3>
      <RouterLink to="/vendor/calls" class="view-all">{{
        t("vendor.overview.viewAll")
      }}</RouterLink>
    </div>

    <template v-if="loading">
      <div v-for="i in 2" :key="i" class="sk-row">
        <div class="sk sk-icon-sq" />
        <div class="sk-lines">
          <div class="sk sk-ln1" />
          <div class="sk sk-ln2" />
        </div>
      </div>
    </template>

    <template v-else-if="calls.length">
      <div
        v-for="call in calls"
        :key="call.callId"
        class="call-row"
        :class="{ imminent: isImminent(call.scheduledAt) }"
      >
        <span class="call-icon">📹</span>
        <div class="call-info">
          <p class="call-name">{{ call.coupleName }}</p>
          <p class="call-time">{{ formatDateTime(call.scheduledAt) }}</p>
        </div>
        <div class="call-right">
          <span v-if="countdownLabel(call.scheduledAt)" class="countdown">
            {{ countdownLabel(call.scheduledAt) }}
          </span>
          <button
            class="join-btn"
            :class="{ pulse: isImminent(call.scheduledAt) }"
            @click="emit('join', call.callId)"
          >
            {{ t("vendor.overview.join") }}
          </button>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      <p class="empty-title">{{ t("vendor.overview.noCallsTitle") }}</p>
      <p class="empty-sub">{{ t("vendor.overview.noCallsSub") }}</p>
    </div>
  </div>
</template>

<style scoped>
.widget {
  background: var(--color-white, #fff);
  border: 1px solid var(--color-border, #e8e0d5);
  border-radius: 12px;
  padding: 20px;
}
.widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.widget-header h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 600;
}
.view-all {
  font-size: 0.78rem;
  color: var(--color-gold);
  text-decoration: none;
}
.view-all:hover {
  text-decoration: underline;
}

.call-row {
  display: flex;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 8px;
  align-items: center;
  border: 1px solid transparent;
  transition:
    border-color 0.15s,
    background 0.12s;
}
.call-row:hover {
  background: var(--color-bg, #f8f5f0);
}
.call-row.imminent {
  border-color: var(--color-gold);
}

.call-icon {
  font-size: 1.3rem;
}
.call-info {
  flex: 1;
  min-width: 0;
}
.call-name {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
}
.call-time {
  margin: 2px 0 0;
  font-size: 0.75rem;
  color: var(--color-muted, #8a7f76);
}

.call-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}
.countdown {
  font-size: 0.72rem;
  color: var(--color-muted, #8a7f76);
}

.join-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
}
.join-btn:hover {
  opacity: 0.85;
}
.join-btn.pulse {
  animation: pulse 1.5s infinite;
}
@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.65;
  }
}

.empty {
  text-align: center;
  padding: 24px 0;
}
.empty-title {
  font-weight: 600;
  margin: 0 0 4px;
  font-size: 0.9rem;
}
.empty-sub {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-muted, #8a7f76);
}

/* Skeleton */
.sk-row {
  display: flex;
  gap: 12px;
  padding: 10px 8px;
  align-items: flex-start;
}
.sk-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
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
.sk-icon-sq {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}
.sk-ln1 {
  height: 11px;
  width: 50%;
}
.sk-ln2 {
  height: 9px;
  width: 70%;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
