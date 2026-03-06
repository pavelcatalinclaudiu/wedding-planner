<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import type { VideoCall } from "@/types/lead.types";

const props = defineProps<{
  call: VideoCall;
  leadId: string;
  vendorId?: string;
  /** Passed so the card knows which party is viewing */
  myRole: "COUPLE" | "VENDOR";
}>();

const videoStore = useVideoCallsStore();
const now = ref(Date.now());
const cancelling = ref(false);
const accepting = ref(false);
let ticker: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  ticker = setInterval(() => (now.value = Date.now()), 1000);
});
onUnmounted(() => {
  if (ticker) clearInterval(ticker);
});

// ── Derived ───────────────────────────────────────────────────────────
const scheduledMs = computed(() => new Date(props.call.scheduledAt).getTime());
const msUntil = computed(() => scheduledMs.value - now.value);

/**
 * Join is enabled:
 *  - When IN_PROGRESS (always — allows rejoin after technical issues)
 *  - When SCHEDULED and within 30 min of the scheduled time
 */
const canJoin = computed(
  () =>
    props.call.status === "IN_PROGRESS" ||
    (props.call.status === "SCHEDULED" && msUntil.value <= 30 * 60 * 1000),
);

/** True when the OTHER party proposed and I need to respond. */
const needsMyResponse = computed(
  () =>
    props.call.status === "PENDING" && props.call.proposedBy !== props.myRole,
);

/** True when I proposed and am waiting for the other party. */
const waitingForOther = computed(
  () =>
    props.call.status === "PENDING" && props.call.proposedBy === props.myRole,
);

const otherParty = computed(() =>
  props.myRole === "VENDOR" ? "couple" : "vendor",
);

const countdown = computed(() => {
  if (msUntil.value <= 0) return "Now";
  const totalSec = Math.floor(msUntil.value / 1000);
  const days = Math.floor(totalSec / 86400);
  const hrs = Math.floor((totalSec % 86400) / 3600);
  const mins = Math.floor((totalSec % 3600) / 60);
  const secs = totalSec % 60;

  if (days > 0) return `${days}d ${hrs}h`;
  if (hrs > 0) return `${hrs}h ${mins}m`;
  return `${mins}m ${secs < 10 ? "0" : ""}${secs}s`;
});

const formattedDate = computed(() =>
  new Date(props.call.scheduledAt).toLocaleString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }),
);

// ── Actions ───────────────────────────────────────────────────────────
async function joinCall() {
  await videoStore.joinCall(props.call.id);
}

async function accept() {
  if (accepting.value) return;
  accepting.value = true;
  try {
    await videoStore.acceptCall(props.call.id, props.leadId);
  } finally {
    accepting.value = false;
  }
}

async function cancel() {
  if (cancelling.value) return;
  cancelling.value = true;
  try {
    await videoStore.cancelCall(props.call.id, props.leadId);
  } finally {
    cancelling.value = false;
  }
}

function reschedule() {
  videoStore.openScheduleModal(props.leadId, props.vendorId, props.call.id);
}
</script>

