<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from "vue";
import { useMessagesStore } from "@/stores/messages.store";
import { useAuthStore } from "@/stores/auth.store";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { useWebSocket } from "@/composables/useWebSocket";
import MessageBubble from "./MessageBubble.vue";
import SendOfferModal from "@/components/vendor/SendOfferModal.vue";
import CallBanner from "@/components/video/CallBanner.vue";
import ScheduleCallModal from "@/components/video/ScheduleCallModal.vue";
import { Gem, MapPin, ClipboardList, Calendar } from "lucide-vue-next";

const props = defineProps<{
  threadId: string;
  dealId?: string;
  dealStatus?: string;
  vendorId?: string;
  coupleWeddingDate?: string;
  coupleLocation?: string;
}>();

const messagesStore = useMessagesStore();
const authStore = useAuthStore();
const videoStore = useVideoCallsStore();
const ws = useWebSocket();
const newMessage = ref("");
const sending = ref(false);
const showOfferModal = ref(false);
const scrollRef = ref<HTMLDivElement | null>(null);
let offNewMessage: (() => void) | null = null;

const messages = computed(() => messagesStore.messages[props.threadId] ?? []);
const isVendor = computed(() => authStore.user?.role === "VENDOR");
const callsRoute = computed(() =>
  isVendor.value ? "/vendor/calls" : "/couple/calls",
);
const canSendOffer = computed(
  () =>
    isVendor.value &&
    !!props.dealId &&
    (props.dealStatus === "IN_DISCUSSION" || props.dealStatus === "NEW"),
);
const activeCall = computed(() =>
  props.dealId ? (videoStore.liveCallByLead[props.dealId] ?? null) : null,
);

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    if (scrollRef.value) {
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
    }
  },
);

onMounted(async () => {
  await messagesStore.openThread(props.threadId);
  if (props.dealId) {
    // Connect to the deal's WebSocket channel for live message delivery
    ws.connect(props.dealId);
    offNewMessage = ws.on("NEW_MESSAGE", async () => {
      await messagesStore.fetchMessages(props.threadId);
      await messagesStore.markRead(props.threadId);
    });
    await videoStore.fetchActiveForLead(props.dealId);
  }
  await nextTick();
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
});

onUnmounted(() => {
  // Deregister this component's handler only — do NOT disconnect the shared singleton
  offNewMessage?.();
});

async function send() {
  const content = newMessage.value.trim();
  if (!content) return;
  sending.value = true;
  newMessage.value = "";
  await messagesStore.sendMessage(props.threadId, content);
  sending.value = false;
}

function onOfferSent() {
  showOfferModal.value = false;
  messagesStore.openThread(props.threadId);
}

async function onQuoteActioned() {
  await messagesStore.fetchThreads();
  await messagesStore.openThread(props.threadId);
}

function statusLabel(s?: string) {
  if (!s) return "";
  return s.replace(/_/g, " ");
}

function openScheduleModal() {
  if (!props.dealId) return;
  videoStore.openScheduleModal(props.dealId, props.vendorId);
}

async function onCallScheduled(leadId: string) {
  await videoStore.fetchActiveForLead(leadId);
}
</script>

<template>
  <div class="message-window">
    <!-- Deal context header -->
    <div v-if="dealId" class="deal-header">
      <div class="deal-header-info">
        <span class="deal-status-badge" :class="dealStatus?.toLowerCase()">
          {{ statusLabel(dealStatus) }}
        </span>
        <span v-if="coupleWeddingDate" class="deal-meta">
          <Gem :size="14" /> {{ coupleWeddingDate }}
        </span>
        <span v-if="coupleLocation" class="deal-meta">
          <MapPin :size="14" /> {{ coupleLocation }}
        </span>
      </div>
      <div class="deal-header-actions">
        <button
          v-if="canSendOffer"
          class="offer-btn"
          @click="showOfferModal = true"
        >
          <ClipboardList :size="14" /> Send Offer
        </button>
        <button
          v-if="
            dealId &&
            (!activeCall ||
              activeCall.status === 'CANCELLED' ||
              activeCall.status === 'COMPLETED')
          "
          class="video-call-btn"
          @click="openScheduleModal"
        >
          <Calendar :size="14" /> Schedule Call
        </button>
        <CallBanner
          v-else-if="activeCall && dealId"
          :call="activeCall"
          :calls-route="callsRoute"
          :my-role="isVendor ? 'VENDOR' : 'COUPLE'"
        />
      </div>
    </div>

    <!-- Messages -->
    <div ref="scrollRef" class="messages-scroll">
      <div v-if="messages.length === 0" class="empty">
        No messages yet. Say hi!
      </div>
      <MessageBubble
        v-for="msg in messages"
        :key="msg.id"
        :message="msg"
        :deal-status="dealStatus"
        @refresh="onQuoteActioned"
      />
    </div>

    <!-- Compose bar -->
    <div class="compose-bar">
      <input
        v-model="newMessage"
        class="compose-input"
        placeholder="Type a message…"
        :disabled="sending"
        @keyup.enter="send"
      />
      <button
        class="send-btn"
        :disabled="sending || !newMessage.trim()"
        @click="send"
      >
        Send
      </button>
    </div>

    <!-- Send Offer modal -->
    <SendOfferModal
      v-if="showOfferModal && dealId"
      :lead-id="dealId"
      @close="showOfferModal = false"
      @sent="onOfferSent"
    />

    <!-- Video call modals -->
    <ScheduleCallModal
      v-if="videoStore.showScheduleModal"
      @close="videoStore.closeScheduleModal()"
      @scheduled="onCallScheduled"
    />
  </div>
</template>

<style scoped>
.message-window {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

/* ─── Deal header ─── */
.deal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  gap: 12px;
  flex-wrap: wrap;
}
.deal-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.deal-status-badge {
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 20px;
  padding: 3px 10px;
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.deal-status-badge.confirmed {
  background: var(--chip-green-bg);
  color: var(--color-green);
}
.deal-status-badge.quote_accepted {
  background: var(--chip-green-bg);
  color: var(--color-green);
}
.deal-meta {
  font-size: 0.78rem;
  color: var(--color-muted);
}
.offer-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 7px 16px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.offer-btn:hover {
  opacity: 0.9;
}
.deal-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.video-call-btn {
  background: #2d6cdf;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 7px 16px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.video-call-btn:hover:not(:disabled) {
  background: #2459c4;
}
.video-call-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ─── Messages scroll ─── */
.messages-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
}
.empty {
  text-align: center;
  color: var(--color-muted);
  font-size: 0.85rem;
  padding: 32px;
}

/* ─── Compose bar ─── */
.compose-bar {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  background: var(--color-white);
}
.compose-input {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 20px;
  font-size: 0.9rem;
  outline: none;
  color: var(--color-text);
  background: var(--color-white);
}
.compose-input:focus {
  border-color: var(--color-gold);
}
.send-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 20px;
  padding: 10px 20px;
  font-weight: 700;
  cursor: pointer;
}
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
