<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { X } from "lucide-vue-next";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import { useAuthStore } from "@/stores/auth.store";
import { videoCallsApi } from "@/api/videoCalls.api";

const emit = defineEmits<{ close: []; scheduled: [leadId: string] }>();

const videoStore = useVideoCallsStore();
const authStore = useAuthStore();
const step = ref<"pick" | "confirm">("pick");
const selectedDate = ref("");
const selectedTime = ref("");
const scheduling = ref(false);
const error = ref("");
const blockedDates = ref<Set<string>>(new Set());
const nowDate = ref(new Date());
let clockTick: ReturnType<typeof setInterval> | null = null;

const isVendor = computed(() => authStore.user?.role === "VENDOR");

onMounted(async () => {
  clockTick = setInterval(() => (nowDate.value = new Date()), 60_000);
  if (videoStore.scheduleTargetVendorId) {
    try {
      const res = await videoCallsApi.getBlockedDates(
        videoStore.scheduleTargetVendorId,
      );
      blockedDates.value = new Set(res.data.map((d) => d.date));
    } catch {}
  }
});

onUnmounted(() => {
  if (clockTick) clearInterval(clockTick);
});

const today = new Date().toISOString().slice(0, 10);

/** All 48 half-hour slots from 00:00 to 23:30. */
const allTimeSlots = computed(() => {
  const slots: string[] = [];
  for (let h = 0; h < 24; h++) {
    slots.push(`${String(h).padStart(2, "0")}:00`);
    slots.push(`${String(h).padStart(2, "0")}:30`);
  }
  return slots;
});

/** A slot is in the past when the selected date is today and the slot time has already passed. */
function isSlotPast(slot: string): boolean {
  if (!selectedDate.value || selectedDate.value !== today) return false;
  const [h, m] = slot.split(":").map(Number);
  const slotMinutes = h * 60 + m;
  const nowMinutes = nowDate.value.getHours() * 60 + nowDate.value.getMinutes();
  return slotMinutes <= nowMinutes;
}

const dateError = computed(() => {
  if (!selectedDate.value) return "";
  if (selectedDate.value < today) return "Please choose a future date.";
  if (blockedDates.value.has(selectedDate.value))
    return "The vendor is not available on this date.";
  return "";
});

// Clear time when date changes to avoid stale past-slot selection
watch(selectedDate, () => {
  if (selectedTime.value && isSlotPast(selectedTime.value))
    selectedTime.value = "";
});

const canProceed = computed(
  () =>
    !!selectedDate.value &&
    !!selectedTime.value &&
    !dateError.value &&
    !isSlotPast(selectedTime.value),
);

const formattedDateTime = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return "";
  const d = new Date(`${selectedDate.value}T${selectedTime.value}`);
  return d.toLocaleString(undefined, {
    weekday: "long",
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
});

const scheduledAtISO = computed(() => {
  if (!selectedDate.value || !selectedTime.value) return "";
  return new Date(
    `${selectedDate.value}T${selectedTime.value}:00`,
  ).toISOString();
});

const confirmNote = computed(() => {
  if (videoStore.rescheduleCallId) {
    return "A new time proposal will be sent to the other party. They can accept, propose a different time, or cancel.";
  }
  const other = isVendor.value ? "the couple" : "the vendor";
  return `Your request will be sent to ${other} for confirmation. They can accept, propose a new time, or decline.`;
});

function goConfirm() {
  if (!canProceed.value) return;
  step.value = "confirm";
}