<template>
  <div class="vc-card" :class="call.status.toLowerCase()">
    <div class="vc-header">
      <span class="vc-icon">📹</span>
      <div class="vc-title-block">
        <span class="vc-label">Video Call</span>
        <span class="vc-status" :class="call.status.toLowerCase()">{{
          call.status === "PENDING"
            ? "Awaiting Confirmation"
            : call.status.replace("_", " ")
        }}</span>
      </div>
    </div>

    <div class="vc-datetime">{{ formattedDate }}</div>

    <div class="vc-participants">
      <span v-if="call.coupleName" class="vc-participant couple">
        👰 {{ call.coupleName }}
      </span>
      <span v-if="call.coupleName && call.vendorName" class="vc-participant-sep"
        >·</span
      >
      <span v-if="call.vendorName" class="vc-participant vendor">
        🤝 {{ call.vendorName }}
      </span>
    </div>

    <!-- Countdown — only for confirmed scheduled calls -->
    <div v-if="call.status === 'SCHEDULED' && msUntil > 0" class="vc-countdown">
      <span class="countdown-label">Starts in</span>
      <span
        class="countdown-value"
        :class="{ imminent: msUntil <= 30 * 60 * 1000 }"
      >
        {{ countdown }}
      </span>
    </div>

    <!-- PENDING: Other party proposed — I need to respond -->
    <div v-if="needsMyResponse" class="vc-pending-respond">
      <p class="pending-msg">
        📬 The {{ otherParty }} proposed this time. Please respond:
      </p>
      <div class="vc-actions">
        <button class="btn-accept" :disabled="accepting" @click="accept">
          {{ accepting ? "…" : "✓ Accept" }}
        </button>
        <button class="btn-reschedule" @click="reschedule">
          Propose New Time
        </button>
        <button class="btn-cancel" :disabled="cancelling" @click="cancel">
          {{ cancelling ? "…" : "✗ Decline" }}
        </button>
      </div>
    </div>

    <!-- PENDING: I proposed — waiting for the other party -->
    <div v-else-if="waitingForOther" class="vc-pending-waiting">
      <p class="pending-msg">
        ⏳ Waiting for the {{ otherParty }} to confirm this time.
      </p>
      <div class="vc-actions">
        <button class="btn-reschedule" @click="reschedule">
          Change Proposal
        </button>
        <button class="btn-cancel" :disabled="cancelling" @click="cancel">
          {{ cancelling ? "…" : "Cancel" }}
        </button>
      </div>
    </div>

    <!-- SCHEDULED / IN_PROGRESS: confirmed call actions -->
    <div
      v-else-if="call.status === 'SCHEDULED' || call.status === 'IN_PROGRESS'"
      class="vc-actions"
    >
      <button
        class="btn-join"
        :disabled="!canJoin"
        :title="canJoin ? 'Join call' : 'Available 30 min before the call'"
        @click="joinCall"
      >
        {{ call.status === "IN_PROGRESS" ? "Rejoin Call" : "Join Call" }}
      </button>
      <button class="btn-reschedule" @click="reschedule">Reschedule</button>
      <button class="btn-cancel" :disabled="cancelling" @click="cancel">
        {{ cancelling ? "…" : "Cancel" }}
      </button>
    </div>

    <div v-else-if="call.status === 'CANCELLED'" class="vc-done-msg">
      This call was cancelled.
    </div>
    <div v-else-if="call.status === 'COMPLETED'" class="vc-done-msg">
      Call completed.
    </div>
  </div>
</template>

<style scoped>
.vc-card {
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 12px;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.vc-card.pending {
  border-color: var(--color-gold);
  background: var(--color-gold-light, #fdf8ee);
}
.vc-card.scheduled {
  border-color: var(--chip-blue-text, #4a6cf7);
}
.vc-card.in_progress {
  border-color: var(--color-gold);
}
.vc-card.cancelled {
  opacity: 0.6;
}

.vc-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.vc-icon {
  font-size: 1.3rem;
}
.vc-title-block {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.vc-label {
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.vc-status {
  font-size: 0.72rem;
  font-weight: 700;
  border-radius: 20px;
  padding: 2px 8px;
  background: var(--color-surface);
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  align-self: flex-start;
}
.vc-status.pending {
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold);
}
.vc-status.scheduled {
  background: var(--chip-blue-bg, #eef1ff);
  color: var(--chip-blue-text, #4a6cf7);
}
.vc-status.in_progress {
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold);
}
.vc-status.completed {
  background: var(--chip-green-bg);
  color: var(--color-green);
}
.vc-status.cancelled {
  background: var(--chip-red-bg);
  color: var(--color-error);
}

.vc-datetime {
  font-size: 0.92rem;
  font-weight: 600;
  color: var(--color-text);
}

.vc-participants {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.vc-participant {
  font-size: 0.82rem;
  color: var(--color-text-secondary, #666);
}
.vc-participant-sep {
  font-size: 0.82rem;
  color: var(--color-text-secondary, #888);
}

.vc-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
}
.countdown-label {
  font-size: 0.78rem;
  color: var(--color-muted);
}
.countdown-value {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
  font-variant-numeric: tabular-nums;
}
.countdown-value.imminent {
  color: var(--color-gold);
}

.vc-actions {
  display: flex;
  gap: 7px;
  flex-wrap: wrap;
}
.btn-join {
  flex: 1;
  padding: 8px 12px;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-join:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.btn-reschedule,
.btn-cancel {
  padding: 7px 11px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  background: none;
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
}
.btn-cancel {
  border-color: var(--color-error, #e74c3c);
  color: var(--color-error, #e74c3c);
}
.btn-cancel:hover {
  background: var(--chip-red-bg);
}
.btn-cancel:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-reschedule:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}

.vc-done-msg {
  font-size: 0.82rem;
  color: var(--color-muted);
  font-style: italic;
}

.vc-pending-respond,
.vc-pending-waiting {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pending-msg {
  margin: 0;
  font-size: 0.83rem;
  color: var(--color-text);
}
.btn-accept {
  flex: 1;
  padding: 8px 12px;
  background: var(--color-green, #27ae60);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 700;
  font-size: 0.85rem;
  cursor: pointer;
  transition: opacity 0.15s;
}
.btn-accept:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
