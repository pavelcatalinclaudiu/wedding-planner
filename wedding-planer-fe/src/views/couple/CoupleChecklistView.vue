<template>
  <div class="checklist-page">
    <!-- Top Header Bar -->
    <div class="page-header">
      <div class="header-left">
        <h2>{{ t("checklist.pageTitle") }}</h2>
        <p class="subtitle">{{ t("checklist.subtitle") }}</p>
      </div>
      <button class="btn-primary" @click="openAdd">
        <span class="btn-icon">+</span> {{ t("checklist.addTask") }}
      </button>
    </div>

    <!-- Two-column layout -->
    <div class="checklist-layout">
      <!-- ── Sidebar ── -->
      <aside class="cl-sidebar">
        <!-- Circular Progress Ring -->
        <div class="ring-card">
          <svg class="progress-ring" viewBox="0 0 100 100">
            <circle class="ring-bg" cx="50" cy="50" r="40" />
            <circle
              class="ring-fill"
              cx="50"
              cy="50"
              r="40"
              :stroke-dasharray="ringCircumference"
              :stroke-dashoffset="ringOffset"
            />
          </svg>
          <div class="ring-label">
            <span class="ring-pct">{{ globalPct }}%</span>
            <span class="ring-sub">{{ t("checklist.complete") }}</span>
          </div>
        </div>

        <!-- Stat Cards -->
        <div class="stat-grid">
          <div class="stat-card stat-done">
            <span class="stat-value">{{ checklistStore.done }}</span>
            <span class="stat-name">{{ t("checklist.stats.done") }}</span>
          </div>
          <div class="stat-card stat-pending">
            <span class="stat-value">{{
              checklistStore.total - checklistStore.done
            }}</span>
            <span class="stat-name">{{ t("checklist.stats.pending") }}</span>
          </div>
          <div
            class="stat-card stat-overdue"
            :class="{ 'stat-alert': checklistStore.overdue > 0 }"
          >
            <span class="stat-value">{{ checklistStore.overdue }}</span>
            <span class="stat-name">{{ t("checklist.stats.overdue") }}</span>
          </div>
          <div
            class="stat-card stat-urgent"
            :class="{ 'stat-alert-amber': checklistStore.urgent > 0 }"
          >
            <span class="stat-value">{{ checklistStore.urgent }}</span>
            <span class="stat-name">{{ t("checklist.stats.dueSoon") }}</span>
          </div>
        </div>

        <!-- Filter Nav -->
        <nav class="sidebar-nav">
          <p class="nav-label">{{ t("checklist.filterLabel") }}</p>
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="nav-btn"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <span class="nav-icon"
              ><component :is="tab.icon" :size="16"
            /></span>
            <span class="nav-text">{{ tab.label }}</span>
            <span class="nav-count">{{ tabCount(tab.key) }}</span>
          </button>
        </nav>

        <!-- All-done celebration -->
        <div
          v-if="
            checklistStore.total > 0 &&
            checklistStore.done === checklistStore.total
          "
          class="celebration-card"
        >
          <Sparkles :size="16" /> {{ t("checklist.completedMsg") }}
        </div>
      </aside>

      <!-- ── Main content ── -->
      <main class="cl-main">
        <!-- Loading -->
        <div v-if="checklistStore.loading" class="loading-state">
          <div class="spinner"></div>
          <p>{{ t("checklist.loading") }}</p>
        </div>

        <!-- Empty -->
        <div v-else-if="checklistStore.total === 0" class="empty-state">
          <div class="empty-icon"><ClipboardList :size="48" /></div>
          <h3>{{ t("checklist.noTasks") }}</h3>
          <p>{{ t("checklist.startAdding") }}</p>
          <button class="btn-primary" @click="openAdd">
            {{ t("checklist.addFirstTask") }}
          </button>
        </div>

        <!-- Groups -->
        <div v-else class="groups">
          <div
            v-for="period in orderedPeriods"
            :key="period"
            class="group-card"
          >
            <!-- Group Header -->
            <div class="group-header" @click="toggleGroup(period)">
              <div class="group-header-top">
                <div class="group-title-row">
                  <span
                    class="group-chevron"
                    :class="{ collapsed: collapsedGroups.has(period) }"
                  >
                    ▾
                  </span>
                  <span class="group-name">{{ period }}</span>
                  <span class="group-count">{{
                    groupItems(period).length
                  }}</span>
                </div>
                <span class="group-pct-label">{{ groupPct(period) }}%</span>
              </div>
              <div class="group-track">
                <div
                  class="group-fill"
                  :style="{ width: groupPct(period) + '%' }"
                ></div>
              </div>
            </div>

            <!-- Task list -->
            <div v-show="!collapsedGroups.has(period)" class="group-items">
              <div
                v-for="item in groupItems(period)"
                :key="item.id"
                class="task-row"
                :class="{
                  'task-done': item.done,
                  'task-overdue': isOverdue(item),
                  'task-urgent': isUrgent(item),
                }"
              >
                <button
                  class="check-btn"
                  @click="checklistStore.toggleItem(item.id)"
                >
                  <span class="check-icon"
                    ><Check v-if="item.done" :size="14"
                  /></span>
                </button>
                <div class="task-body">
                  <span class="task-label">{{ item.label }}</span>
                  <p v-if="item.notes" class="task-note">{{ item.notes }}</p>
                  <div class="task-meta">
                    <span
                      v-if="item.dueDate"
                      class="due-chip"
                      :class="dueDateClass(item)"
                    >
                      {{ dueDateLabel(item) }}
                    </span>
                  </div>
                </div>
                <div class="task-actions">
                  <button class="icon-btn" title="Edit" @click="openEdit(item)">
                    <Pencil :size="14" />
                  </button>
                  <button
                    class="icon-btn danger"
                    title="Delete"
                    @click="deleteTask(item.id)"
                  >
                    <Trash2 :size="14" />
                  </button>
                </div>
              </div>
              <div v-if="groupItems(period).length === 0" class="empty-group">
                {{ t("checklist.noTasksInPeriod") }}
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>

    <!-- Add / Edit Modal -->
    <Teleport to="body">
      <div v-if="modal.open" class="modal-overlay" @click.self="closeModal">
        <div class="modal">
          <div class="modal-header">
            <h2>
              {{
                modal.isEdit ? t("checklist.editTask") : t("checklist.addTask")
              }}
            </h2>
            <button class="close-btn" @click="closeModal">
              <X :size="18" />
            </button>
          </div>
          <div class="modal-body">
            <div class="form-field">
              <label>{{ t("checklist.taskTitle") }} *</label>
              <input
                v-model="modal.form.label"
                type="text"
                :placeholder="t('checklist.taskTitle')"
              />
            </div>
            <div class="form-field">
              <label>{{ t("checklist.timePeriod") }}</label>
              <select v-model="modal.form.timePeriod">
                <option value="">{{ t("checklist.generalPeriod") }}</option>
                <option v-for="p in TIME_PERIODS" :key="p" :value="p">
                  {{ p }}
                </option>
              </select>
            </div>
            <div class="form-field">
              <label>{{ t("checklist.dueDate") }}</label>
              <input v-model="modal.form.dueDate" type="date" />
            </div>
            <div class="form-field">
              <label>{{ t("checklist.taskNote") }}</label>
              <textarea
                v-model="modal.form.notes"
                rows="3"
                :placeholder="t('checklist.optionalNotes')"
              ></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closeModal">
              {{ t("common.cancel") }}
            </button>
            <button
              class="btn-primary"
              :disabled="!modal.form.label.trim()"
              @click="saveModal"
            >
              {{
                modal.isEdit
                  ? t("checklist.saveChanges")
                  : t("checklist.addTask")
              }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from "vue";
import type { Component } from "vue";
import { useI18n } from "vue-i18n";
import { useChecklistStore } from "@/stores/checklist.store";
import { useConfirm } from "@/composables/useConfirm";
import type { ChecklistItem } from "@/types/couple.types";
import { format, isBefore, addDays, parseISO } from "date-fns";
import {
  List,
  Clock,
  AlertCircle,
  CheckCircle2,
  Check,
  X,
  Sparkles,
  ClipboardList,
  Pencil,
  Trash2,
} from "lucide-vue-next";

const { t } = useI18n();
const checklistStore = useChecklistStore();

const TIME_PERIODS = [
  "12+ Months Before",
  "6–12 Months Before",
  "3–6 Months Before",
  "1–3 Months Before",
  "2–4 Weeks Before",
  "Wedding Week",
];

const tabs = computed<{ key: string; label: string; icon: Component }[]>(() => [
  { key: "all", label: t("checklist.filters.all"), icon: List },
  { key: "pending", label: t("checklist.filters.pending"), icon: Clock },
  { key: "overdue", label: t("checklist.filters.overdue"), icon: AlertCircle },
  { key: "done", label: t("checklist.filters.done"), icon: CheckCircle2 },
]);

const activeTab = ref("all");
const collapsedGroups = reactive(new Set<string>());

// ----- Filtering -----
const filteredItems = computed(() => {
  const now = new Date();
  switch (activeTab.value) {
    case "pending":
      return checklistStore.items.filter(
        (i) => !i.done && (!i.dueDate || !isBefore(parseISO(i.dueDate), now)),
      );
    case "overdue":
      return checklistStore.items.filter(
        (i) => !i.done && i.dueDate && isBefore(parseISO(i.dueDate), now),
      );
    case "done":
      return checklistStore.items.filter((i) => i.done);
    default:
      return checklistStore.items;
  }
});

function tabCount(key: string) {
  const now = new Date();
  switch (key) {
    case "pending":
      return checklistStore.items.filter(
        (i) => !i.done && (!i.dueDate || !isBefore(parseISO(i.dueDate), now)),
      ).length;
    case "overdue":
      return checklistStore.items.filter(
        (i) => !i.done && i.dueDate && isBefore(parseISO(i.dueDate), now),
      ).length;
    case "done":
      return checklistStore.items.filter((i) => i.done).length;
    default:
      return checklistStore.items.length;
  }
}

// ----- Groups -----
const orderedPeriods = computed(() => {
  const periodsInData = new Set(
    filteredItems.value.map((i) => i.timePeriod || "General"),
  );
  const ordered = [...TIME_PERIODS, "General"].filter((p) =>
    periodsInData.has(p),
  );
  // any unknown periods appended
  periodsInData.forEach((p) => {
    if (!ordered.includes(p)) ordered.push(p);
  });
  return ordered;
});

function groupItems(period: string) {
  return filteredItems.value.filter(
    (i) => (i.timePeriod || "General") === period,
  );
}

function toggleGroup(period: string) {
  if (collapsedGroups.has(period)) collapsedGroups.delete(period);
  else collapsedGroups.add(period);
}

// ----- Progress -----
const globalPct = computed(() =>
  checklistStore.total === 0
    ? 0
    : Math.round((checklistStore.done / checklistStore.total) * 100),
);

// SVG ring — r=40, circumference = 2π×40 ≈ 251.33
const ringCircumference = 2 * Math.PI * 40;
const ringOffset = computed(
  () => ringCircumference * (1 - globalPct.value / 100),
);

function groupPct(period: string) {
  const items = checklistStore.items.filter(
    (i) => (i.timePeriod || "General") === period,
  );
  if (!items.length) return 0;
  return Math.round((items.filter((i) => i.done).length / items.length) * 100);
}

// ----- Due date helpers -----
function isOverdue(item: ChecklistItem) {
  return (
    !item.done && !!item.dueDate && isBefore(parseISO(item.dueDate), new Date())
  );
}

function isUrgent(item: ChecklistItem) {
  if (item.done || !item.dueDate) return false;
  const due = parseISO(item.dueDate);
  const now = new Date();
  return !isBefore(due, now) && isBefore(due, addDays(now, 7));
}

function dueDateClass(item: ChecklistItem) {
  if (item.done) return "due-done";
  if (isOverdue(item)) return "due-overdue";
  if (isUrgent(item)) return "due-urgent";
  return "due-normal";
}

function dueDateLabel(item: ChecklistItem) {
  const dateStr = format(parseISO(item.dueDate!), "MMM d, yyyy");
  if (item.done) return `✓ ${dateStr}`;
  if (isOverdue(item)) return `🔴 ${dateStr}`;
  if (isUrgent(item)) return `🟡 ${dateStr}`;
  return `📅 ${dateStr}`;
}

// ----- Delete -----
async function deleteTask(id: string) {
  const ok = await useConfirm().ask("Delete this task?", {
    title: "Delete Task",
    confirmText: "Delete",
  });
  if (ok) await checklistStore.removeItem(id);
}

// ----- Modal -----
interface ModalForm {
  label: string;
  timePeriod: string;
  dueDate: string;
  notes: string;
}
const modal = reactive({
  open: false,
  isEdit: false,
  editId: "",
  form: { label: "", timePeriod: "", dueDate: "", notes: "" } as ModalForm,
});

function openAdd() {
  modal.isEdit = false;
  modal.editId = "";
  modal.form = { label: "", timePeriod: "", dueDate: "", notes: "" };
  modal.open = true;
}

function openEdit(item: ChecklistItem) {
  modal.isEdit = true;
  modal.editId = item.id;
  modal.form = {
    label: item.label,
    timePeriod: item.timePeriod || "",
    dueDate: item.dueDate || "",
    notes: item.notes || "",
  };
  modal.open = true;
}

async function saveModal() {
  if (!modal.form.label.trim()) return;
  const data = {
    label: modal.form.label.trim(),
    timePeriod: modal.form.timePeriod || undefined,
    dueDate: modal.form.dueDate || undefined,
    notes: modal.form.notes || undefined,
  };
  if (modal.isEdit) {
    await checklistStore.updateItem(modal.editId, data);
  } else {
    await checklistStore.addItem(data);
  }
  closeModal();
}

function closeModal() {
  modal.open = false;
}

onMounted(() => {
  if (!checklistStore.items.length) checklistStore.fetchChecklist();
});
</script>

<style scoped>
/* ══════════════════════════════
   PAGE SHELL
══════════════════════════════ */
.checklist-page {
  color: var(--color-text);
  min-height: 100%;
}

/* ── Header ── */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 0 0 4px;
  letter-spacing: -0.5px;
}

