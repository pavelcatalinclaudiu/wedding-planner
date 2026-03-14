<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useLeads } from "@/composables/useLeads";
import { useConversation } from "@/composables/useConversation";
import { useOffer } from "@/composables/useOffer";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { useBudgetStore } from "@/stores/budget.store";
import { conversationsApi } from "@/api/conversations.api";
import LeadCard from "@/components/lead/LeadCard.vue";
import ConversationThread from "@/components/lead/ConversationThread.vue";
import OfferCard from "@/components/lead/OfferCard.vue";
import CallBanner from "@/components/video/CallBanner.vue";
import ScheduleCallModal from "@/components/video/ScheduleCallModal.vue";
import type { Lead } from "@/types/lead.types";
import { MessageSquare, ClipboardList, ChevronLeft } from "lucide-vue-next";
import { Calendar } from "lucide-vue-next";

const { t } = useI18n();

const { leads, loading, fetchCoupleLeads } = useLeads();
const { conversation, messages, sending, loadForLead, send } =
  useConversation();
const {
  offers,
  loadForLead: loadOffers,
  accept: acceptOffer,
  decline: declineOffer,
  requestRevision,
  markViewed,
} = useOffer({ onLeadUpdated: () => fetchCoupleLeads() });

const selectedLead = ref<Lead | null>(null);
const activeTab = ref<"chat" | "offers">("chat");
const showDetail = ref(false);
const videoStore = useVideoCallsStore();
const budgetStore = useBudgetStore();
const route = useRoute();

const activeCall = computed(() =>
  selectedLead.value
    ? (videoStore.liveCallByLead[selectedLead.value.id] ?? null)
    : null,
);

const latestOffer = computed(() => offers.value[0] ?? null);
const hasPendingOffer = computed(() =>
  offers.value.some((o) => o.status === "PENDING"),
);
const hasAcceptedOffer = computed(() =>
  offers.value.some((o) => o.status === "ACCEPTED"),
);
// Revision was requested when the latest offer is REVISED and no new pending
// has arrived yet — derived from server data, no local state needed.
const revisionRequested = computed(
  () => latestOffer.value?.status === "REVISED" && !hasPendingOffer.value,
);

function requestOfferLabel() {
  if (revisionRequested.value) return t("leads.revisionRequested");
  return hasPendingOffer.value
    ? t("leads.requestDifferentOffer")
    : t("leads.requestNewOffer");
}

function formatOfferPrice(price: number) {
  return "\u20AC" + Math.round(price).toLocaleString();
}

onMounted(async () => {
  await fetchCoupleLeads();
  await budgetStore.fetchBudget();
  await handleDeepLink();
});

/** Re-run when already on this page and a notification changes the query params. */
watch(
  () => route.query,
  async (q, prev) => {
    if (q.lead !== prev?.lead || q.conversation !== prev?.conversation) {
      await handleDeepLink();
    }
  },
);

async function handleDeepLink() {
  const leadId = route.query.lead as string | undefined;
  const convId = route.query.conversation as string | undefined;
  if (leadId) {
    const lead = leads.value.find((l) => l.id === leadId);
    if (lead) await openLead(lead);
  } else if (convId) {
    try {
      const convs = (await conversationsApi.listMine()).data;
      const conv = convs.find((c) => c.id === convId);
      if (conv) {
        const lead = leads.value.find((l) => l.id === conv.leadId);
        if (lead) await openLead(lead);
      }
    } catch {
      /* ignore — just land on the page without auto-opening a thread */
    }
  }
}

function closeDetail() {
  showDetail.value = false;
  selectedLead.value = null;
}

async function openLead(lead: Lead) {
  selectedLead.value = lead;
  activeTab.value = "chat";
  showDetail.value = true;
  const tasks: Promise<unknown>[] = [loadForLead(lead.id), loadOffers(lead.id)];
  if (["IN_DISCUSSION", "QUOTED", "BOOKED"].includes(lead.status)) {
    tasks.push(videoStore.fetchActiveForLead(lead.id));
  }
  // Refresh budget when opening a cancelled lead so the removed item disappears
  if (lead.status === "CANCELLED") {
    tasks.push(budgetStore.fetchBudget());
  }
  await Promise.all(tasks);
  // Auto-mark pending offer as viewed since couple is now looking at it
  const pending = offers.value.find(
    (o) => o.status === "PENDING" && !o.viewedAt,
  );
  if (pending) markViewed(pending.id);
}

