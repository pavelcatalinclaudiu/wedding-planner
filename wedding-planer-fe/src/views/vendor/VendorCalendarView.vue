<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { vendorApi } from "@/api/vendor.api";
import { videoCallsApi } from "@/api/videoCalls.api";
import { useLeadsStore } from "@/stores/leads.store";
import CalendarGrid from "@/components/ui/CalendarGrid.vue";
import { format, parseISO, isAfter, startOfToday, compareAsc } from "date-fns";
import type { AvailabilityBlock } from "@/types/vendor.types";
import type { Lead, VideoCall } from "@/types/lead.types";

const availability = ref<AvailabilityBlock[]>([]);
const calls = ref<VideoCall[]>([]);
const loading = ref(false);
const leadsStore = useLeadsStore();
const activeTab = ref<"calls" | "bookings">("calls");

async function fetchAvailability() {
  loading.value = true;
  try {
    availability.value = (await vendorApi.getAvailability()).data;
  } finally {
    loading.value = false;
  }
}

async function fetchCalls() {
  try {
    calls.value = (await videoCallsApi.list()).data;
  } catch {
    calls.value = [];
  }
}

async function block(date: string) {
  await vendorApi.blockDate(date);
  await fetchAvailability();
}

async function unblock(date: string) {
  await vendorApi.unblockDate(date);
  await fetchAvailability();
}

/* ── Upcoming bookings ──────────────────────────────────────────────────── */
const today = startOfToday();

const upcomingBookings = computed<Lead[]>(() => {
  return leadsStore.leads
    .filter(
      (l) =>
        l.status === "BOOKED" &&
        l.eventDate &&
        isAfter(parseISO(l.eventDate), today),
    )
    .sort((a, b) => compareAsc(parseISO(a.eventDate!), parseISO(b.eventDate!)));
});

const STATUS_LABELS: Record<string, string> = {
  NEW: "New Enquiry",
  VIEWED: "Viewed",
  IN_DISCUSSION: "Chatting",
  QUOTED: "Offer Sent",
  BOOKED: "Booked",
  DECLINED: "Declined",
};

const STATUS_CLASS: Record<string, string> = {
  NEW: "status-enquiry",
  VIEWED: "status-enquiry",
  IN_DISCUSSION: "status-chat",
  QUOTED: "status-quote",
  BOOKED: "status-confirmed",
  DECLINED: "status-declined",
};

function dayNum(iso: string) {
  return format(parseISO(iso), "d");
}
function monthAbbr(iso: string) {
  return format(parseISO(iso), "MMM").toUpperCase();
}
function weekday(iso: string) {
  return format(parseISO(iso), "EEEE");
}

/* ── Video calls ────────────────────────────────────────────────── */
const upcomingCalls = computed<VideoCall[]>(() =>
  calls.value
    .filter(
      (c) =>
        (c.status === "SCHEDULED" ||
          c.status === "PENDING" ||
          c.status === "IN_PROGRESS") &&
        isAfter(parseISO(c.scheduledAt), today),
    )
    .sort((a, b) =>
      compareAsc(parseISO(a.scheduledAt), parseISO(b.scheduledAt)),
    ),
);

const videoCallDates = computed<string[]>(() =>
  upcomingCalls.value.map((c) => format(parseISO(c.scheduledAt), "yyyy-MM-dd")),
);

function callTime(iso: string) {
  return format(parseISO(iso), "HH:mm");
}
function callDayNum(iso: string) {
  return format(parseISO(iso), "d");
}
function callMonthAbbr(iso: string) {
  return format(parseISO(iso), "MMM").toUpperCase();
}

onMounted(async () => {
  await Promise.all([
    fetchAvailability(),
    fetchCalls(),
    leadsStore.fetchVendorLeads(),
  ]);
});
</script>

