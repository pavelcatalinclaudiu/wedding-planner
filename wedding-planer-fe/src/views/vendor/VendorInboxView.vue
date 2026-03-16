<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useLeads } from "@/composables/useLeads";
import { useConversation } from "@/composables/useConversation";
import { useOffer } from "@/composables/useOffer";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { conversationsApi } from "@/api/conversations.api";
import { leadsApi } from "@/api/leads.api";
import { useFeatureAccess } from "@/composables/useFeatureAccess";
import LeadCard from "@/components/lead/LeadCard.vue";
import ConversationThread from "@/components/lead/ConversationThread.vue";
import OfferCard from "@/components/lead/OfferCard.vue";
import CallBanner from "@/components/video/CallBanner.vue";
import ScheduleCallModal from "@/components/video/ScheduleCallModal.vue";
import type { Lead } from "@/types/lead.types";
import {
  MessageSquare,
  ClipboardList,
  Lock,
  ChevronLeft,
} from "lucide-vue-next";
import { Calendar } from "lucide-vue-next";

const { t } = useI18n();
const { leads, loading, fetchVendorLeads, accept, decline } = useLeads();
const { canAccess } = useFeatureAccess();
const exportingCsv = ref(false);
const { conversation, messages, sending, loadForLead, send } =
  useConversation();
const { offers, loadForLead: loadOffers, createOffer } = useOffer();
const videoStore = useVideoCallsStore();

const selectedLead = ref<Lead | null>(null);
const activeTab = ref<"chat" | "offers">("chat");
const statusFilter = ref("");
const showDetail = ref(false);
const showOfferForm = ref(false);
const offerFormData = ref({ packageDetails: "", price: "", expiresAt: "" });
const route = useRoute();

const activeCall = computed(() =>
  selectedLead.value
    ? (videoStore.liveCallByLead[selectedLead.value.id] ?? null)
    : null,
);

const latestOffer = computed(() => offers.value[0] ?? null);
const canSendNewOffer = computed(() => {
  if (!offers.value.length) return true;
  const now = new Date();
  return !offers.value.some((o) => {
    if (o.status !== "PENDING") return false;
    if (!o.expiresAt) return true; // pending with no expiry = still active
    return new Date(o.expiresAt) > now;
  });
});
const sendBlockReason = computed(() => {
  if (canSendNewOffer.value) return null;
  const pending = offers.value.find((o) => {
    if (o.status !== "PENDING") return false;
    if (!o.expiresAt) return true;
    return new Date(o.expiresAt) > new Date();
  });
  if (!pending) return null;
  if (!pending.expiresAt) return t("leads.pendingOfferAwaiting");
  return `${t("leads.pendingOfferAwaiting")} ${new Date(pending.expiresAt).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" })}.`;
});

function formatOfferPrice(price: number) {
  return "\u20AC" + Math.round(price).toLocaleString();
}

const filtered = computed(() => {
  if (!statusFilter.value) return leads.value;
  return leads.value.filter((l) => l.status === statusFilter.value);
});

