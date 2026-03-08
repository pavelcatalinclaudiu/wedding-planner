<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import type { VideoCall } from "@/types/lead.types";
import { Inbox, Clock, Video } from "lucide-vue-next";

const props = defineProps<{
  call: VideoCall;
  /** The calls management route for this role, e.g. "/couple/calls" */
  callsRoute: string;
  myRole?: "COUPLE" | "VENDOR";
}>();

const videoStore = useVideoCallsStore();
const now = ref(Date.now());
let ticker: ReturnType<typeof setInterval> | null = null;

onMounted(() => {
  ticker = setInterval(() => (now.value = Date.now()), 1000);
});
onUnmounted(() => {
  if (ticker) clearInterval(ticker);
});

const scheduledMs = computed(() => new Date(props.call.scheduledAt).getTime());
const msUntil = computed(() => scheduledMs.value - now.value);

/** Join is enabled 30 min before the scheduled time, or any time the call is IN_PROGRESS (rejoin). */
const canJoin = computed(
  () =>
    props.call.status === "IN_PROGRESS" ||
    (props.call.status === "SCHEDULED" && msUntil.value <= 30 * 60 * 1000),
);

const isImminent = computed(
  () => msUntil.value <= 30 * 60 * 1000 && msUntil.value > 0,
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

/** True when the OTHER party proposed and I need to respond. */
const needsMyResponse = computed(
  () =>
    props.call.status === "PENDING" &&
    props.myRole !== undefined &&
    props.call.proposedBy !== props.myRole,
);

async function joinCall() {
  await videoStore.joinCall(props.call.id);
}
</script>

<template>
  <!-- PENDING state -->
  <div
    v-if="call.status === 'PENDING'"
    class="call-banner pending"
    :class="{ 'action-required': needsMyResponse }"
  >
    <span class="banner-icon"
      ><Inbox v-if="needsMyResponse" :size="20" /><Clock v-else :size="20"
    /></span>
    <div class="banner-body">
      <span class="banner-date">{{ formattedDate }}</span>
      <span class="banner-pending-label">{{
        needsMyResponse ? "Response needed" : "Awaiting confirmation"
      }}</span>
    </div>
    <RouterLink :to="callsRoute" class="btn-respond">{{
      needsMyResponse ? "Respond →" : "View →"
    }}</RouterLink>
  </div>

  <!-- SCHEDULED / IN_PROGRESS state -->
  <div
    v-else
    class="call-banner"
    :class="[call.status.toLowerCase(), { imminent: isImminent }]"
  >
    <span class="banner-icon"><Video :size="20" /></span>
    <div class="banner-body">
      <span class="banner-date">{{ formattedDate }}</span>
      <span
        v-if="call.status === 'SCHEDULED' && msUntil > 0"
        class="banner-countdown"
      >
        in {{ countdown }}
      </span>
      <span v-else-if="call.status === 'IN_PROGRESS'" class="banner-live"
        >● Live</span
      >
    </div>
    <button
      class="btn-join"
      :disabled="!canJoin"
      :title="canJoin ? 'Join call' : 'Available 30 min before the call'"
      @click="joinCall"
    >
      {{ call.status === "IN_PROGRESS" ? "Rejoin" : "Join" }}
    </button>
    <RouterLink :to="callsRoute" class="btn-manage">All calls →</RouterLink>
  </div>
</template>

<style scoped>
.call-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--chip-blue-bg, #eef1ff);
  border: 1.5px solid rgba(74, 108, 247, 0.35);
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 0.82rem;
  transition: border-color 0.2s;
}
.call-banner.imminent,
.call-banner.in_progress {
  background: var(--color-gold-light, #fdf8ee);
  border-color: var(--color-gold);
}

.banner-icon {
  font-size: 1rem;
  flex-shrink: 0;
}
.banner-body {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.banner-date {
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
}
.banner-countdown {
  color: var(--color-muted);
  font-size: 0.77rem;
  white-space: nowrap;
}
.banner-live {
  background: var(--color-gold);
  color: #fff;
  border-radius: 20px;
  padding: 1px 7px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}

.btn-join {
  background: var(--chip-blue-text, #4a6cf7);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 5px 14px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
}
.call-banner.imminent .btn-join,
.call-banner.in_progress .btn-join {
  background: var(--color-gold);
}
.btn-join:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.call-banner.pending {
  background: var(--color-gold-light, #fdf8ee);
  border-color: rgba(255, 193, 7, 0.5);
}
.call-banner.pending.action-required {
  background: var(--color-amber-light);
  border-color: var(--color-gold);
}
.banner-pending-label {
  font-size: 0.75rem;
  color: var(--color-gold);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  white-space: nowrap;
}
.btn-respond {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 5px 14px;
  font-size: 0.8rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  text-decoration: none;
}

.btn-manage {
  font-size: 0.78rem;
  color: var(--chip-blue-text, #4a6cf7);
  text-decoration: none;
  white-space: nowrap;
  font-weight: 600;
  flex-shrink: 0;
}
.call-banner.imminent .btn-manage,
.call-banner.in_progress .btn-manage {
  color: var(--color-gold);
}
.btn-manage:hover {
  text-decoration: underline;
}
</style>
