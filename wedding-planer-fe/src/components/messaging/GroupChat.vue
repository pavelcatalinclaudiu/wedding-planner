<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from "vue";
import { useMessagesStore } from "@/stores/messages.store";
import { useConfirm } from "@/composables/useConfirm";
import { useAuthStore } from "@/stores/auth.store";
import { useLeadsStore } from "@/stores/leads.store";
import { messagesApi } from "@/api/messages.api";
import type { ThreadParticipant } from "@/types/message.types";
import type { Lead } from "@/types/lead.types";
import MessageBubble from "./MessageBubble.vue";
import { UserPlus, X, Plus } from "lucide-vue-next";

const props = defineProps<{ threadId: string }>();
const emit = defineEmits<{ (e: "left"): void }>();

const messagesStore = useMessagesStore();
const authStore = useAuthStore();
const leadsStore = useLeadsStore();

const newMessage = ref("");
const sending = ref(false);
const scrollRef = ref<HTMLDivElement | null>(null);
const showPanel = ref(false);
const participants = ref<ThreadParticipant[]>([]);
const loadingParticipants = ref(false);
const leavingOrRemoving = ref(false);
const addingVendorId = ref<string | null>(null);

const isCouple = computed(() => authStore.user?.role === "COUPLE");
const isVendor = computed(() => authStore.user?.role === "VENDOR");

const messages = computed(() => messagesStore.messages[props.threadId] ?? []);
const thread = computed(() =>
  messagesStore.threads.find((t) => t.id === props.threadId),
);

// Display name for this thread
const threadName = computed(() => thread.value?.name ?? "Group Chat");
const memberCount = computed(
  () => participants.value.length || thread.value?.participantCount || 0,
);

// Booked leads whose vendor is NOT yet a participant in the chat
const addableVendors = computed<Lead[]>(() => {
  if (!isCouple.value) return [];
  const presentNames = new Set(
    participants.value.filter((p) => p.role === "VENDOR").map((p) => p.name),
  );
  return leadsStore.leads.filter(
    (l) => l.status === "BOOKED" && !presentNames.has(l.vendorName),
  );
});

watch(
  () => messages.value.length,
  async () => {
    await nextTick();
    if (scrollRef.value)
      scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
  },
);

onMounted(async () => {
  await messagesStore.openThread(props.threadId);
  await nextTick();
  if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight;
});

async function send() {
  const content = newMessage.value.trim();
  if (!content) return;
  sending.value = true;
  newMessage.value = "";
  await messagesStore.sendMessage(props.threadId, content);
  sending.value = false;
}

async function openPanel() {
  showPanel.value = true;
  loadingParticipants.value = true;
  try {
    const res = await messagesApi.getParticipants(props.threadId);
    participants.value = res.data;
    // Ensure booked leads are loaded so the couple can add vendors manually
    if (isCouple.value && leadsStore.leads.length === 0) {
      await leadsStore.fetchCoupleLeads();
    }
  } finally {
    loadingParticipants.value = false;
  }
}

async function removeParticipant(p: ThreadParticipant) {
  const ok = await useConfirm().ask(`Remove ${p.name} from the chat?`, {
    title: "Remove Participant",
    confirmText: "Remove",
  });
  if (!ok) return;
  leavingOrRemoving.value = true;
  try {
    await messagesApi.removeParticipant(props.threadId, p.id);
    participants.value = participants.value.filter((x) => x.id !== p.id);
  } finally {
    leavingOrRemoving.value = false;
  }
}

async function addVendor(lead: Lead) {
  addingVendorId.value = lead.vendorId;
  try {
    await messagesApi.addParticipant(props.threadId, lead.vendorId);
    // Refresh participants list
    const res = await messagesApi.getParticipants(props.threadId);
    participants.value = res.data;
  } finally {
    addingVendorId.value = null;
  }
}

async function leaveChat() {
  const ok = await useConfirm().ask(
    "Leave this group chat? You will no longer receive messages.",
    { title: "Leave Chat", confirmText: "Leave", danger: true },
  );
  if (!ok) return;
  leavingOrRemoving.value = true;
  try {
    await messagesApi.leaveThread(props.threadId);
    emit("left");
  } finally {
    leavingOrRemoving.value = false;
  }
}

function avatarInitial(name: string) {
  return name?.[0]?.toUpperCase() ?? "?";
}
</script>