// When a new PENDING offer arrives via WS while the couple is already viewing this lead, auto-mark it viewed
watch(latestOffer, (offer) => {
  if (
    offer &&
    offer.status === "PENDING" &&
    !offer.viewedAt &&
    selectedLead.value
  ) {
    markViewed(offer.id);
  }
});

async function sendMessage(content: string) {
  await send(content);
}

async function handleAccept(offerId: string) {
  await acceptOffer(offerId);
}

async function handleDecline(offerId: string) {
  await declineOffer(offerId);
}

async function requestNewOffer() {
  const pending = offers.value.find((o) => o.status === "PENDING");
  if (pending) {
    // Mark offer REVISED — backend automatically posts the system event message in chat
    await requestRevision(pending.id);
  } else {
    // No existing offer yet — send a plain text to initiate
    await send(t("leads.requestOfferMessage"));
  }
  activeTab.value = "chat";
}

function openScheduleModal() {
  if (!selectedLead.value) return;
  videoStore.openScheduleModal(
    selectedLead.value.id,
    selectedLead.value.vendorId,
  );
}

async function onCallScheduled(leadId: string) {
  await videoStore.fetchActiveForLead(leadId);
}
</script>

<template>
  <div class="couple-leads-layout">
    <!-- Sidebar list -->
    <aside class="lead-panel">
      <div class="panel-header">
        <div>
          <h2>{{ t("leads.title") }}</h2>
          <p class="panel-sub">{{ t("leads.subtitle") }}</p>
        </div>
      </div>

      <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
      <div v-else-if="leads.length === 0" class="empty">
        {{ t("leads.noLeads") }}
      </div>
      <div v-else class="lead-list">
        <LeadCard
          v-for="lead in leads"
          :key="lead.id"
          :lead="lead"
          role="COUPLE"
          :class="{ selected: selectedLead?.id === lead.id }"
          @open="openLead"
        />
      </div>
    </aside>

    <!-- Detail -->
    <main class="detail-panel" :class="{ 'detail-visible': showDetail }">
      <template v-if="!selectedLead">
        <div class="placeholder">
          <p>{{ t("leads.selectEnquiry") }}</p>
        </div>
      </template>

      <template v-else>
        <div class="detail-header">
          <button class="mobile-back" @click="closeDetail">
            <ChevronLeft :size="18" /> {{ t("leads.title") }}
          </button>
          <div class="detail-title">
            <div class="dh-avatar">
              <img
                v-if="selectedLead.vendorProfilePicture"
                :src="selectedLead.vendorProfilePicture"
                class="dh-avatar-img"
                alt=""
              />
              <template v-else>{{
                (selectedLead.vendorName?.[0] ?? "?").toUpperCase()
              }}</template>
            </div>
            <div>
              <h3>{{ selectedLead.vendorName }}</h3>
              <small
                >{{ selectedLead.vendorCategory }} ·
                {{ selectedLead.eventDate }}</small
              >
            </div>
          </div>
          <div
            v-if="
              ['IN_DISCUSSION', 'QUOTED', 'BOOKED'].includes(
                selectedLead.status,
              )
            "
            class="detail-actions"
          >
            <button
              v-if="
                !activeCall ||
                activeCall.status === 'CANCELLED' ||
                activeCall.status === 'COMPLETED'
              "
              class="btn-video"
              @click="openScheduleModal"
            >
              <Calendar :size="15" /> {{ t("leads.scheduleCall") }}
            </button>
            <CallBanner
              v-else
              :call="activeCall"
              calls-route="/couple/calls"
              my-role="COUPLE"
            />
          </div>
        </div>

        <!-- Tab bar (only once vendor has engaged — past NEW/VIEWED) -->
        <div
          v-if="
            conversation && !['NEW', 'VIEWED'].includes(selectedLead.status)
          "
          class="detail-tabs"
        >
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'chat' }"
            @click="activeTab = 'chat'"
          >
            <MessageSquare :size="14" /> {{ t("leads.chatTab") }}
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'offers' }"
            @click="activeTab = 'offers'"
          >
            <ClipboardList :size="14" /> {{ t("leads.offersTab") }}
            <span
              v-if="offers.filter((o) => o.status === 'PENDING').length"
              class="tab-badge"
            >
              {{ offers.filter((o) => o.status === "PENDING").length }}
            </span>
          </button>
        </div>

        <!-- Pending (vendor hasn't engaged yet) -->
        <div
          v-if="
            selectedLead.status === 'NEW' || selectedLead.status === 'VIEWED'
          "
          class="pending-notice"
        >
          <p>{{ t("leads.waitingForVendor") }}</p>
        </div>

        <!-- Chat tab -->
        <template v-else-if="activeTab === 'chat'">
          <div
            v-if="latestOffer"
            class="offer-banner"
            :class="latestOffer.status.toLowerCase()"
          >
            <div class="offer-banner-info">
              <span class="offer-banner-label">{{
                t("leads.latestOffer")
              }}</span>
              <strong class="offer-banner-price">{{
                formatOfferPrice(latestOffer.price)
              }}</strong>
              <span
                class="offer-banner-status"
                :class="latestOffer.status.toLowerCase()"
              >
                {{ latestOffer.status }}
              </span>
            </div>
            <div class="offer-banner-actions">
              <button class="offer-banner-link" @click="activeTab = 'offers'">
                {{ t("leads.viewAll") }} →
              </button>
            </div>
          </div>
          <div v-if="conversation" class="conv-section">
            <ConversationThread
              :messages="messages"
              my-role="COUPLE"
              :sending="sending"
              @send="sendMessage"
            />
          </div>
        </template>

        <!-- Offers tab -->
        <template v-else-if="activeTab === 'offers'">
          <div class="offers-panel">
            <div class="offers-panel-header">
              <h4>{{ t("leads.offersFromVendor") }}</h4>
              <button
                v-if="conversation && !hasAcceptedOffer"
                class="btn-request-offer"
                :class="{ requested: revisionRequested }"
                :disabled="revisionRequested"
                @click="requestNewOffer"
              >
                {{ requestOfferLabel() }}
              </button>
            </div>
            <div v-if="!offers.length" class="offers-empty">
              {{ t("leads.noOffers") }}
            </div>
            <div class="offers-list">
              <OfferCard
                v-for="offer in offers"
                :key="offer.id"
                :offer="offer"
                my-role="COUPLE"
                @accept="handleAccept"
                @decline="handleDecline"
              />
            </div>
          </div>
        </template>
      </template>
    </main>
  </div>

  <!-- Modals -->
  <ScheduleCallModal
    v-if="videoStore.showScheduleModal"
    @close="videoStore.closeScheduleModal()"
    @scheduled="onCallScheduled"
  />
</template>

<style scoped>
.couple-leads-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  height: calc(100vh - 116px);
  overflow: hidden;
  position: relative;
}

