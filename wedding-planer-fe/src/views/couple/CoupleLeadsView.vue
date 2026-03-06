<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useLeads } from "@/composables/useLeads";
import { useConversation } from "@/composables/useConversation";
import { useOffer } from "@/composables/useOffer";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { conversationsApi } from "@/api/conversations.api";
import LeadCard from "@/components/lead/LeadCard.vue";
import ConversationThread from "@/components/lead/ConversationThread.vue";
import OfferCard from "@/components/lead/OfferCard.vue";
import CallBanner from "@/components/video/CallBanner.vue";
import ScheduleCallModal from "@/components/video/ScheduleCallModal.vue";
import type { Lead } from "@/types/lead.types";

const { leads, loading, fetchCoupleLeads } = useLeads();
const { conversation, messages, sending, loadForLead, send } =
  useConversation();
const {
  offers,
  loadForLead: loadOffers,
  accept: acceptOffer,
  decline: declineOffer,
  requestRevision,
} = useOffer({ onLeadUpdated: () => fetchCoupleLeads() });

const selectedLead = ref<Lead | null>(null);
const activeTab = ref<"chat" | "offers">("chat");
const videoStore = useVideoCallsStore();
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
  if (revisionRequested.value) return "✓ Revision Requested";
  return hasPendingOffer.value
    ? "Request Different Offer"
    : "Request New Offer";
}

function formatOfferPrice(price: number) {
  return new Intl.NumberFormat("en-GB", {
    style: "currency",
    currency: "GBP",
  }).format(price);
}

onMounted(async () => {
  await fetchCoupleLeads();
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

async function openLead(lead: Lead) {
  selectedLead.value = lead;
  activeTab.value = "chat";
  const tasks: Promise<unknown>[] = [loadForLead(lead.id), loadOffers(lead.id)];
  if (["IN_DISCUSSION", "QUOTED", "BOOKED"].includes(lead.status)) {
    tasks.push(videoStore.fetchActiveForLead(lead.id));
  }
  await Promise.all(tasks);
}

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
    await send("Could you please send me an offer?");
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
        <h2>My Enquiries</h2>
      </div>

      <div v-if="loading" class="loading">Loading…</div>
      <div v-else-if="leads.length === 0" class="empty">
        You haven't contacted any vendors yet.
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
    <main class="detail-panel">
      <template v-if="!selectedLead">
        <div class="placeholder">
          <p>Select an enquiry to view the conversation and offers.</p>
        </div>
      </template>

      <template v-else>
        <div class="detail-header">
          <div>
            <h3>{{ selectedLead.vendorName }}</h3>
            <small
              >{{ selectedLead.vendorCategory }} ·
              {{ selectedLead.eventDate }}</small
            >
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
              📅 Schedule Call
            </button>
            <CallBanner
              v-else
              :call="activeCall"
              calls-route="/couple/calls"
              my-role="COUPLE"
            />
          </div>
        </div>

        <!-- Tab bar (show when conversation exists, regardless of offers) -->
        <div v-if="conversation" class="detail-tabs">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'chat' }"
            @click="activeTab = 'chat'"
          >
            💬 Chat
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'offers' }"
            @click="activeTab = 'offers'"
          >
            📋 Offers
            <span v-if="offers.length" class="tab-badge">{{
              offers.length
            }}</span>
          </button>
        </div>

        <!-- Pending (no tabs yet) -->
        <div
          v-if="
            !conversation &&
            (selectedLead.status === 'NEW' || selectedLead.status === 'VIEWED')
          "
          class="pending-notice"
        >
          <p>Your enquiry is waiting for the vendor to respond.</p>
        </div>

        <!-- Chat tab -->
        <template v-else-if="activeTab === 'chat'">
          <div
            v-if="latestOffer"
            class="offer-banner"
            :class="latestOffer.status.toLowerCase()"
          >
            <div class="offer-banner-info">
              <span class="offer-banner-label">Latest offer</span>
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
              <button
                v-if="!hasAcceptedOffer"
                class="offer-banner-request"
                :class="{ requested: revisionRequested }"
                :disabled="revisionRequested"
                @click="requestNewOffer"
              >
                {{ requestOfferLabel() }}
              </button>
              <button class="offer-banner-link" @click="activeTab = 'offers'">
                View all →
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
              <h4>Offers from vendor</h4>
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
              No offers sent yet.
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
}
.panel-header h2 {
  margin: 0;
  font-size: 1.1rem;
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
</style>