<template>
  <div class="calendar-view">
    <!-- Header -->
    <div class="cv-header">
      <div>
        <h2 class="cv-title">Availability Calendar</h2>
        <p class="cv-sub">Click a date to block or unblock it for bookings.</p>
      </div>
    </div>

    <!-- Stat chips -->
    <div class="cv-stats">
      <div class="cv-stat cv-stat--blocked">
        <span class="cv-stat-icon">🚫</span>
        <div>
          <p class="cv-stat-val">{{ availability.length }}</p>
          <p class="cv-stat-label">Blocked Dates</p>
        </div>
      </div>
      <div class="cv-stat cv-stat--call">
        <span class="cv-stat-icon">📹</span>
        <div>
          <p class="cv-stat-val">{{ upcomingCalls.length }}</p>
          <p class="cv-stat-label">Upcoming Calls</p>
        </div>
      </div>
      <div class="cv-stat cv-stat--booking">
        <span class="cv-stat-icon">📅</span>
        <div>
          <p class="cv-stat-val">{{ upcomingBookings.length }}</p>
          <p class="cv-stat-label">Upcoming Bookings</p>
        </div>
      </div>
    </div>

    <div class="cv-body">
      <!-- Calendar column -->
      <div class="cv-calendar-col">
        <div v-if="loading" class="cv-loading">Loading…</div>
        <CalendarGrid
          v-else
          :blocked-dates="availability.map((a) => a.date)"
          :video-call-dates="videoCallDates"
          @block="block"
          @unblock="unblock"
        />
        <div class="cv-legend">
          <span class="legend-pip legend-pip--booking"></span
          ><span class="legend-lbl">Booked</span>
          <span class="legend-pip legend-pip--call"></span
          ><span class="legend-lbl">Video Call</span>
          <span class="legend-pip legend-pip--blocked"></span
          ><span class="legend-lbl">Blocked</span>
          <span class="cv-hint">👆 Click any date to toggle availability</span>
        </div>
      </div>

      <!-- Right: Tabbed events panel -->
      <div class="cv-panel">
        <div class="panel-tabs">
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'calls' }"
            @click="activeTab = 'calls'"
          >
            <span class="tab-pip tab-pip--call"></span>
            Calls
            <span class="tab-badge">{{ upcomingCalls.length }}</span>
          </button>
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'bookings' }"
            @click="activeTab = 'bookings'"
          >
            <span class="tab-pip tab-pip--booking"></span>
            Bookings
            <span class="tab-badge">{{ upcomingBookings.length }}</span>
          </button>
        </div>

        <div class="panel-body">
          <!-- Calls tab -->
          <template v-if="activeTab === 'calls'">
            <div v-if="upcomingCalls.length === 0" class="bk-empty">
              <span class="bk-empty-icon">📹</span>
              <p>No upcoming video calls.</p>
              <p class="bk-empty-sub">
                Schedule a call from an enquiry conversation.
              </p>
            </div>
            <ul v-else class="bk-list">
              <li v-for="call in upcomingCalls" :key="call.id" class="bk-item">
                <div class="bk-date-badge bk-date-badge--call">
                  <span class="bk-month">{{
                    callMonthAbbr(call.scheduledAt)
                  }}</span>
                  <span class="bk-day">{{ callDayNum(call.scheduledAt) }}</span>
                </div>
                <div class="bk-info">
                  <p class="bk-couple">{{ call.coupleName ?? "Couple" }}</p>
                  <p class="bk-weekday">{{ callTime(call.scheduledAt) }}</p>
                </div>
                <span
                  class="bk-status"
                  :class="
                    call.status === 'PENDING'
                      ? 'status-pending'
                      : 'status-confirmed'
                  "
                >
                  {{ call.status === "PENDING" ? "Pending" : "Confirmed" }}
                </span>
              </li>
            </ul>
          </template>

          <!-- Bookings tab -->
          <template v-else>
            <div v-if="leadsStore.loading" class="bk-loading">Loading…</div>
            <div v-else-if="upcomingBookings.length === 0" class="bk-empty">
              <span class="bk-empty-icon">📅</span>
              <p>No upcoming bookings yet.</p>
              <p class="bk-empty-sub">
                Bookings with a set wedding date will appear here.
              </p>
            </div>
            <ul v-else class="bk-list">
              <li
                v-for="lead in upcomingBookings"
                :key="lead.id"
                class="bk-item"
              >
                <div class="bk-date-badge bk-date-badge--booking">
                  <span class="bk-month">{{ monthAbbr(lead.eventDate!) }}</span>
                  <span class="bk-day">{{ dayNum(lead.eventDate!) }}</span>
                </div>
                <div class="bk-info">
                  <p class="bk-couple">{{ lead.coupleName }}</p>
                  <p class="bk-weekday">{{ weekday(lead.eventDate!) }}</p>
                </div>
                <span class="bk-status" :class="STATUS_CLASS[lead.status]">
                  {{ STATUS_LABELS[lead.status] ?? lead.status }}
                </span>
              </li>
            </ul>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ── Page shell ──────────────────────────────────────── */
.cv-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.cv-title {
  margin: 0 0 4px;
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--color-text);
}

.cv-sub {
  color: var(--color-muted);
  margin: 0;
  font-size: 0.88rem;
}

/* ── Stat chips ──────────────────────────────────────── */
.cv-stats {
  display: flex;
  gap: 14px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.cv-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 14px 20px;
  flex: 1;
  min-width: 140px;
  transition: box-shadow 0.15s;
}

.cv-stat:hover {
  box-shadow: var(--shadow-md);
}
.cv-stat-icon {
  font-size: 1.4rem;
  line-height: 1;
}

.cv-stat-val {
  font-size: 1.5rem;
  font-weight: 800;
  margin: 0;
  line-height: 1.1;
  color: var(--color-text);
}