.subtitle {
  color: var(--color-muted);
  font-size: 0.9rem;
  margin: 0;
}

/* ══════════════════════════════
   TWO-COLUMN LAYOUT
══════════════════════════════ */
.checklist-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 1.75rem;
  align-items: start;
}

/* ══════════════════════════════
   SIDEBAR
══════════════════════════════ */
.cl-sidebar {
  position: sticky;
  top: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

/* ── Progress Ring Card ── */
.ring-card {
  background: linear-gradient(
    135deg,
    var(--color-gold) 0%,
    var(--color-gold-dark) 100%
  );
  border-radius: 18px;
  padding: 1.5rem 1rem 1.25rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.75rem;
  position: relative;
}

.progress-ring {
  width: 120px;
  height: 120px;
  transform: rotate(-90deg);
}

.ring-bg {
  fill: none;
  stroke: rgba(255, 255, 255, 0.2);
  stroke-width: 8;
}

.ring-fill {
  fill: none;
  stroke: var(--color-white);
  stroke-width: 8;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.5s ease;
}

.ring-label {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -42%);
  text-align: center;
  color: var(--color-white);
  pointer-events: none;
}

.ring-pct {
  display: block;
  font-size: 1.5rem;
  font-weight: 800;
  letter-spacing: -1px;
  line-height: 1;
}