<template>
  <div class="gc-root">
    <!-- ── Main chat column ─────────────────────────────────────── -->
    <div class="gc-main">
      <!-- Header -->
      <div class="gc-header">
        <div class="gc-header-left">
          <h3>{{ threadName }}</h3>
          <span class="member-count">{{ memberCount }} members</span>
        </div>
        <div class="gc-header-actions">
          <button class="action-btn" title="Manage members" @click="openPanel">
            <UserPlus :size="18" />
          </button>
          <button
            v-if="isVendor"
            class="leave-btn"
            :disabled="leavingOrRemoving"
            @click="leaveChat"
          >
            Leave Chat
          </button>
        </div>
      </div>

      <!-- Messages -->
      <div ref="scrollRef" class="gc-messages">
        <div v-if="messages.length === 0" class="empty">
          Start the conversation!
        </div>
        <MessageBubble v-for="msg in messages" :key="msg.id" :message="msg" />
      </div>

      <!-- Compose -->
      <div class="gc-compose">
        <input
          v-model="newMessage"
          class="compose-input"
          placeholder="Message the group…"
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
    </div>

    <!-- ── Participants side-panel ──────────────────────────────── -->
    <transition name="slide">
      <div v-if="showPanel" class="gc-panel">
        <div class="panel-header">
          <span>Members</span>
          <button class="close-btn" @click="showPanel = false">
            <X :size="16" />
          </button>
        </div>

        <div v-if="loadingParticipants" class="panel-loading">Loading…</div>

        <template v-else>
          <ul class="participant-list">
            <li v-for="p in participants" :key="p.id" class="participant-item">
              <div class="p-avatar">
                <img v-if="p.avatarUrl" :src="p.avatarUrl" alt="" />
                <span v-else>{{ avatarInitial(p.name) }}</span>
              </div>
              <div class="p-info">
                <span class="p-name">{{ p.name }}</span>
                <span class="p-role">{{ p.role }}</span>
              </div>
              <!-- Couple can remove vendors (not themselves) -->
              <button
                v-if="isCouple && p.role === 'VENDOR'"
                class="remove-btn"
                :disabled="leavingOrRemoving"
                title="Remove from chat"
                @click="removeParticipant(p)"
              >
                <X :size="14" />
              </button>
            </li>
          </ul>

          <!-- Add vendors section (couple only) -->
          <template v-if="isCouple && addableVendors.length > 0">
            <div class="add-section-header">Add to chat</div>
            <ul class="participant-list addable-list">
              <li
                v-for="lead in addableVendors"
                :key="lead.vendorId"
                class="participant-item"
              >
                <div class="p-avatar">
                  <img
                    v-if="lead.vendorProfilePicture"
                    :src="lead.vendorProfilePicture"
                    alt=""
                  />
                  <span v-else>{{ avatarInitial(lead.vendorName) }}</span>
                </div>
                <div class="p-info">
                  <span class="p-name">{{ lead.vendorName }}</span>
                  <span class="p-role">{{ lead.vendorCategory }}</span>
                </div>
                <button
                  class="add-btn"
                  :disabled="addingVendorId === lead.vendorId"
                  title="Add to chat"
                  @click="addVendor(lead)"
                >
                  <Plus :size="14" />
                </button>
              </li>
            </ul>
          </template>
        </template>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.gc-root {
  display: flex;
  flex: 1;
  overflow: hidden;
  height: 100%;
  position: relative;
}

/* ── Main chat ─────────────────────────────────────────────────── */
.gc-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}
.gc-header {
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.gc-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.gc-header-left h3 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
}
.member-count {
  font-size: 0.78rem;
  color: var(--color-muted);
}
.gc-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-muted);
  padding: 4px;
  border-radius: 6px;
  display: flex;
  align-items: center;
}
.action-btn:hover {
  color: var(--color-text);
  background: var(--color-surface);
}
.leave-btn {
  font-size: 0.8rem;
  background: none;
  border: 1.5px solid var(--color-error);
  color: var(--color-error);
  border-radius: 8px;
  padding: 4px 12px;
  cursor: pointer;
  font-weight: 600;
}
.leave-btn:hover {
  background: var(--color-error);
  color: var(--color-white);
}
.leave-btn:disabled {
  opacity: 0.5;
}
.gc-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
}
.empty {
  text-align: center;
  color: var(--color-muted);
  font-size: 0.85rem;
  padding: 32px;
}
.gc-compose {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
}
.compose-input {
  flex: 1;
  padding: 10px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 20px;
  font-size: 0.9rem;
  outline: none;
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
}

/* ── Participants panel ─────────────────────────────────────────── */
.gc-panel {
  width: 240px;
  border-left: 1px solid var(--color-border);
  background: var(--color-white);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 700;
  font-size: 0.88rem;
}
.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  color: var(--color-muted);
}
.panel-loading {
  padding: 20px;
  text-align: center;
  color: var(--color-muted);
  font-size: 0.85rem;
}
.participant-list {
  list-style: none;
  margin: 0;
  padding: 8px 0;
  overflow-y: auto;
  flex: 1;
}
.participant-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
}
.participant-item:hover {
  background: var(--color-surface);
}
.p-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-gold-light, #f5f0e8);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.85rem;
  overflow: hidden;
  flex-shrink: 0;
}
.p-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.p-info {
  flex: 1;
  min-width: 0;
}
.p-name {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-role {
  display: block;
  font-size: 0.72rem;
  color: var(--color-muted);
  text-transform: capitalize;
}
.remove-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-muted);
  font-size: 0.8rem;
  flex-shrink: 0;
  padding: 4px;
  border-radius: 4px;
}
.remove-btn:hover {
  color: var(--color-error);
  background: var(--color-error-light);
}
.remove-btn:disabled {
  opacity: 0.4;
}

/* Add-vendor section */
.add-section-header {
  padding: 10px 16px 4px;
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: var(--color-muted);
  border-top: 1px solid var(--color-border);
  margin-top: 4px;
}
.addable-list {
  border-top: none;
}
.add-btn {
  background: none;
  border: 1.5px solid var(--color-gold);
  cursor: pointer;
  color: var(--color-gold);
  flex-shrink: 0;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
}
.add-btn:hover {
  background: var(--color-gold);
  color: #fff;
}
.add-btn:disabled {
  opacity: 0.4;
}

/* Slide-in animation for the side panel */
.slide-enter-active,
.slide-leave-active {
  transition: width 0.2s ease;
}
.slide-enter-from,
.slide-leave-to {
  width: 0;
  overflow: hidden;
}
</style>
