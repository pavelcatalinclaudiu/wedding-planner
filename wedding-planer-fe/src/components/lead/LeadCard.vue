<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { Lead } from "@/types/lead.types";
import LeadStatusBadge from "./LeadStatusBadge.vue";
import { Calendar, Euro } from "lucide-vue-next";

const { t, locale } = useI18n();

const props = defineProps<{
  lead: Lead;
  role: "VENDOR" | "COUPLE";
}>();

const emit = defineEmits<{
  (e: "accept", id: string): void;
  (e: "decline", id: string): void;
  (e: "open", lead: Lead): void;
}>();

const budget = computed(() =>
  props.lead.budget
    ? new Intl.NumberFormat(locale.value === "ro" ? "ro-RO" : "en", {
        maximumFractionDigits: 0,
      }).format(props.lead.budget) + " EUR"
    : null,
);

const formattedEventDate = computed(() => {
  if (!props.lead.eventDate) return null;
  return new Date(props.lead.eventDate).toLocaleDateString(locale.value, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
});

const canAct = computed(
  () => props.role === "VENDOR" && props.lead.status === "NEW",
);
</script>

<template>
  <div class="lead-card" @click="emit('open', lead)">
    <div class="lead-header">
      <div class="lead-identity">
        <div class="lc-avatar">
          <img
            v-if="
              role === 'VENDOR'
                ? lead.coupleProfilePicture
                : lead.vendorProfilePicture
            "
            :src="
              role === 'VENDOR'
                ? lead.coupleProfilePicture
                : lead.vendorProfilePicture
            "
            class="lc-avatar-img"
            alt=""
          />
          <template v-else>{{
            role === "VENDOR"
              ? (lead.coupleName?.[0] ?? "?").toUpperCase()
              : (lead.vendorName?.[0] ?? "?").toUpperCase()
          }}</template>
        </div>
        <div class="lead-name">
          {{ role === "VENDOR" ? lead.coupleName : lead.vendorName }}
        </div>
      </div>
      <LeadStatusBadge :status="lead.status" :perspective="role" />
    </div>

    <div class="lead-meta">
      <span v-if="formattedEventDate"
        ><Calendar :size="13" /> {{ formattedEventDate }}</span
      >
      <span v-if="budget"><Euro :size="13" /> {{ budget }}</span>
      <span v-if="role === 'COUPLE'">{{ lead.vendorCategory }}</span>
    </div>

    <p v-if="lead.message" class="lead-message">{{ lead.message }}</p>

    <div v-if="canAct" class="lead-actions" @click.stop>
      <button class="btn btn-accept" @click="emit('accept', lead.id)">
        {{ t("leads.cardAccept") }}
      </button>
      <button class="btn btn-decline" @click="emit('decline', lead.id)">
        {{ t("leads.cardDecline") }}
      </button>
    </div>

    <div v-else class="lead-footer">
      <small class="card-date">{{
        new Date(lead.createdAt).toLocaleDateString()
      }}</small>
      <span
        v-if="['IN_DISCUSSION', 'QUOTED'].includes(lead.status)"
        class="chat-hint"
        >{{ t("leads.openChat") }}</span
      >
    </div>
  </div>
</template>

<style scoped>
.lead-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 20px;
  cursor: pointer;
  transition:
    box-shadow 0.2s,
    border-color 0.2s;
}
.lead-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: var(--color-gold);
}

.lead-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.lead-identity {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}
.lc-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
  transition:
    transform 0.18s,
    box-shadow 0.18s;
}
.lc-avatar:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}
.lc-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.lead-name {
  font-weight: 600;
  font-size: 0.95rem;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lead-meta {
  display: flex;
  gap: 12px;
  font-size: 0.8rem;
  color: var(--color-muted);
  margin-bottom: 8px;
}

.lead-message {
  font-size: 0.85rem;
  color: var(--color-text-secondary, #555);
  line-height: 1.5;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 12px;
}

.lead-actions {
  display: flex;
  gap: 8px;
}

.btn {
  padding: 6px 16px;
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

.lead-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-date {
  font-size: 0.75rem;
  color: var(--color-muted);
}
.chat-hint {
  font-size: 0.8rem;
  color: var(--color-gold);
  font-weight: 500;
}
.lead-card.selected .chat-hint {
  display: none;
}
</style>