.ring-sub {
  display: block;
  font-size: 0.72rem;
  opacity: 0.85;
  font-weight: 500;
  margin-top: 0.15rem;
}

/* ── Stat Grid ── */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.6rem;
}

.stat-card {
  border-radius: 14px;
  padding: 0.85rem 0.75rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.2rem;
  border: 1.5px solid transparent;
  transition: box-shadow 0.15s;
}

.stat-card:hover {
  box-shadow: var(--shadow-sm);
}

.stat-value {
  font-size: 1.5rem;
  font-weight: 800;
  line-height: 1;
}

.stat-name {
  font-size: 0.72rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  opacity: 0.75;
}

.stat-done {
  background: var(--color-green-light);
  color: var(--color-green);
  border-color: var(--color-green);
}
.stat-pending {
  background: var(--color-info-light);
  color: var(--color-info);
  border-color: var(--color-info);
}

.stat-overdue {
  background: var(--color-surface);
  color: var(--color-muted);
  border-color: var(--color-border);
}
.stat-overdue.stat-alert {
  background: var(--color-error-light);
  color: var(--color-error);
  border-color: var(--color-error);
}

.stat-urgent {
  background: var(--color-surface);
  color: var(--color-muted);
  border-color: var(--color-border);
}
.stat-urgent.stat-alert-amber {
  background: var(--color-amber-light);
  color: var(--color-amber);
  border-color: var(--color-amber);
}

