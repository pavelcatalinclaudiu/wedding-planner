<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { videoCallsApi } from "@/api/videoCalls.api";
import { bookingsApi } from "@/api/bookings.api";
import CalendarGrid from "@/components/ui/CalendarGrid.vue";
import {
  format,
  parseISO,
  isAfter,
  isBefore,
  startOfToday,
  compareAsc,
} from "date-fns";
import type { VideoCall, Booking } from "@/types/lead.types";
import { useChecklistStore } from "@/stores/checklist.store";

const { t } = useI18n();

const calls = ref<VideoCall[]>([]);
const bookings = ref<Booking[]>([]);
const loading = ref(false);
const today = startOfToday();
const checklistStore = useChecklistStore();
const activeTab = ref<"bookings" | "calls" | "tasks">("bookings");

async function fetchAll() {
  loading.value = true;
  try {
    const [callsRes, bookingsRes] = await Promise.all([
      videoCallsApi.list(),
      bookingsApi.list(),
    ]);
    calls.value = callsRes.data;
    bookings.value = bookingsRes.data;
    if (!checklistStore.items.length) await checklistStore.fetchChecklist();
  } catch {
    calls.value = [];
    bookings.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(fetchAll);

/* ── Upcoming calls (not completed/cancelled, in the future) ── */
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

const pastCalls = computed<VideoCall[]>(() =>
  calls.value
    .filter((c) => c.status === "COMPLETED" || c.status === "CANCELLED")
    .sort((a, b) =>
      compareAsc(parseISO(b.scheduledAt), parseISO(a.scheduledAt)),
    )
    .slice(0, 5),
);

/** Dates (yyyy-MM-dd) that have a video call — fed to CalendarGrid. */
const videoCallDates = computed<string[]>(() =>
  upcomingCalls.value.map((c) => format(parseISO(c.scheduledAt), "yyyy-MM-dd")),
);

/** Dates (yyyy-MM-dd) that have a wedding booking — shown gold on CalendarGrid. */
const bookingDates = computed<string[]>(() =>
  bookings.value.filter((b) => !!b.weddingDate).map((b) => b.weddingDate!),
);

/** Task due dates for pending (not done) checklist items. */
const taskDates = computed(() =>
  checklistStore.items
    .filter((i) => !i.done && !!i.dueDate)
    .map((i) => ({
      date: i.dueDate!,
      done: false,
      overdue: isBefore(parseISO(i.dueDate!), today),
    })),
);

/** Upcoming (not done) checklist items with a due date, sorted by due date. */
const upcomingTasks = computed(() =>
  checklistStore.items
    .filter((i) => !i.done && !!i.dueDate)
    .sort((a, b) => compareAsc(parseISO(a.dueDate!), parseISO(b.dueDate!)))
    .slice(0, 8),
);

function bookingDay(dateStr: string) {
  return format(parseISO(dateStr), "d");
}
function bookingMonth(dateStr: string) {
  return format(parseISO(dateStr), "MMM").toUpperCase();
}
function bookingYear(dateStr: string) {
  return format(parseISO(dateStr), "yyyy");
}

function callTime(iso: string) {
  return format(parseISO(iso), "HH:mm");
}
function callDayNum(iso: string) {
  return format(parseISO(iso), "d");
}
function callMonthAbbr(iso: string) {
  return format(parseISO(iso), "MMM").toUpperCase();
}
function taskDayNum(dateStr: string) {
  return format(parseISO(dateStr), "d");
}
function taskMonthAbbr(dateStr: string) {
  return format(parseISO(dateStr), "MMM").toUpperCase();
}
function isTaskOverdue(dateStr: string) {
  return isBefore(parseISO(dateStr), today);
}
</script>

<template>
  <div class="calendar-view">
    <!-- Header -->
    <div class="cv-header">
      <div>
        <h2 class="cv-title">{{ t("calendar.pageTitle") }}</h2>
        <p class="cv-sub">
          {{ t("calendar.subtitle") }}
        </p>
      </div>
    </div>

    <!-- Stat chips -->
    <div class="cv-stats">
      <div class="cv-stat cv-stat--booking">
        <span class="cv-stat-icon">💍</span>
        <div>
          <p class="cv-stat-val">{{ bookings.length }}</p>
          <p class="cv-stat-label">{{ t("calendar.weddingBookings") }}</p>
        </div>
      </div>
      <div class="cv-stat cv-stat--call">
        <span class="cv-stat-icon">📹</span>
        <div>
          <p class="cv-stat-val">{{ upcomingCalls.length }}</p>
          <p class="cv-stat-label">{{ t("calendar.upcomingCalls") }}</p>
        </div>
      </div>
      <div class="cv-stat cv-stat--task">
        <span class="cv-stat-icon">✅</span>
        <div>
          <p class="cv-stat-val">{{ upcomingTasks.length }}</p>
          <p class="cv-stat-label">{{ t("calendar.pendingTasks") }}</p>
        </div>
      </div>
    </div>

    <div class="cv-body">
      <!-- Calendar column -->
      <div class="cv-calendar-col">
        <div v-if="loading" class="cv-loading">{{ t("common.loading") }}</div>
        <CalendarGrid
          v-else
          :blocked-dates="[]"
          :video-call-dates="videoCallDates"
          :booking-dates="bookingDates"
          :task-dates="taskDates"
          :readonly="true"
        />
        <div class="cv-legend">
          <span class="legend-pip legend-pip--booking"></span
          ><span class="legend-lbl">{{ t("calendar.legend.booking") }}</span>
          <span class="legend-pip legend-pip--call"></span
          ><span class="legend-lbl">{{ t("calendar.legend.call") }}</span>
          <span class="legend-pip legend-pip--task"></span
          ><span class="legend-lbl">{{ t("calendar.legend.task") }}</span>
          <span class="legend-pip legend-pip--overdue"></span
          ><span class="legend-lbl">{{ t("calendar.legend.overdue") }}</span>
        </div>
      </div>

      <!-- Right: Tabbed events panel -->
      <div class="cv-panel">
        <div class="panel-tabs">
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'bookings' }"
            @click="activeTab = 'bookings'"
          >
            <span class="tab-pip tab-pip--booking"></span>
            {{ t("calendar.tabs.bookings") }}
            <span class="tab-badge">{{ bookings.length }}</span>
          </button>
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'calls' }"
            @click="activeTab = 'calls'"
          >
            <span class="tab-pip tab-pip--call"></span>
            {{ t("calendar.tabs.calls") }}
            <span class="tab-badge">{{ upcomingCalls.length }}</span>
          </button>
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'tasks' }"
            @click="activeTab = 'tasks'"
          >
            <span class="tab-pip tab-pip--task"></span>
            {{ t("calendar.tabs.tasks") }}
            <span
              class="tab-badge"
              :class="{
                'tab-badge--warn': upcomingTasks.some((t) =>
                  isTaskOverdue(t.dueDate!),
                ),
              }"
              >{{ upcomingTasks.length }}</span
            >
          </button>
        </div>

        <div class="panel-body">
          <!-- Bookings tab -->
          <template v-if="activeTab === 'bookings'">
            <div v-if="loading" class="bk-loading">
              {{ t("common.loading") }}
            </div>
            <div v-else-if="bookings.length === 0" class="bk-empty">
              <span class="bk-empty-icon">💍</span>
              <p>{{ t("calendar.noBookings") }}</p>
              <p class="bk-empty-sub">
                {{ t("calendar.noBookingsSub") }}
              </p>
            </div>
            <ul v-else class="bk-list">
              <li v-for="b in bookings" :key="b.id" class="bk-item">
                <div class="bk-date-badge bk-date-badge--booking">
                  <span class="bk-month">{{
                    bookingMonth(b.weddingDate!)
                  }}</span>
                  <span class="bk-day">{{ bookingDay(b.weddingDate!) }}</span>
                </div>
                <div class="bk-info">
                  <p class="bk-couple">{{ b.vendorName ?? "Vendor" }}</p>
                  <p class="bk-weekday">
                    {{ b.vendorCategory ?? "" }} ·
                    {{ bookingYear(b.weddingDate!) }}
                  </p>
                </div>
                <span class="bk-status status-confirmed">{{
                  t("common.booked")
                }}</span>
              </li>
            </ul>
          </template>

          <!-- Calls tab -->
          <template v-else-if="activeTab === 'calls'">
            <div v-if="loading" class="bk-loading">
              {{ t("common.loading") }}
            </div>
            <div
              v-else-if="upcomingCalls.length === 0 && pastCalls.length === 0"
              class="bk-empty"
            >
              <span class="bk-empty-icon">📹</span>
              <p>{{ t("videoCalls.noCalls") }}</p>
              <p class="bk-empty-sub">
                {{ t("calendar.noCallsSub") }}
              </p>
            </div>
            <template v-else>
              <p v-if="upcomingCalls.length" class="section-label">
                {{ t("calendar.upcoming") }}
              </p>
              <ul class="bk-list">
                <li
                  v-for="call in upcomingCalls"
                  :key="call.id"
                  class="bk-item"
                >
                  <div class="bk-date-badge bk-date-badge--call">
                    <span class="bk-month">{{
                      callMonthAbbr(call.scheduledAt)
                    }}</span>
                    <span class="bk-day">{{
                      callDayNum(call.scheduledAt)
                    }}</span>
                  </div>
                  <div class="bk-info">
                    <p class="bk-couple">{{ call.vendorName ?? "Vendor" }}</p>
                    <p class="bk-weekday">{{ callTime(call.scheduledAt) }}</p>
                  </div>
                  <span
                    class="bk-status"
                    :class="
                      call.status === 'PENDING'
                        ? 'status-pending'
                        : call.status === 'IN_PROGRESS'
                          ? 'status-live'
                          : 'status-confirmed'
                    "
                  >
                    {{
                      call.status === "PENDING"
                        ? t("common.pending")
                        : call.status === "IN_PROGRESS"
                          ? "Live"
                          : t("common.confirmed")
                    }}
                  </span>
                </li>
              </ul>
              <template v-if="pastCalls.length > 0">
                <p
                  class="section-label section-label--muted"
                  style="margin-top: 16px"
                >
                  {{ t("calendar.past") }}
                </p>
                <ul class="bk-list">
                  <li
                    v-for="call in pastCalls"
                    :key="call.id"
                    class="bk-item bk-item--past"
                  >
                    <div class="bk-date-badge bk-date-badge--past">
                      <span class="bk-month">{{
                        callMonthAbbr(call.scheduledAt)
                      }}</span>
                      <span class="bk-day">{{
                        callDayNum(call.scheduledAt)
                      }}</span>
                    </div>
                    <div class="bk-info">
                      <p class="bk-couple">{{ call.vendorName ?? "Vendor" }}</p>
                      <p class="bk-weekday">{{ callTime(call.scheduledAt) }}</p>
                    </div>
                    <span
                      class="bk-status"
                      :class="
                        call.status === 'CANCELLED'
                          ? 'status-cancelled'
                          : 'status-done'
                      "
                    >
                      {{
                        call.status === "CANCELLED"
                          ? t("common.cancelled")
                          : t("common.completed")
                      }}
                    </span>
                  </li>
                </ul>
              </template>
            </template>
          </template>

          <!-- Tasks tab -->
          <template v-else>
            <div v-if="loading" class="bk-loading">
              {{ t("common.loading") }}
            </div>
            <div v-else-if="upcomingTasks.length === 0" class="bk-empty">
              <span class="bk-empty-icon">✅</span>
              <p>{{ t("calendar.pendingTasks") }}.</p>
              <p class="bk-empty-sub">{{ t("checklist.addTask") }}.</p>
            </div>
            <ul v-else class="bk-list">
              <li v-for="task in upcomingTasks" :key="task.id" class="bk-item">
                <div
                  class="bk-date-badge"
                  :class="
                    isTaskOverdue(task.dueDate!)
                      ? 'bk-date-badge--overdue'
                      : 'bk-date-badge--task'
                  "
                >
                  <span class="bk-month">{{
                    taskMonthAbbr(task.dueDate!)
                  }}</span>
                  <span class="bk-day">{{ taskDayNum(task.dueDate!) }}</span>
                </div>
                <div class="bk-info">
                  <p class="bk-couple">{{ task.label }}</p>
                  <p class="bk-weekday">{{ task.timePeriod ?? "" }}</p>
                </div>
                <span
                  class="bk-status"
                  :class="
                    isTaskOverdue(task.dueDate!)
                      ? 'status-overdue'
                      : 'status-task'
                  "
                >
                  {{
                    isTaskOverdue(task.dueDate!)
                      ? t("checklist.stats.overdue")
                      : t("common.pending")
                  }}
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
/* ── Page shell ──────────────────────────────────────────────── */
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

