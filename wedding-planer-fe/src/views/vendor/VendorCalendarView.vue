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
import type { Booking, VideoCall } from "@/types/lead.types";
import { Calendar, Video, X, CalendarClock } from "lucide-vue-next";

const calls = ref<VideoCall[]>([]);
const bookings = ref<Booking[]>([]);
const loadingBookings = ref(false);
const activeTab = ref<"bookings" | "calls">("bookings");
const { t } = useI18n();

// Modal state
const cancelTarget = ref<Booking | null>(null);
const rescheduleTarget = ref<Booking | null>(null);
const newDate = ref("");
const rescheduleNote = ref("");
const modalBusy = ref(false);
const modalError = ref("");
const minDate = computed(() => format(startOfToday(), "yyyy-MM-dd"));

async function fetchCalls() {
  try {
    calls.value = (await videoCallsApi.list()).data;
  } catch {
    calls.value = [];
  }
}

async function fetchBookings() {
  loadingBookings.value = true;
  try {
    bookings.value = (await bookingsApi.list()).data;
  } catch {
    bookings.value = [];
  } finally {
    loadingBookings.value = false;
  }
}

/* ── Bookings ─────────────────────────────────────────────────────────────── */
const today = startOfToday();

/** For a RESCHEDULE_PENDING booking the proposed date is the relevant date. */
function effectiveDate(b: Booking): string | undefined {
  if (b.status === "RESCHEDULE_PENDING" && b.proposedDate)
    return b.proposedDate;
  return b.weddingDate ?? undefined;
}

const upcomingBookings = computed<Booking[]>(() =>
  bookings.value
    .filter(
      (b) =>
        b.status !== "CANCELLED" &&
        (b.status === "RESCHEDULE_PENDING" ||
          (!!effectiveDate(b) &&
            !isBefore(parseISO(effectiveDate(b)!), today))),
    )
    .sort((a, b) => {
      const da = effectiveDate(a) ?? "9999-12-31";
      const db = effectiveDate(b) ?? "9999-12-31";
      return compareAsc(parseISO(da), parseISO(db));
    }),
);

function bookingStatusClass(status?: string) {
  if (status === "RESCHEDULE_PENDING") return "status-pending";
  return "status-confirmed";
}
function bookingStatusLabel(status?: string) {
  if (status === "RESCHEDULE_PENDING")
    return t("vendor.calendar.reschedulePending");
  return t("vendor.calendar.confirmed");
}

// Cancel modal
function openCancel(b: Booking) {
  cancelTarget.value = b;
  modalError.value = "";
}
async function confirmCancel() {
  if (!cancelTarget.value) return;
  modalBusy.value = true;
  modalError.value = "";
  try {
    await bookingsApi.cancel(cancelTarget.value.id);
    cancelTarget.value = null;
    // Re-fetch from server so the calendar grid is fully in sync
    await fetchBookings();
  } catch (e: any) {
    modalError.value = e?.response?.data?.message ?? "Something went wrong.";
  } finally {
    modalBusy.value = false;
  }
}

// Reschedule modal
function openReschedule(b: Booking) {
  rescheduleTarget.value = b;
  newDate.value = "";
  rescheduleNote.value = "";
  modalError.value = "";
}
async function confirmReschedule() {
  if (!rescheduleTarget.value || !newDate.value) return;
  modalBusy.value = true;
  modalError.value = "";
  try {
    const updated = await bookingsApi.proposeReschedule(
      rescheduleTarget.value.id,
      newDate.value,
      rescheduleNote.value || undefined,
    );
    const idx = bookings.value.findIndex(
      (b) => b.id === rescheduleTarget.value!.id,
    );
    if (idx !== -1) bookings.value[idx] = updated.data;
    rescheduleTarget.value = null;
  } catch (e: any) {
    modalError.value = e?.response?.data?.message ?? "Something went wrong.";
  } finally {
    modalBusy.value = false;
  }
}

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

const bookingDates = computed<string[]>(() =>
  bookings.value
    .filter((b) => b.status !== "CANCELLED" && !!effectiveDate(b))
    .map((b) => effectiveDate(b)!),
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
  await Promise.all([fetchCalls(), fetchBookings()]);
});
</script>