/* ── Sidebar Nav ── */
.sidebar-nav {
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 16px;
  padding: 0.85rem 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.nav-label {
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-muted);
  margin: 0 0 0.3rem 0.5rem;
}

.nav-btn {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.55rem 0.75rem;
  border-radius: 10px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 0.88rem;
  color: var(--color-text);
  font-weight: 500;
  transition: all 0.15s;
  text-align: left;
}

.nav-btn:hover {
  background: var(--color-surface-alt);
}

.nav-btn.active {
  background: var(--color-gold-light);
  color: var(--color-gold);
  font-weight: 700;
}

.nav-icon {
  font-size: 0.85rem;
  width: 18px;
  text-align: center;
}

.nav-text {
  flex: 1;
}

.nav-count {
  background: var(--color-surface-alt);
  border-radius: 999px;
  padding: 0.1rem 0.5rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.nav-btn.active .nav-count {
  background: var(--color-gold-light);
  color: var(--color-gold-dark);
}

/* ── Celebration ── */
.celebration-card {
  background: var(--color-green-light);
  border: 1.5px solid var(--color-green);
  border-radius: 14px;
  padding: 0.9rem 1rem;
  text-align: center;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--color-green);
}

/* ══════════════════════════════
   MAIN CONTENT
══════════════════════════════ */
.cl-main {
  min-width: 0;
}

