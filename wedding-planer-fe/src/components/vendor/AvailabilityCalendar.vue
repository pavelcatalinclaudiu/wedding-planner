<script setup lang="ts">
import { ref, computed } from "vue";
import { Calendar, X } from "lucide-vue-next";

const props = defineProps<{
  modelValue: string; // selected date yyyy-MM-dd ('' = none)
  blockedDates: string[]; // ['yyyy-MM-dd', ...]
  loading?: boolean;
}>();

const emit = defineEmits<{
  "update:modelValue": [value: string];
}>();

/* ── constants ─────────────────────────────────────────────────────────────── */
const MONTH_NAMES = [
  "January",
  "February",
  "March",
  "April",
  "May",
  "June",
  "July",
  "August",
  "September",
  "October",
  "November",
  "December",
];
const DAY_HEADS = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"];

/* ── today (time-stripped) ─────────────────────────────────────────────────── */
const today = new Date();
today.setHours(0, 0, 0, 0);
const todayStr = today.toISOString().slice(0, 10);

/* ── view state ────────────────────────────────────────────────────────────── */
const viewYear = ref(today.getFullYear());
const viewMonth = ref(today.getMonth()); // 0-based

const canGoPrev = computed(
  () =>
    viewYear.value > today.getFullYear() || viewMonth.value > today.getMonth(),
);

function prevMonth() {
  if (viewMonth.value === 0) {
    viewYear.value--;
    viewMonth.value = 11;
  } else {
    viewMonth.value--;
  }
}
function nextMonth() {
  if (viewMonth.value === 11) {
    viewYear.value++;
    viewMonth.value = 0;
  } else {
    viewMonth.value++;
  }
}

/* ── grid cells ─────────────────────────────────────────────────────────────── */
interface DayCell {
  date: Date | null;
  str: string;
  day: number;
}

