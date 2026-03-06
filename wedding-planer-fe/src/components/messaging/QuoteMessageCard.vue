<script setup lang="ts">
import { computed, ref } from "vue";
import type { QuotePayload } from "@/types/message.types";
import { useAuthStore } from "@/stores/auth.store";
import { offersApi } from "@/api/offers.api";
import { useLeadsStore } from "@/stores/leads.store";

const props = defineProps<{ content: string; dealStatus?: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();
const authStore = useAuthStore();
const acting = ref(false);
const error = ref("");

const quote = computed<QuotePayload | null>(() => {
  try {
    return JSON.parse(props.content) as QuotePayload;
  } catch {
    return null;
  }
});

// Resolved quotes start collapsed; pending quotes start expanded
const RESOLVED_STATUSES = ["ACCEPTED", "REJECTED", "EXPIRED"];
const isResolved = computed(() =>
  quote.value ? RESOLVED_STATUSES.includes(quote.value.status) : false,
);
// Initialise collapsed for resolved quotes (message content is immutable so this fires once)
const expanded = ref(
  !RESOLVED_STATUSES.includes(
    (() => {
      try {
        return (JSON.parse(props.content) as QuotePayload).status;
      } catch {
        return "";
      }
    })(),
  ),
);

const isCouple = computed(() => authStore.user?.role === "COUPLE");

/**
 * Show Accept/Decline only when the deal hasn't moved past QUOTE_SENT yet.
 * We rely on the live dealStatus from the thread (passed in as a prop) rather
 * than the status baked into the message JSON, which is always "PENDING".
 */
const canAct = computed(() => {
  if (!isCouple.value) return false;
  // If dealStatus is available, use it as the authoritative source
  if (props.dealStatus) {
    return props.dealStatus === "QUOTED";
  }
  // Fallback: use the embedded status (may be stale)
  return quote.value?.status === "PENDING";
});

const statusLabel: Record<string, string> = {
  PENDING: "Awaiting your response",
  ACCEPTED: "Accepted ✓",
  REJECTED: "Declined",
  EXPIRED: "Expired",
};

async function accept() {
  if (!quote.value) return;
  acting.value = true;
  error.value = "";
  try {
    await offersApi.accept(quote.value.quoteId);
    // Refresh leads store so the BOOKED status is visible in the wedding team view
    try {
      await useLeadsStore().fetchCoupleLeads();
    } catch (_) {}
    emit("refresh");
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? "Failed to accept offer";
  } finally {
    acting.value = false;
  }
}

async function decline() {
  if (!quote.value) return;
  acting.value = true;
  error.value = "";
  try {
    await offersApi.decline(quote.value.quoteId);
    emit("refresh");
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? "Failed to decline offer";
  } finally {
    acting.value = false;
  }
}

function formatPrice(n: number) {
  return n?.toLocaleString(undefined, { minimumFractionDigits: 2 });
}

function formatDate(iso?: string) {
  if (!iso) return null;
  return new Date(iso).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}
</script>

<template>
  <div
    v-if="quote"
    class="qmc"
    :class="[quote.status.toLowerCase(), { collapsed: !expanded }]"
  >
    <!-- Collapsed summary row (click to expand) -->
    <button v-if="!expanded" class="qmc-collapsed" @click="expanded = true">
      <span class="qmc-icon">📋</span>
      <span class="qmc-package">{{
        quote.packageName ?? "Custom Package"
      }}</span>
      <span class="qmc-badge" :class="quote.status.toLowerCase()">
        {{ statusLabel[quote.status] ?? quote.status }}
      </span>
      <span class="qmc-collapsed-price">{{
        formatPrice(quote.totalPrice)
      }}</span>
      <span class="qmc-chevron">▼ Show</span>
    </button>

    <!-- Full expanded card -->
    <template v-else>
      <!-- Header with collapse toggle for resolved offers -->
      <div class="qmc-header">
        <span class="qmc-icon">📋</span>
        <div class="qmc-title-block">
          <p class="qmc-package">{{ quote.packageName ?? "Custom Package" }}</p>
          <span class="qmc-badge" :class="quote.status.toLowerCase()">
            {{ statusLabel[quote.status] ?? quote.status }}
          </span>
        </div>
        <button
          v-if="isResolved"
          class="qmc-collapse-btn"
          @click="expanded = false"
          title="Collapse"
        >
          ▲
        </button>
      </div>

      <!-- Line items -->
      <ul v-if="quote.items?.length" class="qmc-items">
        <li v-for="(item, i) in quote.items" :key="i" class="qmc-item">
          <span class="item-name">{{ item.name }}</span>
          <span class="item-price">{{ formatPrice(item.total) }}</span>
        </li>
      </ul>

      <!-- Totals -->
      <div class="qmc-totals">
        <div class="total-row">
          <span>Total</span>
          <strong>{{ formatPrice(quote.totalPrice) }}</strong>
        </div>
        <div v-if="quote.depositAmount" class="total-row deposit">
          <span>Deposit</span>
          <span>{{ formatPrice(quote.depositAmount) }}</span>
        </div>
      </div>

      <!-- Expiry -->
      <p v-if="quote.expiresAt" class="qmc-expiry">
        Expires: {{ formatDate(quote.expiresAt) }}
      </p>

      <!-- Actions (couple only, when quote can still be acted upon) -->
      <div v-if="canAct" class="qmc-actions">
        <p v-if="error" class="qmc-error">{{ error }}</p>
        <button class="btn-accept" :disabled="acting" @click="accept">
          Accept Quote
        </button>
        <button class="btn-decline" :disabled="acting" @click="decline">
          Decline
        </button>
      </div>
    </template>
  </div>

  <!-- Fallback for malformed JSON -->
  <div v-else class="qmc-fallback">
    <em>Quote (could not load details)</em>
  </div>
</template>

<style scoped>
.qmc {
  background: var(--color-white);
  border: 1.5px solid var(--color-gold);
  border-radius: 12px;
  padding: 16px 18px;
  width: 100%;
  max-width: 360px;
}
.qmc.accepted {
  border-color: var(--color-green);
}
.qmc.rejected,
.qmc.expired {
  opacity: 0.65;
}

/* ─── Collapsed (single-row) state ─── */
.qmc.collapsed {
  padding: 10px 14px;
  opacity: 0.75;
}
.qmc-collapsed {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  background: transparent;
  border: none;
  padding: 0;
  cursor: pointer;
  text-align: left;
}
.qmc-collapsed .qmc-icon {
  font-size: 1rem;
  flex-shrink: 0;
}
.qmc-collapsed .qmc-package {
  flex: 1;
  font-weight: 600;
  font-size: 0.83rem;
  color: var(--color-text);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.qmc-collapsed-price {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-gold);
  flex-shrink: 0;
}
.qmc-chevron {
  font-size: 0.7rem;
  color: var(--color-muted);
  flex-shrink: 0;
  white-space: nowrap;
}

/* ─── Header (expanded) ─── */
.qmc-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
}
.qmc-icon {
  font-size: 1.4rem;
  line-height: 1;
}
.qmc-title-block {
  flex: 1;
}
.qmc-package {
  margin: 0 0 4px;
  font-weight: 700;
  font-size: 0.92rem;
}
.qmc-collapse-btn {
  background: transparent;
  border: none;
  color: var(--color-muted);
  font-size: 0.72rem;
  cursor: pointer;
  padding: 2px 4px;
  flex-shrink: 0;
  line-height: 1;
}
.qmc-collapse-btn:hover {
  color: var(--color-text);
}
.qmc-badge {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 20px;
  padding: 2px 9px;
  background: var(--color-surface);
  color: var(--color-muted);
}
.qmc-badge.pending {
  background: var(--chip-amber-bg);
  color: var(--color-gold);
}
.qmc-badge.accepted {
  background: var(--chip-green-bg);
  color: var(--color-green);
}
.qmc-badge.rejected,
.qmc-badge.expired {
  background: var(--chip-red-bg);
  color: var(--color-error);
}

.qmc-items {
  list-style: none;
  margin: 0 0 10px;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.qmc-item {
  display: flex;
  justify-content: space-between;
  font-size: 0.83rem;
}
.item-name {
  color: var(--color-text);
}
.item-price {
  font-weight: 600;
}

.qmc-totals {
  border-top: 1px solid var(--color-border);
  padding-top: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 8px;
}
.total-row {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
}
.total-row strong {
  color: var(--color-gold);
  font-size: 1rem;
}
.total-row.deposit {
  font-size: 0.82rem;
  color: var(--color-muted);
}

.qmc-expiry {
  margin: 0 0 10px;
  font-size: 0.75rem;
  color: var(--color-muted);
}

.qmc-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.qmc-error {
  width: 100%;
  font-size: 0.78rem;
  color: var(--color-error);
  margin: 0;
}
.btn-accept {
  flex: 1;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 0;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-accept:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-decline {
  flex: 1;
  background: transparent;
  color: var(--color-muted);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 0;
  font-size: 0.85rem;
  cursor: pointer;
}
.btn-decline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.qmc-fallback {
  color: var(--color-muted);
  font-size: 0.85rem;
  padding: 8px;
}
</style>