/* ── Stat chips ──────────────────────────────────────────────── */
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

.cv-stat--booking {
  border-left: 3px solid var(--color-gold);
}
.cv-stat--call {
  border-left: 3px solid #7c3aed;
}
.cv-stat--task {
  border-left: 3px solid #0891b2;
}

/* ── Two-column body ─────────────────────────────────────────── */
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

/* ── Calendar column ─────────────────────────────────────────── */
.cv-calendar-col {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
}

/* ── Legend ──────────────────────────────────────────────────── */
.cv-legend {
  display: flex;
  align-items: center;
  gap: 6px 16px;
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
.legend-pip--task {
  background: #0891b2;
}
.legend-pip--overdue {
  background: var(--color-error);
}

.legend-lbl {
  font-size: 0.76rem;
  color: var(--color-muted);
  margin-right: 8px;
}

/* ── Tabbed panel ────────────────────────────────────────────── */
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
  padding: 12px 12px;
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

.tab-pip--booking {
  background: var(--color-gold);
}
.tab-pip--call {
  background: #7c3aed;
}
.tab-pip--task {
  background: #0891b2;
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

.tab-badge--warn {
  background: var(--color-error-light);
  color: var(--color-error);
}

.panel-body {
  overflow-y: auto;
  padding: 16px;
  flex: 1;
}

.section-label {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--color-gold);
  margin: 0 0 10px;
}

.section-label--muted {
  color: var(--color-muted);
}

/* ── Placeholder for old class referenced by panel ── */
.cv-bookings-col {
  display: none;
}

/* (bookings-heading replaced by section-label) */

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
  gap: 10px;
}