.cv-stat-label {
  font-size: 0.75rem;
  color: var(--color-muted);
  margin: 2px 0 0;
}

.cv-stat--blocked {
  border-left: 3px solid var(--color-muted);
}
.cv-stat--call {
  border-left: 3px solid #7c3aed;
}
.cv-stat--booking {
  border-left: 3px solid var(--color-gold);
}

/* ── Two-column body ─────────────────────────────────── */
.cv-body {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 900px) {
  .cv-body {
    grid-template-columns: 1fr;
  }
  .cv-stats {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
@media (max-width: 520px) {
  .cv-stats {
    grid-template-columns: 1fr;
  }
}

.cv-loading {
  color: var(--color-muted);
  text-align: center;
  padding: 48px;
}

/* ── Calendar column ─────────────────────────────────── */
.cv-calendar-col {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
}

/* ── Legend + hint ───────────────────────────────────── */
.cv-legend {
  display: flex;
  align-items: center;
  gap: 6px 14px;
  padding: 12px 20px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-wrap: wrap;
}

.legend-pip {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-pip--booking {
  background: var(--color-gold);
}
.legend-pip--call {
  background: #7c3aed;
}
.legend-pip--blocked {
  background: var(--color-border-strong);
}

.legend-lbl {
  font-size: 0.76rem;
  color: var(--color-muted);
  margin-right: 6px;
}

.cv-hint {
  font-size: 0.76rem;
  color: var(--color-muted);
  margin-left: auto;
  font-style: italic;
}

/* ── Tabbed panel ────────────────────────────────────── */
.cv-panel {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

@media (max-width: 900px) {
  .cv-panel {
    position: static;
    max-height: none;
  }
}

.panel-tabs {
  display: flex;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  padding: 0 8px;
  flex-shrink: 0;
}

.panel-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 14px;
  font-size: 0.81rem;
  font-weight: 600;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  color: var(--color-muted);
  transition:
    color 0.15s,
    border-color 0.15s;
  white-space: nowrap;
}

.panel-tab:hover {
  color: var(--color-text);
}
.panel-tab.active {
  color: var(--color-gold);
  border-bottom-color: var(--color-gold);
}

.tab-pip {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tab-pip--call {
  background: #7c3aed;
}
.tab-pip--booking {
  background: var(--color-gold);
}

.tab-badge {
  background: var(--color-surface-alt);
  color: var(--color-muted);
  font-size: 0.68rem;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 20px;
  min-width: 18px;
  text-align: center;
}

.panel-body {
  overflow-y: auto;
  padding: 16px;
  flex: 1;
}

/* ── Event items ─────────────────────────────────────── */
.bk-loading {
  text-align: center;
  color: var(--color-muted);
  padding: 32px 0;
  font-size: 0.9rem;
}

.bk-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 36px 16px;
  text-align: center;
  gap: 6px;
}

.bk-empty-icon {
  font-size: 2rem;
  margin-bottom: 4px;
}

.bk-empty p {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-text);
}

.bk-empty-sub {
  font-size: 0.8rem !important;
  color: var(--color-muted) !important;
}

.bk-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bk-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  transition: border-color 0.15s;
}

.bk-item:hover {
  border-color: var(--color-gold);
}

.bk-date-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-gold);
  color: var(--color-white);
  border-radius: 8px;
  width: 44px;
  height: 48px;
  flex-shrink: 0;
}

.bk-date-badge--booking {
  background: var(--color-gold);
}
.bk-date-badge--call {
  background: #7c3aed;
}

.bk-month {
  font-size: 0.6rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1;
}

.bk-day {
  font-size: 1.3rem;
  font-weight: 800;
  line-height: 1.2;
}

.bk-info {
  flex: 1;
  min-width: 0;
}

.bk-couple {
  margin: 0;
  font-size: 0.88rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--color-text);
}

.bk-weekday {
  margin: 2px 0 0;
  font-size: 0.76rem;
  color: var(--color-muted);
}

.bk-status {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-enquiry {
  background: var(--chip-orange-bg);
  color: var(--chip-orange-text);
}
.status-chat {
  background: var(--chip-blue-bg);
  color: var(--chip-blue-text);
}
.status-quote {
  background: var(--chip-teal-bg);
  color: var(--chip-teal-text);
}
.status-accepted {
  background: var(--chip-teal-bg);
  color: var(--chip-teal-text);
}
.status-deposit {
  background: var(--chip-green-bg);
  color: var(--color-green);
}

.status-confirmed {
  background: var(--color-gold-light);
  color: var(--color-gold);
  border: 1px solid var(--color-gold);
}

.status-pending {
  background: var(--color-amber-light);
  color: var(--color-amber);
  border: 1px solid var(--color-amber);
}
</style>