.lead-panel {
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 20px 20px 12px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 1;
  background: var(--color-surface);
}
.panel-header h2 {
  margin: 0 0 4px;
  font-size: 1.1rem;
  color: var(--color-text);
}
.panel-sub {
  margin: 0;
  font-size: 0.82rem;
  color: var(--color-muted);
  line-height: 1.4;
}
.lead-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.lead-list :deep(.lead-card).selected {
  border-color: var(--color-gold);
  background: var(--color-gold-light, #fdf8ee);
}
.loading,
.empty {
  padding: 24px;
  text-align: center;
  color: var(--color-muted);
}

.detail-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted);
}
.detail-header {
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.detail-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.dh-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 0.9rem;
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
.dh-avatar:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}
.dh-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.detail-header h3 {
  margin: 0 0 4px;
}
.detail-header small {
  color: var(--color-muted);
}
.detail-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.btn-video {
  background: #2d6cdf;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.85rem;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}
.btn-video:hover:not(:disabled) {
  background: #2459c4;
}
.btn-video:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ─── Tabs ─── */
.detail-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  background: var(--color-surface);
}
.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-muted);
  cursor: pointer;
  margin-bottom: -1px;
}
.tab-btn.active {
  color: var(--color-gold);
  border-bottom-color: var(--color-gold);
}
.tab-btn:hover:not(.active) {
  color: var(--color-text);
}
.tab-badge {
  background: var(--color-gold);
  color: #fff;
  border-radius: 20px;
  font-size: 0.68rem;
  font-weight: 700;
  padding: 1px 6px;
  line-height: 1.4;
}