<template>
  <div class="calendar-view">
    <!-- Header -->
    <div class="cv-header">
      <div>
        <h2 class="cv-title">{{ t("vendor.calendar.title") }}</h2>
        <p class="cv-sub">{{ t("vendor.calendar.subtitle") }}</p>
      </div>
    </div>

    <!-- Stat chips -->
    <div class="cv-stats">
      <div class="cv-stat cv-stat--call">
        <span class="cv-stat-icon"><Video :size="20" /></span>
        <div>
          <p class="cv-stat-val">{{ upcomingCalls.length }}</p>
          <p class="cv-stat-label">{{ t("vendor.calendar.upcomingCalls") }}</p>
        </div>
      </div>
      <div class="cv-stat cv-stat--booking">
        <span class="cv-stat-icon"><Calendar :size="20" /></span>
        <div>
          <p class="cv-stat-val">{{ upcomingBookings.length }}</p>
          <p class="cv-stat-label">
            {{ t("vendor.calendar.upcomingBookings") }}
          </p>
        </div>
      </div>
    </div>

    <div class="cv-body">
      <!-- Calendar column -->
      <div class="cv-calendar-col">
        <div class="cv-section-header">
          <h3 class="cv-section-title">
            {{ t("vendor.calendar.calSectionTitle") }}
          </h3>
          <p class="cv-section-desc">
            {{ t("vendor.calendar.calSectionDesc") }}
          </p>
        </div>
        <CalendarGrid
          :blocked-dates="[]"
          :booking-dates="bookingDates"
          :video-call-dates="videoCallDates"
          :readonly="true"
        />
        <div class="cv-legend">
          <span class="legend-pip legend-pip--booking"></span
          ><span class="legend-lbl">{{
            t("vendor.calendar.legendBooked")
          }}</span>
          <span class="legend-pip legend-pip--call"></span
          ><span class="legend-lbl">{{ t("vendor.calendar.legendCall") }}</span>
        </div>
      </div>

      <!-- Right: Tabbed events panel -->
      <div class="cv-panel">
        <div class="cv-section-header cv-section-header--panel">
          <h3 class="cv-section-title">
            {{ t("vendor.calendar.eventsSectionTitle") }}
          </h3>
          <p class="cv-section-desc">
            {{ t("vendor.calendar.eventsSectionDesc") }}
          </p>
        </div>
        <div class="panel-tabs">
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'bookings' }"
            @click="activeTab = 'bookings'"
          >
            <span class="tab-pip tab-pip--booking"></span>
            {{ t("vendor.calendar.tabBookings") }}
            <span class="tab-badge">{{ upcomingBookings.length }}</span>
          </button>
          <button
            class="panel-tab"
            :class="{ active: activeTab === 'calls' }"
            @click="activeTab = 'calls'"
          >
            <span class="tab-pip tab-pip--call"></span>
            {{ t("vendor.calendar.tabCalls") }}
            <span class="tab-badge">{{ upcomingCalls.length }}</span>
          </button>
        </div>

        <div class="panel-body">
          <!-- Bookings tab (first) -->
          <template v-if="activeTab === 'bookings'">
            <div v-if="loadingBookings" class="bk-loading">
              {{ t("common.loading") }}
            </div>
            <div v-else-if="upcomingBookings.length === 0" class="bk-empty">
              <span class="bk-empty-icon"><Calendar :size="28" /></span>
              <p>{{ t("vendor.calendar.noUpcomingBookings") }}</p>
              <p class="bk-empty-sub">
                {{ t("vendor.calendar.bookingsHint") }}
              </p>
            </div>
            <ul v-else class="bk-list">
              <li
                v-for="b in upcomingBookings"
                :key="b.id"
                class="bk-item"
                :class="{ 'is-reschedule': b.status === 'RESCHEDULE_PENDING' }"
              >
                <div class="bk-date-badge bk-date-badge--booking">
                  <span class="bk-month">{{
                    monthAbbr(effectiveDate(b)!)
                  }}</span>
                  <span class="bk-day">{{ dayNum(effectiveDate(b)!) }}</span>
                </div>
                <div class="bk-info">
                  <div class="pf-av" :title="b.coupleName ?? 'Couple'">
                    <img
                      v-if="b.coupleProfilePicture"
                      :src="b.coupleProfilePicture"
                      class="pf-av-img"
                      alt=""
                    />
                    <template v-else>{{
                      (b.coupleName?.[0] ?? "?").toUpperCase()
                    }}</template>
                  </div>
                  <div>
                    <p class="bk-couple">{{ b.coupleName ?? "Couple" }}</p>
                    <p class="bk-weekday">{{ weekday(effectiveDate(b)!) }}</p>
                  </div>
                </div>
                <span class="bk-status" :class="bookingStatusClass(b.status)">
                  {{ bookingStatusLabel(b.status) }}
                </span>
                <div class="bk-actions">
                  <button
                    class="bk-act bk-act--reschedule"
                    :title="t('vendor.calendar.reschedule')"
                    @click="openReschedule(b)"
                  >
                    <CalendarClock :size="13" />
                  </button>
                  <button
                    class="bk-act bk-act--cancel"
                    :title="t('vendor.calendar.cancelBooking')"
                    @click="openCancel(b)"
                  >
                    <X :size="13" />
                  </button>
                </div>
              </li>
            </ul>
          </template>

          <!-- Calls tab -->
          <template v-else>
            <div v-if="upcomingCalls.length === 0" class="bk-empty">
              <span class="bk-empty-icon"><Video :size="28" /></span>
              <p>{{ t("vendor.calendar.noUpcomingCalls") }}</p>
              <p class="bk-empty-sub">
                {{ t("vendor.calendar.scheduleCallHint") }}
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
                  <div class="pf-av" :title="call.coupleName ?? 'Couple'">
                    <img
                      v-if="call.coupleProfilePicture"
                      :src="call.coupleProfilePicture"
                      class="pf-av-img"
                      alt=""
                    />
                    <template v-else>{{
                      (call.coupleName?.[0] ?? "?").toUpperCase()
                    }}</template>
                  </div>
                  <div>
                    <p class="bk-couple">{{ call.coupleName ?? "Couple" }}</p>
                    <p class="bk-weekday">{{ callTime(call.scheduledAt) }}</p>
                  </div>
                </div>
                <span
                  class="bk-status"
                  :class="
                    call.status === 'PENDING'
                      ? 'status-pending'
                      : 'status-confirmed'
                  "
                >
                  {{
                    call.status === "PENDING"
                      ? t("vendor.calendar.pending")
                      : t("vendor.calendar.confirmed")
                  }}
                </span>
              </li>
            </ul>
          </template>
        </div>
      </div>
    </div>

    <!-- ── Cancel Booking Modal ─────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="cancelTarget"
        class="vcal-overlay"
        @click.self="cancelTarget = null"
      >
        <div class="vcal-modal">
          <h3 class="vcal-modal-title">
            {{ t("vendor.calendar.cancelTitle") }}
          </h3>
          <p class="vcal-modal-body">
            {{
              t("vendor.calendar.cancelBody", {
                name: cancelTarget.coupleName,
              })
            }}
          </p>
          <p class="vcal-modal-warn">
            {{ t("vendor.calendar.cancelWarning") }}
          </p>
          <p v-if="modalError" class="vcal-modal-error">{{ modalError }}</p>
          <div class="vcal-modal-footer">
            <button
              class="vcal-btn vcal-btn--secondary"
              @click="cancelTarget = null"
            >
              {{ t("vendor.calendar.keepBooking") }}
            </button>
            <button
              class="vcal-btn vcal-btn--danger"
              :disabled="modalBusy"
              @click="confirmCancel"
            >
              {{ t("vendor.calendar.cancelBookingConfirm") }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- ── Reschedule Modal ──────────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="rescheduleTarget"
        class="vcal-overlay"
        @click.self="rescheduleTarget = null"
      >
        <div class="vcal-modal">
          <h3 class="vcal-modal-title">
            {{ t("vendor.calendar.rescheduleTitle") }}
          </h3>
          <p class="vcal-modal-body">
            {{
              t("vendor.calendar.rescheduleBody", {
                name: rescheduleTarget.coupleName,
              })
            }}
          </p>
          <div class="vcal-modal-field">
            <label class="vcal-modal-label">{{
              t("vendor.calendar.newDate")
            }}</label>
            <input
              type="date"
              v-model="newDate"
              :min="minDate"
              class="vcal-modal-input"
            />
          </div>
          <div class="vcal-modal-field">
            <label class="vcal-modal-label">{{
              t("vendor.calendar.noteOptional")
            }}</label>
            <textarea
              v-model="rescheduleNote"
              class="vcal-modal-textarea"
              :placeholder="t('vendor.calendar.notePlaceholder')"
              rows="3"
            ></textarea>
          </div>
          <p v-if="modalError" class="vcal-modal-error">{{ modalError }}</p>
          <div class="vcal-modal-footer">
            <button
              class="vcal-btn vcal-btn--secondary"
              @click="rescheduleTarget = null"
            >
              {{ t("vendor.calendar.cancelAction") }}
            </button>
            <button
              class="vcal-btn vcal-btn--primary"
              :disabled="modalBusy || !newDate"
              @click="confirmReschedule"
            >
              {{ t("vendor.calendar.sendProposal") }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
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

.cv-stat--call {
  border-left: 3px solid #7c3aed;
}
.cv-stat--booking {
  border-left: 3px solid var(--color-gold);
}

/* ── Two-column body ─────────────────────────────────── */
.cv-body {
  display: grid;
  grid-template-columns: 2fr 2fr;
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

/* ── Calendar column ─────────────────────────────────── */
.cv-calendar-col {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  overflow: hidden;
}

/* ── Section header (title + desc) ──────────────────── */
.cv-section-header {
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--color-border);
}
.cv-section-header--panel {
  background: var(--color-surface);
}
.cv-section-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 3px;
}
.cv-section-desc {
  font-size: 0.78rem;
  color: var(--color-muted);
  margin: 0;
  line-height: 1.45;
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
.legend-lbl {
  font-size: 0.76rem;
  color: var(--color-muted);
  margin-right: 6px;
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
  display: flex;
  align-items: center;
  gap: 8px;
}
.pf-av {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-gold);
  color: #fff;
  font-size: 0.68rem;
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
.pf-av:hover {
  transform: scale(3);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  z-index: 9999;
}
.pf-av-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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

/* ── Booking action buttons ──────────────────────────────── */
.bk-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.bk-act {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  background: var(--color-white);
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
  color: var(--color-muted);
}

.bk-act:hover {
  background: var(--color-surface-alt);
  border-color: var(--color-text);
  color: var(--color-text);
}

.bk-act--cancel:hover {
  border-color: var(--color-error);
  color: var(--color-error);
  background: var(--chip-red-bg);
}

.bk-act--reschedule:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
  background: var(--color-gold-light);
}

.is-reschedule {
  border-left: 3px solid var(--color-amber, #f59e0b);
}

/* ── Modals ──────────────────────────────────────────────── */
.vcal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}

.vcal-modal {
  background: var(--color-white);
  border-radius: 16px;
  padding: 28px 28px 24px;
  max-width: 420px;
  width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.vcal-modal-title {
  margin: 0 0 12px;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--color-text);
}

.vcal-modal-body {
  margin: 0 0 8px;
  font-size: 0.92rem;
  color: var(--color-text);
}

.vcal-modal-warn {
  margin: 0 0 16px;
  font-size: 0.83rem;
  color: var(--color-muted);
  padding: 10px 12px;
  background: var(--chip-amber-bg, #fef9c3);
  border-radius: 8px;
  border-left: 3px solid var(--color-amber, #f59e0b);
}

.vcal-modal-field {
  margin-bottom: 14px;
}

.vcal-modal-label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 6px;
}

.vcal-modal-input,
.vcal-modal-textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
  font-family: inherit;
  box-sizing: border-box;
  background: var(--color-white);
  color: var(--color-text);
}

.vcal-modal-textarea {
  resize: vertical;
  min-height: 72px;
}

.vcal-modal-error {
  color: var(--color-error);
  font-size: 0.82rem;
  margin: 0 0 12px;
}

.vcal-modal-footer {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.vcal-btn {
  padding: 9px 20px;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: none;
}

.vcal-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.vcal-btn--secondary {
  background: var(--color-surface);
  color: var(--color-muted);
  border: 1px solid var(--color-border);
}

.vcal-btn--danger {
  background: var(--color-error);
  color: #fff;
}

.vcal-btn--primary {
  background: var(--color-gold);
  color: #fff;
}

/* ── Mobile ─────────────────────────────────────────────────────────────── */
@media (max-width: 767px) {
  /* Booking items: wrap so date badge + info stack above status + actions */
  .bk-item {
    flex-wrap: wrap;
    row-gap: 8px;
  }

  .bk-date-badge {
    width: 40px;
    height: 44px;
  }

  .bk-day {
    font-size: 1.1rem !important;
  }

  .bk-info {
    flex: 1 1 calc(100% - 52px);
    min-width: 0;
  }

  .bk-status {
    margin-left: auto;
  }

  .bk-actions {
    width: 100%;
    justify-content: flex-end;
    border-top: 1px solid var(--color-border);
    padding-top: 8px;
    margin-top: 2px;
  }

  .bk-act {
    width: 32px;
    height: 32px;
  }

  /* Modal → bottom-sheet */
  .vcal-overlay {
    align-items: flex-end !important;
    padding: 0 !important;
  }

  .vcal-modal {
    border-radius: 20px 20px 0 0 !important;
    max-width: 100% !important;
    width: 100% !important;
    max-height: 92dvh !important;
    overflow-y: auto;
    padding: 24px 20px calc(env(safe-area-inset-bottom, 0px) + 24px) !important;
    box-sizing: border-box;
  }

  .vcal-modal::before {
    content: "";
    display: block;
    width: 40px;
    height: 4px;
    border-radius: 2px;
    background: var(--color-border);
    margin: 0 auto 18px;
  }

  /* Modal footer: stack buttons */
  .vcal-modal-footer {
    flex-direction: column;
  }

  .vcal-btn {
    width: 100%;
    text-align: center;
  }

  /* Stats top section: tighten */
  .cv-stats {
    gap: 10px !important;
  }
}

@media (max-width: 480px) {
  /* Two stats: single column on tiny screens */
  .cv-stats {
    grid-template-columns: 1fr !important;
  }
}
</style>
