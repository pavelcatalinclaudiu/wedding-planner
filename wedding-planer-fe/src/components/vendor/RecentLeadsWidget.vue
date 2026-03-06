<script setup lang="ts">
import { useRouter } from "vue-router";
import type { RecentLeadDTO } from "@/types/vendor.types";

const props = defineProps<{
  leads: RecentLeadDTO[];
  loading?: boolean;
}>();

const router = useRouter();

function openLead() {
  router.push("/vendor/leads");
}

function initials(name: string): string {
  return name
    .split(/[\s&]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((w) => w[0]?.toUpperCase() ?? "")
    .join("");
}

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  return `${Math.floor(hrs / 24)}d ago`;
}

const statusColour: Record<string, string> = {
  ENQUIRY: "#c8a951",
  CONVERSATION: "#1565c0",
  QUOTE_SENT: "#6a1b9a",
  QUOTE_ACCEPTED: "#2e7d32",
  DEPOSIT_PAID: "#00695c",
  CONFIRMED: "#1b5e20",
};
</script>

<template>
  <div class="widget">
    <div class="widget-header">
      <h3>Recent Leads</h3>
      <RouterLink to="/vendor/leads" class="view-all">View all →</RouterLink>
    </div>

    <template v-if="loading">
      <div v-for="i in 3" :key="i" class="sk-row">
        <div class="sk sk-avatar" />
        <div class="sk-lines">
          <div class="sk sk-ln1" />
          <div class="sk sk-ln2" />
        </div>
      </div>
    </template>

    <template v-else-if="leads.length">
      <button
        v-for="lead in leads"
        :key="lead.dealId"
        class="lead-row"
        @click="openLead()"
      >
        <div class="avatar" :title="lead.coupleName">
          {{ initials(lead.coupleName) }}
        </div>
        <div class="lead-info">
          <p class="lead-name">{{ lead.coupleName }}</p>
          <p class="lead-meta">
            <span v-if="lead.weddingDate">{{ lead.weddingDate }}</span>
            <span v-if="lead.weddingDate && lead.location"> · </span>
            <span v-if="lead.location">{{ lead.location }}</span>
          </p>
          <p class="lead-msg" v-if="lead.message">{{ lead.message }}</p>
        </div>
        <div class="lead-right">
          <span class="lead-time">{{ timeAgo(lead.receivedAt) }}</span>
          <span
            class="status-pill"
            :style="{ background: statusColour[lead.status] ?? '#999' }"
            >{{ lead.status.replace("_", " ") }}</span
          >
        </div>
      </button>
    </template>

    <div v-else class="empty">
      <p class="empty-title">No new leads yet</p>
      <p class="empty-sub">Your profile is live — couples will find you soon</p>
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

.lead-row {
  display: flex;
  gap: 12px;
  padding: 10px 8px;
  border: none;
  background: none;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
  text-align: left;
  transition: background 0.12s;
}
.lead-row:hover {
  background: var(--color-surface);
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.lead-info {
  flex: 1;
  min-width: 0;
}
.lead-name {
  margin: 0;
  font-size: 0.85rem;
  font-weight: 600;
}
.lead-meta {
  margin: 1px 0;
  font-size: 0.75rem;
  color: var(--color-muted, #8a7f76);
}
.lead-msg {
  margin: 2px 0 0;
  font-size: 0.73rem;
  color: var(--color-muted, #8a7f76);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.lead-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}
.lead-time {
  font-size: 0.72rem;
  color: var(--color-muted, #8a7f76);
}
.status-pill {
  font-size: 0.62rem;
  font-weight: 600;
  color: #fff;
  padding: 2px 7px;
  border-radius: 10px;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

/* Empty state */
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
.sk-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  flex-shrink: 0;
}
.sk-ln1 {
  height: 11px;
  width: 55%;
}
.sk-ln2 {
  height: 9px;
  width: 75%;
}
@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}
</style>