onMounted(async () => {
  await fetchVendorLeads();
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
  showOfferForm.value = false;
  const tasks: Promise<unknown>[] = [];
  if (["IN_DISCUSSION", "QUOTED", "BOOKED"].includes(lead.status)) {
    tasks.push(loadForLead(lead.id), loadOffers(lead.id));
    tasks.push(videoStore.fetchActiveForLead(lead.id));
  } else if (["CANCELLED", "DECLINED"].includes(lead.status)) {
    // Load chat history and offers (read-only) for terminal leads
    tasks.push(loadForLead(lead.id), loadOffers(lead.id));
  }
  await Promise.all(tasks);
}

async function onAccept(id: string) {
  const updated = await accept(id);
  if (selectedLead.value?.id === id) selectedLead.value = updated;
  await loadForLead(id);
}

async function onDecline(id: string) {
  const updated = await decline(id);
  if (selectedLead.value?.id === id) selectedLead.value = updated;
}

async function sendMessage(content: string) {
  await send(content);
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

async function submitOffer() {
  if (!selectedLead.value) return;
  await createOffer({
    leadId: selectedLead.value.id,
    conversationId: conversation.value?.id,
    packageDetails: offerFormData.value.packageDetails,
    price: parseFloat(offerFormData.value.price),
    expiresAt: offerFormData.value.expiresAt || undefined,
  });
  showOfferForm.value = false;
  offerFormData.value = { packageDetails: "", price: "", expiresAt: "" };
}

async function downloadCsv() {
  exportingCsv.value = true;
  try {
    const res = await leadsApi.exportCsv();
    const url = URL.createObjectURL(
      new Blob([res.data as unknown as BlobPart], { type: "text/csv" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = "leads.csv";
    a.click();
    URL.revokeObjectURL(url);
  } finally {
    exportingCsv.value = false;
  }
}
</script>

<template>
  <div class="inbox-layout">
    <!-- Lead List Panel -->
    <aside class="lead-panel">
      <div class="panel-header">
        <div>
          <h2>{{ t("leads.vendor.title") }}</h2>
          <p class="panel-sub">{{ t("leads.vendor.subtitle") }}</p>
        </div>
        <div class="panel-header-actions">
          <select v-model="statusFilter" class="filter-select">
            <option value="">{{ t("leads.filterAll") }}</option>
            <option value="NEW">{{ t("leads.filterNew") }}</option>
            <option value="VIEWED">{{ t("leads.filterViewed") }}</option>
            <option value="IN_DISCUSSION">
              {{ t("leads.filterInDiscussion") }}
            </option>
            <option value="QUOTED">{{ t("leads.filterOfferSent") }}</option>
            <option value="BOOKED">{{ t("leads.filterBooked") }}</option>
            <option value="DECLINED">{{ t("leads.filterDeclined") }}</option>
          </select>
          <button
            v-if="canAccess('leadExport')"
            class="btn-export-csv"
            :disabled="exportingCsv"
            @click="downloadCsv"
          >
            {{ exportingCsv ? t("common.loading") : t("leads.exportCsv") }}
          </button>
        </div>
      </div>

      <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
      <div v-else-if="filtered.length === 0" class="empty">
        {{ t("leads.noLeads") }}
      </div>
      <div v-else class="lead-list">
        <LeadCard
          v-for="lead in filtered"
          :key="lead.id"
          :lead="lead"
          role="VENDOR"
          :class="{ selected: selectedLead?.id === lead.id }"
          @accept="onAccept"
          @decline="onDecline"
          @open="openLead"
        />
      </div>
    </aside>

    <!-- Detail Panel -->
    <main class="detail-panel" :class="{ 'detail-visible': showDetail }">
      <template v-if="!selectedLead">
        <div class="placeholder">
          <p>{{ t("leads.selectLead") }}</p>
        </div>
      </template>

      <template v-else>
        <div class="detail-header">
          <button class="mobile-back" @click="closeDetail">
            <ChevronLeft :size="18" /> {{ t("leads.vendor.title") }}
          </button>
          <div class="detail-title">
            <div class="dh-avatar">
              <img
                v-if="selectedLead.coupleProfilePicture"
                :src="selectedLead.coupleProfilePicture"
                class="dh-avatar-img"
                alt=""
              />
              <template v-else>{{
                (selectedLead.coupleName?.[0] ?? "?").toUpperCase()
              }}</template>
            </div>
            <div>
              <h3>{{ selectedLead.coupleName }}</h3>
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
              calls-route="/vendor/calls"
              my-role="VENDOR"
            />
          </div>
        </div>

        <!-- Tab bar -->
        <div v-if="conversation || offers.length" class="detail-tabs">
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
            <span v-if="latestOffer?.status === 'REVISED'" class="tab-badge"
              >!</span
            >
          </button>
        </div>

        <!-- New lead enquiry preview (no tabs) -->
        <div v-if="selectedLead.status === 'NEW'" class="enquiry-preview">
          <p class="preview-label">{{ t("leads.enquiryMessage") }}</p>
          <p>{{ selectedLead.message || t("leads.noMessage") }}</p>
          <div class="accept-actions">
            <button class="btn-accept" @click="onAccept(selectedLead.id)">
              {{ t("leads.acceptStartChat") }}
            </button>
            <button class="btn-decline" @click="onDecline(selectedLead.id)">
              {{ t("leads.markDeclined") }}
            </button>
          </div>
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
            <button class="offer-banner-link" @click="activeTab = 'offers'">
              {{ t("leads.viewAll") }} →
            </button>
          </div>
          <div v-if="conversation" class="conv-section">
            <ConversationThread
              :messages="messages"
              my-role="VENDOR"
              :sending="sending"
              @send="sendMessage"
            />
          </div>
        </template>

        <!-- Offers tab -->
        <template v-else-if="activeTab === 'offers'">
          <div class="offers-panel">
            <div class="offers-panel-header">
              <h4>{{ t("leads.offersTab") }}</h4>
              <button
                v-if="
                  canSendNewOffer &&
                  ['IN_DISCUSSION', 'QUOTED'].includes(selectedLead.status)
                "
                class="btn-send-offer"
                @click="showOfferForm = !showOfferForm"
              >
                {{
                  showOfferForm
                    ? t("leads.cancelOffer")
                    : `+ ${t("leads.newOffer")}`
                }}
              </button>
            </div>

            <!-- Blocked reason -->
            <div v-if="sendBlockReason" class="send-blocked">
              <Lock :size="14" /> {{ sendBlockReason }}
            </div>

            <!-- Inline offer form -->
            <form
              v-if="showOfferForm && canSendNewOffer"
              class="offer-form-panel"
              @submit.prevent="submitOffer"
            >
              <h5>{{ t("leads.newOffer") }}</h5>
              <label
                >{{ t("leads.packageDetails") }}
                <textarea
                  v-model="offerFormData.packageDetails"
                  rows="3"
                  :placeholder="t('leads.packagePlaceholder')"
                />
              </label>
              <label
                >{{ t("leads.price") }}
                <input
                  v-model="offerFormData.price"
                  type="number"
                  min="1"
                  step="0.01"
                  required
                />
              </label>
              <label
                >{{ t("leads.expiresOn") }}
                <input v-model="offerFormData.expiresAt" type="date" />
              </label>
              <div class="offer-form-actions">
                <button type="submit" class="btn-submit-offer">
                  {{ t("leads.sendOffer") }}
                </button>
                <button
                  type="button"
                  class="btn-cancel-offer"
                  @click="showOfferForm = false"
                >
                  {{ t("leads.cancelOffer") }}
                </button>
              </div>
            </form>

            <div v-if="!offers.length" class="offers-empty">
              {{ t("leads.noOffers") }}
            </div>
            <div class="offers-list">
              <OfferCard
                v-for="offer in offers"
                :key="offer.id"
                :offer="offer"
                my-role="VENDOR"
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
.inbox-layout {
  display: grid;
  grid-template-columns: 340px 1fr;
  height: calc(100vh - 116px);
  overflow: hidden;
  position: relative;
}

/* Lead Panel */
.lead-panel {
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
.panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 20px 12px;
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 1;
}
.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.btn-export-csv {
  font-size: 0.78rem;
  padding: 4px 10px;
  border: 1px solid var(--color-gold);
  border-radius: 6px;
  background: transparent;
  color: var(--color-gold);
  cursor: pointer;
  font-weight: 600;
  white-space: nowrap;
}
.btn-export-csv:hover {
  background: var(--color-gold);
  color: #fff;
}
.btn-export-csv:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.panel-sub {
  margin: 2px 0 0;
  font-size: 0.82rem;
  color: var(--color-muted);
  line-height: 1.4;
}
.panel-header h2 {
  margin: 0;
  font-size: 1.1rem;
}
.filter-select {
  font-size: 0.8rem;
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-white);
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

/* Detail Panel */
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
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
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
.btn-send-offer,
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
.btn-send-offer:hover,
.btn-request-offer:hover {
  background: var(--color-gold-light, #fdf8ee);
}
.send-blocked {
  background: var(--chip-amber-bg, #fff8e1);
  border: 1px solid var(--color-gold);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 0.83rem;
  color: var(--color-gold);
  flex-shrink: 0;
}
.offer-form-panel {
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
}
.offer-form-panel h5 {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 700;
}
.offer-form-panel label {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 0.85rem;
  font-weight: 500;
}
.offer-form-panel input,
.offer-form-panel textarea {
  padding: 8px 10px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
}
.offer-form-actions {
  display: flex;
  gap: 8px;
}
.btn-submit-offer {
  background: var(--color-gold);
  color: #fff;
  border: none;
  padding: 8px 20px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.85rem;
}
.btn-cancel-offer {
  background: transparent;
  border: 1.5px solid var(--color-border);
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-muted);
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

.enquiry-preview {
  padding: 24px;
}
.preview-label {
  font-weight: 600;
  margin-bottom: 8px;
}
.accept-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}
.btn-accept {
  background: var(--color-gold);
  color: #fff;
  border: none;
  padding: 9px 20px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
.btn-decline {
  background: transparent;
  border: 1.5px solid var(--color-border);
  padding: 9px 20px;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  color: var(--color-muted);
}
.btn-decline:hover {
  border-color: var(--color-error);
  color: var(--color-error);
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
  .inbox-layout {
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
    padding: 12px 16px;
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
  .panel-header {
    flex-direction: column;
    gap: 8px;
  }
  .filter-select {
    width: 100%;
  }
}
</style>
