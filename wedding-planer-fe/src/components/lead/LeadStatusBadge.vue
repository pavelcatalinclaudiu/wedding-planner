<script setup lang="ts">
import { computed } from "vue";
import { useI18n } from "vue-i18n";
import type { LeadStatus } from "@/types/lead.types";

const { t } = useI18n();

const props = defineProps<{
  status: LeadStatus;
  perspective?: "COUPLE" | "VENDOR";
}>();

const label = computed(() => {
  if (props.perspective === "COUPLE" && props.status === "QUOTED") {
    return t("leads.statuses.QUOTED_COUPLE");
  }
  return t(`leads.statuses.${props.status}`, props.status);
});
</script>

<template>
  <span class="lead-badge" :class="`status-${status.toLowerCase()}`">
    {{ label }}
  </span>
</template>

<style scoped>
.lead-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}
.status-new {
  background: #e8f4fd;
  color: #1a73e8;
}
.status-viewed {
  background: #f0e8ff;
  color: #7b2ff7;
}
.status-in_discussion {
  background: #fff3cd;
  color: #856404;
}
.status-quoted {
  background: #d1ecf1;
  color: #0c5460;
}
.status-booked {
  background: #d4edda;
  color: #155724;
}
.status-declined {
  background: #f8d7da;
  color: #721c24;
}
.status-cancelled {
  background: #e2e3e5;
  color: #383d41;
}
</style>
