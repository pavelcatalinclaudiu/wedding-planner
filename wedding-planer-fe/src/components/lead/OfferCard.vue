<script setup lang="ts">
import { computed, ref } from "vue";
import type { Offer } from "@/types/offer.types";

const props = defineProps<{
  offer: Offer;
  myRole: "COUPLE" | "VENDOR";
}>();

const emit = defineEmits<{
  (e: "accept", id: string): void;
  (e: "decline", id: string): void;
}>();

const price = computed(() =>
  new Intl.NumberFormat("en", {
    style: "currency",
    currency: "EUR",
    maximumFractionDigits: 0,
  }).format(props.offer.price),
);

const canAct = computed(
  () => props.myRole === "COUPLE" && props.offer.status === "PENDING",
);

const RESOLVED = ["ACCEPTED", "DECLINED", "EXPIRED", "REVISED"];
const isResolved = computed(() => RESOLVED.includes(props.offer.status));
// Resolved offers start collapsed; pending/active start expanded
const expanded = ref(!RESOLVED.includes(props.offer.status));

const statusColor: Record<string, string> = {
  PENDING: "#856404",
  ACCEPTED: "#155724",
  DECLINED: "#721c24",
  REVISED: "#1a4f8a",
};

const statusBg: Record<string, string> = {
  PENDING: "var(--chip-amber-bg, #fff8e1)",
  ACCEPTED: "var(--chip-green-bg, #e8f5e9)",
  DECLINED: "var(--chip-red-bg, #fce4e4)",
  EXPIRED: "var(--chip-red-bg, #fce4e4)",
  REVISED: "var(--chip-blue-bg, #e3f2fd)",
};
</script>

<template>
  <div
    class="offer-card"
    :class="[`offer-${offer.status.toLowerCase()}`, { collapsed: !expanded }]"
  >
    <!-- Collapsed summary row for resolved offers -->
    <button v-if="!expanded" class="offer-collapsed" @click="expanded = true">
      <span
        class="offer-status-chip"
        :style="{
          color: statusColor[offer.status],
          background: statusBg[offer.status],
        }"
        >{{ offer.status }}</span
      >
      <span class="offer-collapsed-price">{{ price }}</span>
      <span v-if="offer.packageDetails" class="offer-collapsed-details">{{
        offer.packageDetails
      }}</span>
      <span class="offer-chevron">▼</span>
    </button>

    <!-- Expanded full card -->
    <template v-else>
      <div class="offer-header">
        <span class="offer-title">{{ offer.vendorName }}</span>
        <div class="offer-header-right">
          <span
            class="offer-status"
            :style="{ color: statusColor[offer.status] }"
          >
            {{ offer.status }}
          </span>
          <button
            v-if="isResolved"
            class="offer-collapse-btn"
            title="Collapse"
            @click="expanded = false"
          >
            ▲
          </button>
        </div>
      </div>

      <div class="offer-price">{{ price }}</div>

      <p v-if="offer.packageDetails" class="offer-details">
        {{ offer.packageDetails }}
      </p>

      <div class="offer-footer">
        <small v-if="offer.expiresAt" class="expires"
          >Expires: {{ offer.expiresAt }}</small
        >

        <div v-if="canAct" class="offer-actions">
          <button class="btn btn-accept" @click="emit('accept', offer.id)">
            Accept Offer
          </button>
          <button class="btn btn-decline" @click="emit('decline', offer.id)">
            Decline
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.offer-card {
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 20px;
}
.offer-card.collapsed {
  padding: 10px 16px;
  opacity: 0.8;
}
.offer-accepted {
  border-color: #28a745;
}
.offer-declined {
  border-color: #dc3545;
  opacity: 0.7;
}

/* ─── Collapsed row ─── */
.offer-collapsed {
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
.offer-status-chip {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-radius: 20px;
  padding: 2px 8px;
  flex-shrink: 0;
}
.offer-collapsed-price {
  font-weight: 700;
  font-size: 0.88rem;
  color: var(--color-gold);
  flex-shrink: 0;
}
.offer-collapsed-details {
  flex: 1;
  font-size: 0.8rem;
  color: var(--color-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.offer-chevron {
  font-size: 0.65rem;
  color: var(--color-muted);
  flex-shrink: 0;
}

/* ─── Expanded header ─── */
.offer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.offer-header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}
.offer-title {
  font-weight: 600;
  font-size: 0.95rem;
}
.offer-status {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.offer-collapse-btn {
  background: transparent;
  border: none;
  color: var(--color-muted);
  font-size: 0.65rem;
  cursor: pointer;
  padding: 2px 4px;
  line-height: 1;
}
.offer-collapse-btn:hover {
  color: var(--color-text);
}

.offer-price {
  font-size: 1.5rem;
  font-weight: 800;
  color: var(--color-gold);
  margin-bottom: 8px;
}

.offer-details {
  font-size: 0.85rem;
  color: var(--color-text-secondary, #555);
  line-height: 1.6;
  margin-bottom: 12px;
}

.offer-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.expires {
  font-size: 0.75rem;
  color: var(--color-muted);
}

.offer-actions {
  display: flex;
  gap: 8px;
}
.btn {
  padding: 7px 16px;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 600;
  cursor: pointer;
  border: none;
}
.btn-accept {
  background: var(--color-gold);
  color: #fff;
}
.btn-decline {
  background: transparent;
  border: 1.5px solid var(--color-border);
  color: var(--color-muted);
}
.btn-decline:hover {
  border-color: var(--color-error);
  color: var(--color-error);
}
</style>