async function confirm() {
  const leadId = videoStore.scheduleTargetLeadId;
  if (!leadId || !scheduledAtISO.value) return;
  scheduling.value = true;
  error.value = "";
  try {
    if (videoStore.rescheduleCallId) {
      await videoStore.rescheduleCall(
        videoStore.rescheduleCallId,
        scheduledAtISO.value,
        leadId,
      );
    } else {
      await videoStore.scheduleCall(leadId, scheduledAtISO.value);
    }
    emit("scheduled", leadId);
    videoStore.closeScheduleModal();
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? "Could not schedule call.";
    step.value = "pick";
  } finally {
    scheduling.value = false;
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h3>
          {{
            step === "pick"
              ? videoStore.rescheduleCallId
                ? "Propose New Time"
                : "Request a Video Call"
              : videoStore.rescheduleCallId
                ? "Confirm New Time"
                : "Confirm Request"
          }}
        </h3>
        <button class="close-btn" @click="emit('close')">
          <X :size="18" />
        </button>
      </div>

      <template v-if="step === 'pick'">
        <div class="modal-body">
          <div class="field">
            <label>Date</label>
            <input
              v-model="selectedDate"
              type="date"
              class="input"
              :min="today"
            />
            <p v-if="dateError" class="field-error">{{ dateError }}</p>
          </div>
          <div v-if="selectedDate && !dateError" class="field">
            <label>Time</label>
            <div class="time-grid">
              <button
                v-for="slot in allTimeSlots"
                :key="slot"
                class="time-slot"
                :class="{
                  active: selectedTime === slot,
                  past: isSlotPast(slot),
                }"
                :disabled="isSlotPast(slot)"
                type="button"
                @click="selectedTime = slot"
              >
                {{ slot }}
              </button>
            </div>
          </div>
          <p v-if="error" class="error-msg">{{ error }}</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="emit('close')">Cancel</button>
          <button
            class="primary-btn"
            :disabled="!canProceed"
            @click="goConfirm"
          >
            Continue
          </button>
        </div>
      </template>

      <template v-else>
        <div class="modal-body confirm-body">
          <div class="confirm-card">
            <span class="confirm-icon">&#x1F4F9;</span>
            <div class="confirm-info">
              <p class="confirm-label">Scheduled for</p>
              <p class="confirm-time">{{ formattedDateTime }}</p>
            </div>
          </div>
          <p class="confirm-note">{{ confirmNote }}</p>
          <p v-if="error" class="error-msg">{{ error }}</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="step = 'pick'">Back</button>
          <button class="primary-btn" :disabled="scheduling" @click="confirm">
            {{
              scheduling
                ? "Sending..."
                : videoStore.rescheduleCallId
                  ? "Send New Proposal"
                  : "Send Request"
            }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 400;
}
.modal {
  background: var(--color-white);
  border-radius: 14px;
  width: 440px;
  max-width: 95vw;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.18);
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}
.modal-header h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
}
.close-btn {
  background: none;
  border: none;
  font-size: 1.4rem;
  cursor: pointer;
  color: var(--color-muted);
}
.modal-body {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
label {
  font-size: 0.84rem;
  font-weight: 600;
}
.input {
  padding: 9px 13px;
  border: 1.5px solid var(--color-border);
  border-radius: 7px;
  font-size: 0.9rem;
  outline: none;
}
.input:focus {
  border-color: var(--color-gold);
}
.field-error {
  color: var(--color-error);
  font-size: 0.8rem;
  margin: 0;
}
.time-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 5px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 2px;
}
.time-slot {
  padding: 7px 4px;
  border: 1.5px solid var(--color-border);
  border-radius: 7px;
  background: var(--color-surface);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition:
    border-color 0.15s,
    background 0.15s;
}
.time-slot:hover {
  border-color: var(--color-gold);
}
.time-slot.active {
  background: var(--color-gold);
  border-color: var(--color-gold);
  color: #fff;
}
.time-slot.past {
  opacity: 0.35;
  cursor: not-allowed;
  background: var(--color-surface);
  border-color: var(--color-border);
  color: var(--color-muted);
}
.time-slot:disabled {
  pointer-events: none;
}
.confirm-body {
  gap: 14px;
}
.confirm-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 16px;
}
.confirm-icon {
  font-size: 1.8rem;
}
.confirm-label {
  margin: 0 0 2px;
  font-size: 0.75rem;
  color: var(--color-muted);
}
.confirm-time {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
}
.confirm-note {
  margin: 0;
  font-size: 0.82rem;
  color: var(--color-muted);
}
.error-msg {
  color: var(--color-error);
  font-size: 0.85rem;
  margin: 0;
}
.modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding: 16px 20px;
}
.cancel-btn {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 18px;
  cursor: pointer;
  font-size: 0.88rem;
}
.primary-btn {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 700;
  cursor: pointer;
  font-size: 0.88rem;
}
.primary-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 767px) {
  .modal {
    border-radius: 14px !important;
  }
  .modal::before {
    display: none !important;
  }
}
</style>