.bk-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  background: var(--color-surface, #fafafa);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  transition: border-color 0.15s;
}

.bk-item:hover {
  border-color: var(--color-gold);
}

.bk-item--past {
  opacity: 0.65;
}

/* Date badge */
.bk-date-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-gold, #c9a84c);
  color: #fff;
  border-radius: 8px;
  width: 46px;
  height: 50px;
  flex-shrink: 0;
}

.bk-date-badge--booking {
  background: var(--color-gold, #c9a84c);
}

.bk-date-badge--call {
  background: #7c3aed;
}

.bk-date-badge--past {
  background: var(--color-muted);
}

.bk-date-badge--task {
  background: #0891b2;
}

.bk-date-badge--overdue {
  background: var(--color-error, #e53e3e);
}

.bk-month {
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1;
}

.bk-day {
  font-size: 1.35rem;
  font-weight: 800;
  line-height: 1.2;
}

/* Info */
.bk-info {
  flex: 1;
  min-width: 0;
}

.bk-couple {
  margin: 0;
  font-size: 0.9rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bk-weekday {
  margin: 2px 0 0;
  font-size: 0.78rem;
  color: var(--color-muted);
}

/* Status chips */
.bk-status {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 4px 8px;
  border-radius: 20px;
  white-space: nowrap;
  flex-shrink: 0;
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

.status-live {
  background: var(--color-green-light);
  color: var(--color-green);
  border: 1px solid var(--color-green);
}

.status-done {
  background: var(--color-surface);
  color: var(--color-muted);
  border: 1px solid var(--color-border);
}

.status-cancelled {
  background: var(--chip-red-bg);
  color: var(--color-error);
  border: 1px solid var(--color-error);
}

.status-task {
  background: var(--color-info-light);
  color: var(--color-info);
  border: 1px solid var(--color-info);
}

.status-overdue {
  background: var(--chip-red-bg, #fff1f0);
  color: var(--color-error, #e53e3e);
  border: 1px solid var(--color-error, #e53e3e);
}
</style>
