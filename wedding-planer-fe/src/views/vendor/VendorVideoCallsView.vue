<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import UpgradeGate from "@/components/ui/UpgradeGate.vue";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { useVendorStore } from "@/stores/vendor.store";
import { useFeatureAccess } from "@/composables/useFeatureAccess";
import VideoCallCard from "@/components/video/VideoCallCard.vue";
import ScheduleCallModal from "@/components/video/ScheduleCallModal.vue";

const { t } = useI18n();
const videoStore = useVideoCallsStore();
const vendorStore = useVendorStore();
const { monetizationEnabled } = useFeatureAccess();

onMounted(() => {
  videoStore.fetchCalls();
  vendorStore.fetchMyProfile();
});

const isFreePlan = computed(
  () => monetizationEnabled.value && vendorStore.profile?.tier === "FREE",
);

const callsUsedThisMonth = computed(() => {
  if (!isFreePlan.value) return 0;
  const now = new Date();
  return videoStore.calls.filter((c) => {
    if (c.status === "CANCELLED") return false;
    const d = new Date(c.scheduledAt);
    return (
      d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear()
    );
  }).length;
});

const monthlyLimitReached = computed(
  () => isFreePlan.value && callsUsedThisMonth.value >= 1,
);

const upcomingCalls = computed(() =>
  videoStore.calls.filter(
    (c) =>
      c.status === "PENDING" ||
      c.status === "SCHEDULED" ||
      c.status === "IN_PROGRESS",
  ),
);

const pastCalls = computed(() =>
  videoStore.calls.filter(
    (c) => c.status === "COMPLETED" || c.status === "CANCELLED",
  ),
);

function formatDate(date: string) {
  return new Date(date).toLocaleString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

async function onCallScheduled() {
  await videoStore.fetchCalls();
}
</script>

<template>
  <UpgradeGate feature="videoCalls">
    <div class="calls-view">
      <div class="page-header">
        <h2>{{ t("videoCalls.title") }}</h2>
        <p class="page-sub">{{ t("videoCalls.subtitle") }}</p>
      </div>

      <!-- FREE plan monthly limit banner -->
      <div
        v-if="isFreePlan"
        class="monthly-limit-banner"
        :class="{ reached: monthlyLimitReached }"
      >
        <span>{{
          t("videoCalls.monthlyUsage", { used: callsUsedThisMonth })
        }}</span>
        <RouterLink
          v-if="monthlyLimitReached"
          to="/vendor/subscription"
          class="upgrade-link"
        >
          {{ t("vendor.subscription.upgrade") }}
        </RouterLink>
      </div>

      <!-- Upcoming / active calls -->
      <section v-if="upcomingCalls.length" class="calls-section">
        <h3 class="section-title">{{ t("videoCalls.upcoming") }}</h3>
        <div class="cards-grid">
          <VideoCallCard
            v-for="call in upcomingCalls"
            :key="call.id"
            :call="call"
            :lead-id="call.leadId"
            :vendor-id="call.vendorId"
            my-role="VENDOR"
          />
        </div>
      </section>

      <!-- Past calls -->
      <section v-if="pastCalls.length" class="calls-section">
        <h3 class="section-title">{{ t("videoCalls.pastCalls") }}</h3>
        <div class="past-list">
          <div v-for="call in pastCalls" :key="call.id" class="past-row">
            <div class="pf-av" :title="call.coupleName ?? 'Couple'">
              <img
                v-if="call.coupleProfilePicture"
                :src="call.coupleProfilePicture"
                class="pf-av-img"
                alt=""
              />
              <template v-else>{{
                (call.coupleName?.[0] ?? "?").toUpperCase()
              }}</template>
            </div>
            <div class="past-info">
              <span class="past-couple">{{ call.coupleName ?? "Couple" }}</span>
              <span class="past-date">{{ formatDate(call.scheduledAt) }}</span>
            </div>
            <span class="call-status" :class="call.status.toLowerCase()">
              {{ call.status }}
            </span>
          </div>
        </div>
      </section>

      <!-- Empty state -->
      <div v-if="!videoStore.calls.length" class="empty">
        <p>{{ t("videoCalls.noCalls") }}</p>
        <p class="empty-hint">
          <RouterLink to="/vendor/leads" class="link">{{
            t("nav.vendor.items.inbox")
          }}</RouterLink>
          — {{ t("videoCalls.goToLeads") }}
        </p>
      </div>

      <!-- Modals -->
      <ScheduleCallModal
        v-if="videoStore.showScheduleModal"
        @close="videoStore.closeScheduleModal()"
        @scheduled="onCallScheduled"
      />
    </div>
  </UpgradeGate>
</template>

<style scoped>
.page-header {
  margin-bottom: 28px;
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.page-sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}

.monthly-limit-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--color-gold-light, #fdf8ee);
  border: 1px solid var(--color-gold);
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-gold);
  margin-bottom: 20px;
}
.monthly-limit-banner.reached {
  background: var(--chip-red-bg, #fef2f2);
  border-color: var(--color-error, #e53935);
  color: var(--color-error, #e53935);
}
.upgrade-link {
  color: inherit;
  font-weight: 700;
  text-decoration: underline;
  white-space: nowrap;
}

.calls-section {
  margin-bottom: 32px;
}
.section-title {
  margin: 0 0 14px;
  font-size: 0.9rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-muted);
}

.cards-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.past-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.past-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 12px 16px;
  gap: 12px;
}
.past-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  flex: 1;
}
.past-couple {
  font-weight: 600;
  font-size: 0.9rem;
}
.past-date {
  font-size: 0.8rem;
  color: var(--color-muted);
}

.pf-av {
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
.pf-av:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}
.pf-av-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.call-status {
  border-radius: 12px;
  padding: 3px 10px;
  font-size: 0.75rem;
  font-weight: 700;
  background: var(--color-surface);
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  white-space: nowrap;
}
.call-status.scheduled {
  background: var(--chip-blue-bg);
  color: var(--chip-blue-text, #4a6cf7);
}
.call-status.in_progress {
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold);
}
.call-status.completed {
  background: var(--chip-green-bg);
  color: var(--color-green);
}
.call-status.cancelled {
  background: var(--chip-red-bg, #fef2f2);
  color: var(--color-error, #e53935);
}

.empty {
  text-align: center;
  color: var(--color-muted);
  padding: 60px 0;
}
.empty p {
  margin: 0 0 8px;
}
.empty-hint {
  font-size: 0.85rem;
}
.link {
  color: var(--color-gold);
  font-weight: 600;
}
</style>
