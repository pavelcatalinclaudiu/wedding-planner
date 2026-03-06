<script setup lang="ts">
import { ref, computed } from "vue";
import {
  addMonths,
  subMonths,
  startOfMonth,
  endOfMonth,
  eachDayOfInterval,
  getDay,
  format,
  isSameMonth,
  isToday,
} from "date-fns";

const props = defineProps<{
  blockedDates: string[];
  /** Dates with a scheduled video call — shown in purple */
  videoCallDates?: string[];
  /** Dates with a confirmed wedding booking — shown in gold */
  bookingDates?: string[];
  /** Task due dates — shown in teal; overdue tasks shown in red */
  taskDates?: { date: string; done: boolean; overdue: boolean }[];
  /** When true, clicking a cell does nothing (no block/unblock) */
  readonly?: boolean;
}>();
const emit = defineEmits<{ block: [date: string]; unblock: [date: string] }>();

const current = ref(new Date());

const days = computed(() => {
  const start = startOfMonth(current.value);
  const end = endOfMonth(current.value);
  const allDays = eachDayOfInterval({ start, end });
  const firstDow = getDay(start); // 0 = Sun
  const padding = Array(firstDow).fill(null);
  return [...padding, ...allDays];
});

function toggle(day: Date | null) {
  if (!day || props.readonly) return;
  const str = format(day, "yyyy-MM-dd");
  if (props.blockedDates.includes(str)) {
    emit("unblock", str);
  } else {
    emit("block", str);
  }
}

function isBlocked(day: Date) {
  return props.blockedDates.includes(format(day, "yyyy-MM-dd"));
}

function isVideoCall(day: Date) {
  return (props.videoCallDates ?? []).includes(format(day, "yyyy-MM-dd"));
}

function isBooking(day: Date) {
  return (props.bookingDates ?? []).includes(format(day, "yyyy-MM-dd"));
}

function isTask(day: Date) {
  const str = format(day, "yyyy-MM-dd");
  return (props.taskDates ?? []).some((t) => t.date === str && !t.done);
}

function isTaskOverdue(day: Date) {
  const str = format(day, "yyyy-MM-dd");
  return (props.taskDates ?? []).some(
    (t) => t.date === str && !t.done && t.overdue,
  );
}
</script>

<template>
  <div class="calendar-grid">
    <div class="cal-nav">
      <button class="nav-btn" @click="current = subMonths(current, 1)">
        ‹
      </button>
      <span class="cal-month">{{ format(current, "MMMM yyyy") }}</span>
      <button class="nav-btn" @click="current = addMonths(current, 1)">
        ›
      </button>
    </div>

    <div class="day-headers">
      <span
        v-for="d in ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']"
        :key="d"
        class="day-header"
        >{{ d }}</span
      >
    </div>

    <div class="days-grid">
      <div
        v-for="(day, i) in days"
        :key="i"
        class="day-cell"
        :class="{
          empty: !day,
          blocked: day && isBlocked(day),
          booking: day && !isBlocked(day) && isBooking(day),
          'video-call':
            day && !isBlocked(day) && !isBooking(day) && isVideoCall(day),
          'task-overdue':
            day &&
            !isBlocked(day) &&
            !isBooking(day) &&
            !isVideoCall(day) &&
            isTaskOverdue(day),
          task:
            day &&
            !isBlocked(day) &&
            !isBooking(day) &&
            !isVideoCall(day) &&
            !isTaskOverdue(day) &&
            isTask(day),
          today:
            day &&
            !isBlocked(day) &&
            !isBooking(day) &&
            !isVideoCall(day) &&
            isToday(day),
          'other-month': day && !isSameMonth(day, current),
          'cal-readonly': props.readonly,
        }"
        @click="toggle(day)"
      >
        <span v-if="day">{{ format(day, "d") }}</span>
      </div>
    </div>

    <div class="cal-legend">
      <span v-if="!readonly" class="legend-item"
        ><span class="legend-dot blocked"></span> Blocked</span
      >
      <span class="legend-item"
        ><span class="legend-dot available"></span> Available</span
      >
      <span v-if="(bookingDates ?? []).length > 0" class="legend-item"
        ><span class="legend-dot booking"></span> Booked</span
      >
      <span
        v-if="(videoCallDates ?? []).length > 0 || readonly"
        class="legend-item"
        ><span class="legend-dot video-call"></span> Video Call</span
      >
      <span v-if="(taskDates ?? []).length > 0" class="legend-item"
        ><span class="legend-dot task"></span> Task Due</span
      >
    </div>
  </div>
</template>

<style scoped>
.calendar-grid {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 20px;
  max-width: 480px;
}
.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.nav-btn {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.nav-btn:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}
.cal-month {
  font-weight: 700;
  font-size: 0.95rem;
}
.day-headers {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  margin-bottom: 4px;
}
.day-header {
  text-align: center;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-muted);
  text-transform: uppercase;
  padding: 4px 0;
}
.days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}
.day-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: background 0.15s;
}
.day-cell:not(.empty):hover {
  background: var(--color-gold-light, #fdf8ee);
}
.day-cell.today {
  font-weight: 800;
  color: var(--color-gold);
}
.day-cell.blocked {
  background: var(--chip-red-bg);
  color: var(--color-error);
  font-weight: 600;
}
.day-cell.empty {
  cursor: default;
}
.day-cell.other-month {
  opacity: 0.3;
}
.cal-legend {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  font-size: 0.78rem;
  color: var(--color-muted);
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
}
.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}
.legend-dot.blocked {
  background: var(--chip-red-bg);
  border: 1px solid var(--color-error);
}
.legend-dot.available {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}
.day-cell.booking {
  background: var(--color-gold-light, #fdf8ee);
  color: var(--color-gold, #c9a84c);
  font-weight: 800;
  box-shadow: inset 0 0 0 2px var(--color-gold, #c9a84c);
}
.legend-dot.booking {
  background: var(--color-gold-light, #fdf8ee);
  border: 2px solid var(--color-gold, #c9a84c);
}
.day-cell.video-call {
  background: #ede9fe;
  color: #7c3aed;
  font-weight: 700;
}
.legend-dot.video-call {
  background: #ede9fe;
  border: 1px solid #7c3aed;
}
.day-cell.task {
  background: #ccfbf1;
  color: #0d9488;
  font-weight: 700;
}
.day-cell.task-overdue {
  background: #fee2e2;
  color: #dc2626;
  font-weight: 700;
}
.legend-dot.task {
  background: #ccfbf1;
  border: 1px solid #0d9488;
}
.day-cell.cal-readonly {
  cursor: default;
}
</style>