/* ── Loading / Empty ── */
.loading-state,
.empty-state {
  text-align: center;
  padding: 4rem 2rem;
  color: var(--color-muted);
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 18px;
}

.spinner {
  width: 38px;
  height: 38px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-gold);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 0.8rem;
}

/* ── Groups ── */
.groups {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.group-card {
  background: var(--color-white);
  border: 1.5px solid var(--color-border);
  border-radius: 18px;
  overflow: hidden;
  transition: box-shadow 0.2s;
}

.group-card:hover {
  box-shadow: var(--shadow-md);
}

.group-header {
  padding: 1rem 1.4rem 0.85rem;
  cursor: pointer;
  user-select: none;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
}

.group-header:hover {
  background: var(--color-gold-light);
}

.group-header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.65rem;
}

.group-title-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.group-chevron {
  font-size: 1rem;
  color: var(--color-muted);
  transition: transform 0.2s;
  display: inline-block;
}

.group-chevron.collapsed {
  transform: rotate(-90deg);
}

.group-name {
  font-weight: 700;
  font-size: 0.97rem;
  color: var(--color-text);
}

.group-count {
  background: var(--color-gold-light);
  color: var(--color-gold);
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  border: 1px solid var(--color-gold);
}

.group-pct-label {
  font-size: 0.8rem;
  color: var(--color-muted);
  font-weight: 700;
}

.group-track {
  height: 5px;
  background: var(--color-border);
  border-radius: 999px;
  overflow: hidden;
}

.group-fill {
  background: linear-gradient(90deg, var(--color-gold), var(--color-gold-dark));
  height: 100%;
  border-radius: 999px;
  transition: width 0.35s ease;
}

/* ── Task Rows ── */
.group-items {
  padding: 0.4rem 0;
}

.empty-group {
  padding: 1.1rem 1.5rem;
  color: var(--color-muted);
  font-size: 0.85rem;
  font-style: italic;
}

.task-row {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  padding: 0.75rem 1.4rem;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.12s;
}

.task-row:last-child {
  border-bottom: none;
}
.task-row:hover {
  background: var(--color-surface);
}

.task-row.task-done .task-label {
  text-decoration: line-through;
  color: var(--color-muted);
}

.task-row.task-overdue {
  background: var(--color-error-light);
}
.task-row.task-overdue:hover {
  background: var(--color-error-light);
}
.task-row.task-urgent {
  background: var(--color-amber-light);
}
.task-row.task-urgent:hover {
  background: var(--color-amber-light);
}