/* ─── Latest offer banner (chat tab) ─── */
.offer-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 20px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}
.offer-banner-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.offer-banner-label {
  font-size: 0.78rem;
  color: var(--color-muted);
}
.offer-banner-price {
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--color-gold);
}
.offer-banner-status {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  border-radius: 20px;
  padding: 2px 8px;
  background: var(--chip-amber-bg, #fff8e1);
  color: var(--color-gold);
}
.offer-banner-status.accepted {
  background: var(--chip-green-bg, #e8f5e9);
  color: var(--color-green);
}
.offer-banner-status.declined,
.offer-banner-status.expired {
  background: var(--chip-red-bg, #fce4e4);
  color: var(--color-error);
}
.offer-banner-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.offer-banner-request {
  background: transparent;
  border: 1px solid var(--color-gold);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 0.8rem;
  color: var(--color-gold);
  cursor: pointer;
  white-space: nowrap;
  font-weight: 600;
}
.offer-banner-request:hover:not(:disabled) {
  background: var(--color-gold-light, #fdf8ee);
}
.offer-banner-request.requested,
.offer-banner-request:disabled {
  border-color: var(--color-muted);
  color: var(--color-muted);
  cursor: default;
  opacity: 0.75;
}
.offer-banner-link {
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 0.8rem;
  color: var(--color-gold);
  cursor: pointer;
  white-space: nowrap;
  font-weight: 600;
}
.offer-banner-link:hover {
  background: var(--color-gold-light, #fdf8ee);
}

/* ─── Offers panel (offers tab) ─── */
.offers-panel {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.offers-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-shrink: 0;
}
.offers-panel-header h4 {
  margin: 0;
}
.btn-request-offer {
  background: var(--color-surface);
  border: 1.5px solid var(--color-gold);
  color: var(--color-gold);
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.btn-request-offer:hover:not(:disabled) {
  background: var(--color-gold-light, #fdf8ee);
}
.btn-request-offer.requested,
.btn-request-offer:disabled {
  border-color: var(--color-muted);
  color: var(--color-muted);
  cursor: default;
  opacity: 0.75;
}
.offers-empty {
  color: var(--color-muted);
  font-size: 0.88rem;
  padding: 20px 0;
  text-align: center;
}
.offers-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* ─── Chat section ─── */
.conv-section {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.pending-notice {
  padding: 24px;
  color: var(--color-muted);
  font-style: italic;
}

/* ─── Mobile back button (hidden on desktop) ─── */
.mobile-back {
  display: none;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-gold);
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
  width: 100%;
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .couple-leads-layout {
    grid-template-columns: 1fr;
  }
  .lead-panel {
    height: 100%;
  }
  .detail-panel {
    position: absolute;
    inset: 0;
    background: var(--color-surface);
    transform: translateX(100%);
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 10;
  }
  .detail-panel.detail-visible {
    transform: translateX(0);
  }
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    padding: 0;
    margin-bottom: 10px;
  }
  .mobile-back {
    display: flex;
  }
  .detail-actions {
    width: 100%;
  }
  .btn-video {
    width: 100%;
    display: flex;
    justify-content: center;
    gap: 5px;
  }
}
</style>