const calendarDays = computed<DayCell[]>(() => {
  const y = viewYear.value;
  const m = viewMonth.value;

  // Monday-first offset
  const firstDow = new Date(y, m, 1).getDay(); // 0=Sun
  const startOffset = (firstDow + 6) % 7; // 0=Mon
  const daysInMonth = new Date(y, m + 1, 0).getDate();

  const cells: DayCell[] = [];

  for (let i = 0; i < startOffset; i++) {
    cells.push({ date: null, str: "", day: 0 });
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const date = new Date(y, m, d);
    const str = `${y}-${String(m + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
    cells.push({ date, str, day: d });
  }

  // Pad tail so grid rows are complete (multiples of 7)
  while (cells.length % 7 !== 0) {
    cells.push({ date: null, str: "", day: 0 });
  }

  return cells;
});

/* ── cell state helpers ─────────────────────────────────────────────────────── */
function isPast(date: Date) {
  return date < today;
}
function isToday(str: string) {
  return str === todayStr;
}
function isBlocked(str: string) {
  return props.blockedDates.includes(str);
}
function isSelected(str: string) {
  return props.modelValue === str;
}

function cellClass(cell: DayCell) {
  if (!cell.date) return "cal-cell cal-empty";
  if (isPast(cell.date)) return "cal-cell cal-past";
  if (isSelected(cell.str)) return "cal-cell cal-selected";
  if (isBlocked(cell.str)) return "cal-cell cal-blocked";
  if (isToday(cell.str)) return "cal-cell cal-today";
  return "cal-cell cal-available";
}

function select(cell: DayCell) {
  if (!cell.date || isPast(cell.date) || isBlocked(cell.str)) return;
  // Toggle off if already selected
  emit("update:modelValue", isSelected(cell.str) ? "" : cell.str);
}

function clearSelection() {
  emit("update:modelValue", "");
}
</script>

<template>
  <div class="cal-wrapper">
    <!-- Loading overlay -->
    <div v-if="loading" class="cal-loading">
      <span class="cal-spinner"></span>
      <span>Loading availability…</span>
    </div>

    <template v-else>
      <!-- Navigation -->
      <div class="cal-nav">
        <button
          class="cal-nav-btn"
          :disabled="!canGoPrev"
          aria-label="Previous month"
          @click="prevMonth"
        >
          ‹
        </button>
        <span class="cal-month-label">
          {{ MONTH_NAMES[viewMonth] }} {{ viewYear }}
        </span>
        <button class="cal-nav-btn" aria-label="Next month" @click="nextMonth">
          ›
        </button>
      </div>

      <!-- Grid -->
      <div class="cal-grid">
        <!-- Day-of-week headers -->
        <div v-for="h in DAY_HEADS" :key="h" class="cal-head-cell">{{ h }}</div>

        <!-- Day cells -->
        <div
          v-for="(cell, i) in calendarDays"
          :key="i"
          :class="cellClass(cell)"
          :title="
            cell.date
              ? isBlocked(cell.str)
                ? 'Already booked'
                : isPast(cell.date)
                  ? 'Past date'
                  : ''
              : ''
          "
          @click="select(cell)"
        >
          <span v-if="cell.date" class="cal-day-num">{{ cell.day }}</span>
          <!-- Booked indicator -->
          <span
            v-if="cell.date && isBlocked(cell.str)"
            class="cal-booked-dot"
          ></span>
        </div>
      </div>

      <!-- Selected date display + clear -->
      <div v-if="modelValue" class="cal-selection-bar">
        <span>
          <Calendar :size="15" />
          {{
            new Date(modelValue + "T00:00:00").toLocaleDateString("en-GB", {
              day: "numeric",
              month: "long",
              year: "numeric",
            })
          }}
        </span>
        <button class="cal-clear-btn" @click="clearSelection">
          <X :size="14" /> Clear
        </button>
      </div>
      <p v-else class="cal-no-selection">
        Click a date to select it (optional)
      </p>
    </template>
  </div>
</template>

<style scoped>
/* ── wrapper ─────────────────────────────────────────────────────────────── */
.cal-wrapper {
  border: 1.5px solid var(--color-border);
  border-radius: 14px;
  padding: 16px;
  background: var(--color-surface);
  user-select: none;
}

/* ── loading ─────────────────────────────────────────────────────────────── */
.cal-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px 0;
  color: var(--color-muted);
  font-size: 0.82rem;
}
.cal-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--color-border);
  border-top-color: var(--color-gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ── navigation ──────────────────────────────────────────────────────────── */
.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.cal-month-label {
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-text);
}
.cal-nav-btn {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  width: 30px;
  height: 30px;
  font-size: 1.1rem;
  cursor: pointer;
  color: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.12s,
    border-color 0.12s;
  line-height: 1;
}
.cal-nav-btn:hover:not(:disabled) {
  background: var(--color-gold-light);
  border-color: var(--color-gold);
  color: var(--color-gold);
}
.cal-nav-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

/* ── grid ─────────────────────────────────────────────────────────────────── */
.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

/* Day-of-week header cells */
.cal-head-cell {
  text-align: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-muted);
  text-transform: uppercase;
  padding-bottom: 6px;
}

/* Base day cell */
.cal-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: background 0.12s;
  font-size: 0.82rem;
}

.cal-empty {
  cursor: default;
  visibility: hidden;
}

/* Past dates */
.cal-past {
  color: var(--color-muted);
  cursor: not-allowed;
  background: transparent;
}

/* Available */
.cal-available {
  color: var(--color-text);
  background: var(--color-white);
  border: 1px solid var(--color-border);
}
.cal-available:hover {
  background: var(--color-gold-light);
  border-color: rgba(201, 168, 76, 0.27);
}

/* Today (available but highlighted) */
.cal-today {
  color: var(--color-gold);
  font-weight: 700;
  background: var(--color-white);
  border: 1.5px solid rgba(201, 168, 76, 0.4);
}
.cal-today:hover {
  background: var(--color-gold-light);
}

/* Selected */
.cal-selected {
  background: var(--color-gold);
  color: var(--color-white);
  font-weight: 700;
  border: 1.5px solid var(--color-gold-dark);
}
.cal-selected:hover {
  background: var(--color-gold-dark);
}

/* Booked (blocked) */
.cal-blocked {
  background: var(--color-error-light);
  color: var(--color-muted);
  cursor: not-allowed;
  border: 1px solid var(--color-error-light);
}
.cal-booked-dot {
  position: absolute;
  bottom: 3px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-error);
}

.cal-day-num {
  line-height: 1;
}

/* ── selection bar ──────────────────────────────────────────────────────── */
.cal-selection-bar {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 0.82rem;
  color: var(--color-text-secondary);
  font-weight: 500;
}
.cal-clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.75rem;
  color: var(--color-muted);
  padding: 0;
}
.cal-clear-btn:hover {
  color: var(--color-error);
}

.cal-no-selection {
  margin: 10px 0 0;
  font-size: 0.75rem;
  color: var(--color-muted);
  text-align: center;
}
</style>