/* ── Checkbox ── */
.check-btn {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  border: 2px solid var(--color-border-strong);
  background: var(--color-white);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
  font-size: 0.8rem;
  font-weight: 700;
  color: var(--color-white);
}

.task-row.task-done .check-btn {
  background: var(--color-green);
  border-color: var(--color-green);
}

.check-btn:hover {
  border-color: var(--color-gold);
  transform: scale(1.1);
}

/* ── Task body ── */
.task-body {
  flex: 1;
  min-width: 0;
}

.task-label {
  display: block;
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--color-text);
}

.task-note {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: var(--color-muted);
  font-style: italic;
  white-space: pre-line;
  line-height: 1.4;
}

.task-meta {
  display: flex;
  gap: 0.4rem;
  margin-top: 0.25rem;
  flex-wrap: wrap;
}

.due-chip {
  font-size: 0.74rem;
  padding: 0.15rem 0.55rem;
  border-radius: 999px;
  font-weight: 500;
}

.due-overdue {
  background: var(--color-error-light);
  color: var(--color-error);
}
.due-urgent {
  background: var(--color-amber-light);
  color: var(--color-amber);
}
.due-normal {
  background: var(--color-green-light);
  color: var(--color-green);
}
.due-done {
  background: var(--color-surface-alt);
  color: var(--color-muted);
}

/* ── Task Actions ── */
.task-actions {
  display: flex;
  gap: 0.3rem;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}

.task-row:hover .task-actions {
  opacity: 1;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
  padding: 0.25rem 0.3rem;
  border-radius: 8px;
  transition: background 0.12s;
}

.icon-btn:hover {
  background: var(--color-surface-alt);
}
.icon-btn.danger:hover {
  background: var(--color-error-light);
}

/* ══════════════════════════════
   BUTTONS
══════════════════════════════ */
.btn-primary {
  background: var(--color-gold);
  color: var(--color-white);
  border: none;
  padding: 0.65rem 1.4rem;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.2s,
    transform 0.1s;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-primary:hover:not(:disabled) {
  background: var(--color-gold-dark);
  transform: translateY(-1px);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-icon {
  font-weight: 400;
  font-size: 1.1rem;
  line-height: 1;
}

.btn-secondary {
  background: var(--color-surface-alt);
  color: var(--color-text);
  border: none;
  padding: 0.65rem 1.4rem;
  border-radius: 12px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: var(--color-border);
}

/* ══════════════════════════════
   MODAL
══════════════════════════════ */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal {
  background: var(--color-white);
  border-radius: 20px;
  width: 100%;
  max-width: 490px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid var(--color-border);
}

.modal-header h2 {
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  color: var(--color-muted);
  padding: 0.25rem 0.5rem;
  border-radius: 8px;
  transition: background 0.15s;
}

.close-btn:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.modal-body {
  padding: 1.4rem 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.form-field label {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.form-field input,
.form-field select,
.form-field textarea {
  border: 1.5px solid var(--color-border);
  border-radius: 10px;
  padding: 0.6rem 0.95rem;
  font-size: 0.9rem;
  outline: none;
  transition: border-color 0.18s;
  font-family: inherit;
  color: var(--color-text);
  background: var(--color-white);
}

.form-field input:focus,
.form-field select:focus,
.form-field textarea:focus {
  border-color: var(--color-gold);
}

.form-field textarea {
  resize: vertical;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
}

/* ══════════════════════════════
   RESPONSIVE
══════════════════════════════ */
@media (max-width: 900px) {
  .checklist-layout {
    grid-template-columns: 1fr;
  }

  .cl-sidebar {
    position: static;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0.75rem;
  }

  .ring-card {
    grid-column: span 2;
    flex-direction: row;
    justify-content: center;
    padding: 1.25rem 1.5rem;
    gap: 1.5rem;
  }

  .progress-ring {
    width: 90px;
    height: 90px;
  }
  .ring-label {
    position: static;
    transform: none;
    color: var(--color-white);
  }
}

@media (max-width: 600px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.8rem;
  }
  .cl-sidebar {
    grid-template-columns: 1fr;
  }
  .ring-card {
    grid-column: auto;
  }
  .stat-grid {
    grid-template-columns: 1fr 1fr;
  }
  .task-actions {
    opacity: 1;
  }
}
</style>
