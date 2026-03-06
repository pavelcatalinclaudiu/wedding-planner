<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useBudgetStore } from "@/stores/budget.store";
import { useConfirm } from "@/composables/useConfirm";
import { useCoupleStore } from "@/stores/couple.store";
import type { BudgetItem } from "@/types/couple.types";

const budgetStore = useBudgetStore();
const coupleStore = useCoupleStore();

onMounted(async () => {
  await Promise.all([
    budgetStore.items.length === 0 && budgetStore.fetchBudget(),
    coupleStore.profile || coupleStore.fetchProfile(),
  ]);
});

// ── Total Budget Editing ─────────────────────────────────────────────────
const editingTotalBudget = ref(false);
const totalBudgetDraft = ref(0);
const savingTotalBudget = ref(false);

function startEditTotalBudget() {
  totalBudgetDraft.value = Number(coupleStore.profile?.totalBudget ?? 0);
  editingTotalBudget.value = true;
}

async function saveTotalBudget() {
  savingTotalBudget.value = true;
  try {
    await coupleStore.updateProfile({ totalBudget: totalBudgetDraft.value });
    editingTotalBudget.value = false;
  } finally {
    savingTotalBudget.value = false;
  }
}

// ── Stats ────────────────────────────────────────────────────────────────
const totalBudget = computed(() =>
  Number(coupleStore.profile?.totalBudget ?? 0),
);
const totalAllocated = computed(() => budgetStore.totalBudgeted);
const totalSpent = computed(() => budgetStore.totalActual);
const totalRemaining = computed(() => totalBudget.value - totalSpent.value);
const spendPercent = computed(() =>
  totalBudget.value > 0
    ? Math.min(100, Math.round((totalSpent.value / totalBudget.value) * 100))
    : 0,
);
const isOverBudget = computed(() => totalSpent.value > totalBudget.value);
const unallocatedAmount = computed(
  () => totalBudget.value - totalAllocated.value,
);

// ── Category Grouping ────────────────────────────────────────────────────
type CategoryGroup = {
  category: string;
  estimated: number;
  actual: number;
  paid: number;
  items: BudgetItem[];
};

const categoryGroups = computed<CategoryGroup[]>(() => {
  const map = new Map<string, CategoryGroup>();
  for (const item of budgetStore.items) {
    const cat = item.category?.trim() || "Uncategorised";
    if (!map.has(cat))
      map.set(cat, {
        category: cat,
        estimated: 0,
        actual: 0,
        paid: 0,
        items: [],
      });
    const g = map.get(cat)!;
    g.estimated += Number(item.estimatedCost ?? 0);
    g.actual += Number(item.actualCost ?? 0);
    if (item.isPaid) g.paid += Number(item.estimatedCost ?? 0);
    g.items.push(item);
  }
  return [...map.values()].sort((a, b) => b.estimated - a.estimated);
});

const expandedCategories = ref<Set<string>>(new Set());
function toggleCategory(cat: string) {
  if (expandedCategories.value.has(cat)) expandedCategories.value.delete(cat);
  else expandedCategories.value.add(cat);
}

// ── Smart Alerts ─────────────────────────────────────────────────────────
const alerts = computed(() => {
  const list: { type: "error" | "warn" | "info"; message: string }[] = [];
  if (isOverBudget.value) {
    list.push({
      type: "error",
      message: `You're ${fmt(totalSpent.value - totalBudget.value)} over budget.`,
    });
  }
  if (
    unallocatedAmount.value > totalBudget.value * 0.15 &&
    unallocatedAmount.value > 0
  ) {
    list.push({
      type: "warn",
      message: `${fmt(unallocatedAmount.value)} of your total budget hasn't been allocated to a category yet.`,
    });
  }
  categoryGroups.value.forEach((g) => {
    if (g.actual > g.estimated && g.estimated > 0) {
      list.push({
        type: "warn",
        message: `${g.category} is over its category budget by ${fmt(g.actual - g.estimated)}.`,
      });
    }
  });
  const unpaidCount = budgetStore.items.filter(
    (i) => !i.isPaid && Number(i.actualCost ?? 0) > 0,
  ).length;
  if (unpaidCount > 0) {
    list.push({
      type: "info",
      message: `${unpaidCount} item${unpaidCount > 1 ? "s have" : " has"} actual costs recorded but haven't been marked as paid.`,
    });
  }
  return list;
});

// ── Add / Edit Item ───────────────────────────────────────────────────────
const showAddModal = ref(false);
const editingItem = ref<BudgetItem | null>(null);
const saving = ref(false);
const addError = ref("");

const CATEGORY_SUGGESTIONS = [
  "Venue",
  "Catering",
  "Photography",
  "Videography",
  "Florals",
  "Music",
  "Cake",
  "Hair & Makeup",
  "Transport",
  "Attire",
  "Stationery",
  "Other",
];

const addForm = ref({
  name: "",
  category: "",
  vendorName: "",
  estimatedCost: 0 as number,
  actualCost: undefined as number | undefined,
  isPaid: false,
  notes: "",
});

function openAddModal(prefillCategory?: string) {
  editingItem.value = null;
  addForm.value = {
    name: "",
    category: prefillCategory ?? "",
    vendorName: "",
    estimatedCost: 0,
    actualCost: undefined,
    isPaid: false,
    notes: "",
  };
  addError.value = "";
  showAddModal.value = true;
}

function openEditModal(item: BudgetItem) {
  editingItem.value = item;
  addForm.value = {
    name: item.name ?? "",
    category: item.category ?? "",
    vendorName: item.vendorName ?? "",
    estimatedCost: Number(item.estimatedCost ?? 0),
    actualCost:
      item.actualCost !== undefined && item.actualCost !== null
        ? Number(item.actualCost)
        : undefined,
    isPaid: item.isPaid,
    notes: item.notes ?? "",
  };
  addError.value = "";
  showAddModal.value = true;
}

async function submitItem() {
  if (!addForm.value.name.trim()) {
    addError.value = "Name is required.";
    return;
  }
  if (!addForm.value.category.trim()) {
    addError.value = "Category is required.";
    return;
  }
  saving.value = true;
  addError.value = "";
  try {
    if (editingItem.value) {
      await budgetStore.updateItem(editingItem.value.id, { ...addForm.value });
    } else {
      await budgetStore.addItem({ ...addForm.value });
    }
    showAddModal.value = false;
  } catch {
    addError.value = "Failed to save. Please try again.";
  } finally {
    saving.value = false;
  }
}

async function togglePaid(item: BudgetItem) {
  await budgetStore.updateItem(item.id, { isPaid: !item.isPaid });
}

async function remove(id: string) {
  const ok = await useConfirm().ask("Delete this budget item?", {
    title: "Delete Item",
    confirmText: "Delete",
  });
  if (ok) await budgetStore.deleteItem(id);
}

// ── Helpers ───────────────────────────────────────────────────────────────
function fmt(n: number): string {
  if (n >= 1000) return `€${(n / 1000).toFixed(n % 1000 === 0 ? 0 : 1)}K`;
  return `€${Math.round(n).toLocaleString()}`;
}

function barWidth(val: number, total: number): string {
  if (!total) return "0%";
  return `${Math.min(100, Math.round((val / total) * 100))}%`;
}

function varianceClass(estimated: number, actual: number): string {
  if (!actual) return "";
  return actual > estimated ? "over" : "under";
}
</script>

<template>
  <div class="budget-view">
    <!-- ── Header ─────────────────────────────────────────────────── -->
    <div class="page-header">
      <h2>Budget Tracker</h2>
      <button class="btn-primary" @click="openAddModal()">+ Add Item</button>
    </div>

    <!-- ── Overview Cards ─────────────────────────────────────────── -->
    <div class="overview-grid">
      <!-- Total Budget — editable -->
      <div class="ov-card ov-total">
        <p class="ov-label">Total Budget</p>
        <div v-if="!editingTotalBudget" class="ov-value-row">
          <span class="ov-value">{{ fmt(totalBudget) }}</span>
          <button
            class="edit-icon-btn"
            title="Edit total budget"
            @click="startEditTotalBudget"
          >
            ✏️
          </button>
        </div>
        <div v-else class="ov-edit-row">
          <input
            v-model.number="totalBudgetDraft"
            type="number"
            class="ov-input"
            autofocus
            @keyup.enter="saveTotalBudget"
            @keyup.escape="editingTotalBudget = false"
          />
          <button
            class="btn-save-sm"
            :disabled="savingTotalBudget"
            @click="saveTotalBudget"
          >
            {{ savingTotalBudget ? "…" : "Save" }}
          </button>
          <button class="btn-cancel-sm" @click="editingTotalBudget = false">
            ✕
          </button>
        </div>
        <p class="ov-sub">your wedding budget cap</p>
      </div>

      <!-- Allocated -->
      <div class="ov-card">
        <p class="ov-label">Allocated</p>
        <span class="ov-value">{{ fmt(totalAllocated) }}</span>
        <p class="ov-sub">across {{ budgetStore.items.length }} items</p>
      </div>

      <!-- Spent -->
      <div class="ov-card" :class="{ 'ov-danger': isOverBudget }">
        <p class="ov-label">Spent</p>
        <span class="ov-value">{{ fmt(totalSpent) }}</span>
        <p class="ov-sub">{{ spendPercent }}% of total budget</p>
      </div>

      <!-- Remaining -->
      <div
        class="ov-card"
        :class="totalRemaining < 0 ? 'ov-danger' : 'ov-good'"
      >
        <p class="ov-label">Remaining</p>
        <span class="ov-value">{{ fmt(Math.abs(totalRemaining)) }}</span>
        <p class="ov-sub">
          {{ totalRemaining < 0 ? "over budget" : "left to spend" }}
        </p>
      </div>
    </div>

    <!-- ── Spend Progress Bar ──────────────────────────────────────── -->
    <div class="global-progress">
      <div class="progress-track">
        <div
          class="progress-fill"
          :class="{ 'fill-danger': isOverBudget }"
          :style="{ width: `${spendPercent}%` }"
        ></div>
      </div>
      <span class="progress-label">{{ spendPercent }}% spent</span>
    </div>

    <!-- ── Smart Alerts ────────────────────────────────────────────── -->
    <div v-if="alerts.length > 0" class="alerts-list">
      <div
        v-for="(alert, i) in alerts"
        :key="i"
        class="alert-row"
        :class="`alert-${alert.type}`"
      >
        <span class="alert-icon">
          {{
            alert.type === "error" ? "🔴" : alert.type === "warn" ? "🟡" : "🔵"
          }}
        </span>
        <span>{{ alert.message }}</span>
      </div>
    </div>

    <!-- ── Category Breakdown ─────────────────────────────────────── -->
    <section class="section">
      <h3 class="section-title">Category Breakdown</h3>

      <div v-if="categoryGroups.length === 0" class="empty-state">
        No budget items yet. Click <strong>+ Add Item</strong> to get started.
      </div>

      <div v-else class="category-list">
        <div
          v-for="group in categoryGroups"
          :key="group.category"
          class="cat-block"
        >
          <!-- Category Header Row -->
          <div class="cat-header" @click="toggleCategory(group.category)">
            <div class="cat-header-left">
              <span class="cat-chevron">
                {{ expandedCategories.has(group.category) ? "▾" : "▸" }}
              </span>
              <span class="cat-name">{{ group.category }}</span>
              <span class="cat-count">
                {{ group.items.length }} item{{
                  group.items.length !== 1 ? "s" : ""
                }}
              </span>
            </div>
            <div class="cat-header-right">
              <div class="cat-bar-wrap">
                <div class="cat-bar-track">
                  <div
                    class="cat-bar-estimated"
                    :style="{ width: barWidth(group.estimated, totalBudget) }"
                  ></div>
                  <div
                    class="cat-bar-actual"
                    :style="{ width: barWidth(group.actual, totalBudget) }"
                  ></div>
                </div>
              </div>
              <div class="cat-amounts">
                <span class="cat-estimated">{{ fmt(group.estimated) }}</span>
                <span
                  v-if="group.actual > 0"
                  class="cat-actual"
                  :class="varianceClass(group.estimated, group.actual)"
                >
                  {{ fmt(group.actual) }} actual
                </span>
              </div>
              <span
                v-if="group.actual > 0"
                class="variance-chip"
                :class="varianceClass(group.estimated, group.actual)"
              >
                {{ group.actual > group.estimated ? "+" : ""
                }}{{ fmt(group.actual - group.estimated) }}
              </span>
              <button
                class="add-in-cat-btn"
                title="Add item to this category"
                @click.stop="openAddModal(group.category)"
              >
                +
              </button>
            </div>
          </div>

          <!-- Expanded Items -->
          <div v-if="expandedCategories.has(group.category)" class="cat-items">
            <div v-for="item in group.items" :key="item.id" class="item-row">
              <div class="item-left">
                <button
                  class="paid-toggle"
                  :class="{ 'is-paid': item.isPaid }"
                  :title="item.isPaid ? 'Mark as unpaid' : 'Mark as paid'"
                  @click="togglePaid(item)"
                >
                  {{ item.isPaid ? "✓" : "○" }}
                </button>
                <div class="item-info">
                  <span
                    class="item-name"
                    :class="{ 'item-paid-name': item.isPaid }"
                    >{{ item.name || "—" }}</span
                  >
                  <span v-if="item.vendorName" class="item-vendor">{{
                    item.vendorName
                  }}</span>
                </div>
              </div>
              <div class="item-right">
                <div class="item-costs">
                  <span class="item-estimated">{{
                    fmt(Number(item.estimatedCost ?? 0))
                  }}</span>
                  <span
                    v-if="
                      item.actualCost !== undefined && item.actualCost !== null
                    "
                    class="item-actual"
                    :class="
                      varianceClass(
                        Number(item.estimatedCost),
                        Number(item.actualCost),
                      )
                    "
                    >{{ fmt(Number(item.actualCost)) }} actual</span
                  >
                </div>
                <span :class="item.isPaid ? 'badge-paid' : 'badge-unpaid'">
                  {{ item.isPaid ? "Paid" : "Unpaid" }}
                </span>
                <button
                  class="icon-btn"
                  title="Edit"
                  @click="openEditModal(item)"
                >
                  ✏️
                </button>
                <button
                  class="icon-btn danger"
                  title="Delete"
                  @click="remove(item.id)"
                >
                  🗑
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ── Spending Breakdown Chart ───────────────────────────────── -->
    <section
      v-if="categoryGroups.length > 0 && (totalSpent > 0 || totalAllocated > 0)"
      class="section"
    >
      <h3 class="section-title">Spending Breakdown</h3>
      <div class="breakdown-chart">
        <div
          v-for="group in categoryGroups"
          :key="group.category"
          class="breakdown-row"
        >
          <span class="breakdown-label">{{ group.category }}</span>
          <div class="breakdown-track">
            <div
              class="breakdown-bar"
              :style="{
                width: barWidth(
                  group.actual || group.estimated,
                  totalSpent || totalAllocated,
                ),
              }"
            ></div>
          </div>
          <span class="breakdown-amount">{{
            fmt(group.actual || group.estimated)
          }}</span>
          <span class="breakdown-pct">
            {{
              totalBudget > 0
                ? Math.round(
                    ((group.actual || group.estimated) / totalBudget) * 100,
                  )
                : 0
            }}%
          </span>
        </div>
      </div>
    </section>

    <!-- ── Add / Edit Modal ───────────────────────────────────────── -->
    <Teleport to="body">
      <div
        v-if="showAddModal"
        class="modal-overlay"
        @click.self="showAddModal = false"
      >
        <div class="modal">
          <div class="modal-header">
            <h3>{{ editingItem ? "Edit Budget Item" : "Add Budget Item" }}</h3>
            <button class="modal-close" @click="showAddModal = false">✕</button>
          </div>

          <div class="modal-body">
            <div class="field-row">
              <label>Item name *</label>
              <input
                v-model="addForm.name"
                class="fi"
                placeholder="e.g. Venue deposit"
              />
            </div>

            <div class="field-row">
              <label>Category *</label>
              <input
                v-model="addForm.category"
                class="fi"
                placeholder="e.g. Venue"
                list="cat-suggestions"
              />
              <datalist id="cat-suggestions">
                <option v-for="c in CATEGORY_SUGGESTIONS" :key="c" :value="c" />
              </datalist>
            </div>

            <div class="field-row">
              <label>Vendor name</label>
              <input
                v-model="addForm.vendorName"
                class="fi"
                placeholder="e.g. Château Elara"
              />
            </div>

            <div class="field-row-2">
              <div class="field-row">
                <label>Budgeted (€) *</label>
                <input
                  v-model.number="addForm.estimatedCost"
                  type="number"
                  min="0"
                  class="fi"
                  placeholder="0"
                />
              </div>
              <div class="field-row">
                <label>Actual cost (€)</label>
                <input
                  v-model.number="addForm.actualCost"
                  type="number"
                  min="0"
                  class="fi"
                  placeholder="Leave blank if unknown"
                />
              </div>
            </div>

            <div class="field-row">
              <label>Notes</label>
              <input
                v-model="addForm.notes"
                class="fi"
                placeholder="Optional notes…"
              />
            </div>

            <label class="check-row">
              <input type="checkbox" v-model="addForm.isPaid" />
              Mark as paid
            </label>

            <p v-if="addError" class="form-error">{{ addError }}</p>
          </div>

          <div class="modal-footer">
            <button class="btn-ghost" @click="showAddModal = false">
              Cancel
            </button>
            <button class="btn-primary" :disabled="saving" @click="submitItem">
              {{
                saving ? "Saving…" : editingItem ? "Save Changes" : "Add Item"
              }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ── Layout ──────────────────────────────────────────────────────────────── */
.budget-view {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

/* ── Header ──────────────────────────────────────────────────────────────── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-header h2 {
  margin: 0;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--color-text);
}

/* ── Overview Grid ───────────────────────────────────────────────────────── */
.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}
@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.ov-card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 18px 20px;
}
.ov-card.ov-total {
  border-color: var(--color-gold);
  background: var(--color-gold-light);
}
.ov-card.ov-danger {
  border-color: var(--color-error);
  background: var(--color-error-light);
}
.ov-card.ov-good {
  border-color: var(--color-green);
  background: var(--color-green-light);
}

.ov-label {
  font-size: 0.75rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-muted);
  font-weight: 700;
  margin: 0 0 6px;
}
.ov-value-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.ov-value {
  font-size: 1.7rem;
  font-weight: 800;
  color: var(--color-text);
  line-height: 1;
}
.ov-edit-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}
.ov-input {
  width: 100px;
  padding: 5px 8px;
  border: 1.5px solid var(--color-gold);
  border-radius: 7px;
  font-size: 1rem;
  font-weight: 700;
  outline: none;
}
.ov-sub {
  font-size: 0.78rem;
  color: var(--color-muted);
  margin: 4px 0 0;
}
.edit-icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 2px;
  opacity: 0.45;
  transition: opacity 0.15s;
}
.edit-icon-btn:hover {
  opacity: 1;
}

/* ── Global Progress ─────────────────────────────────────────────────────── */
.global-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}
.progress-track {
  flex: 1;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--color-gold);
  border-radius: 4px;
  transition: width 0.4s ease;
}
.progress-fill.fill-danger {
  background: var(--color-error);
}
.progress-label {
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--color-muted);
  white-space: nowrap;
}

/* ── Alerts ──────────────────────────────────────────────────────────────── */
.alerts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.alert-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 0.875rem;
  font-weight: 500;
}
.alert-error {
  background: var(--color-error-light);
  border: 1px solid var(--color-error);
  color: var(--color-error);
}
.alert-warn {
  background: var(--color-amber-light);
  border: 1px solid var(--color-amber);
  color: var(--color-amber);
}
.alert-info {
  background: var(--color-info-light);
  border: 1px solid var(--color-info);
  color: var(--color-info);
}
.alert-icon {
  font-size: 0.9rem;
  flex-shrink: 0;
}

/* ── Section ─────────────────────────────────────────────────────────────── */
.section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.section-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: var(--color-text);
}
.empty-state {
  background: var(--color-white);
  border: 1px dashed var(--color-border);
  border-radius: 12px;
  padding: 28px;
  text-align: center;
  color: var(--color-muted);
  font-size: 0.9rem;
}

/* ── Category Block ──────────────────────────────────────────────────────── */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.cat-block {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
}

.cat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  cursor: pointer;
  user-select: none;
  gap: 16px;
  transition: background 0.12s;
}
.cat-header:hover {
  background: var(--color-surface);
}

.cat-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.cat-chevron {
  font-size: 0.8rem;
  color: var(--color-muted);
  width: 14px;
}
.cat-name {
  font-weight: 700;
  font-size: 0.92rem;
  color: var(--color-text);
}
.cat-count {
  font-size: 0.75rem;
  color: var(--color-muted);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 1px 7px;
}

.cat-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  justify-content: flex-end;
}
.cat-bar-wrap {
  flex: 1;
  max-width: 160px;
}
.cat-bar-track {
  height: 6px;
  background: var(--color-border);
  border-radius: 4px;
  position: relative;
  overflow: hidden;
}
.cat-bar-estimated {
  position: absolute;
  height: 100%;
  background: rgba(212, 168, 67, 0.3);
  border-radius: 4px;
}
.cat-bar-actual {
  position: absolute;
  height: 100%;
  background: var(--color-gold);
  border-radius: 4px;
}

.cat-amounts {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}
.cat-estimated {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
}
.cat-actual {
  font-size: 0.73rem;
  color: var(--color-muted);
}
.cat-actual.over {
  color: var(--color-error);
}
.cat-actual.under {
  color: var(--color-green);
}

.variance-chip {
  font-size: 0.73rem;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}
.variance-chip.over {
  background: var(--color-error-light);
  color: var(--color-error);
}
.variance-chip.under {
  background: var(--color-green-light);
  color: var(--color-green);
}

.add-in-cat-btn {
  background: none;
  border: 1.5px dashed var(--color-border);
  border-radius: 6px;
  width: 26px;
  height: 26px;
  font-size: 1rem;
  cursor: pointer;
  color: var(--color-muted);
  line-height: 1;
  flex-shrink: 0;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.add-in-cat-btn:hover {
  border-color: var(--color-gold);
  color: var(--color-gold);
}

/* ── Item Rows ───────────────────────────────────────────────────────────── */
.cat-items {
  border-top: 1px solid var(--color-border);
}
.item-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
  gap: 12px;
}
.item-row:last-child {
  border-bottom: none;
}

.item-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.paid-toggle {
  font-size: 0.85rem;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
  background: none;
  cursor: pointer;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    border-color 0.15s,
    background 0.15s,
    color 0.15s;
  color: var(--color-muted);
}
.paid-toggle.is-paid {
  border-color: var(--color-green);
  background: var(--color-green-light);
  color: var(--color-green);
}
.item-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}
.item-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.item-paid-name {
  color: var(--color-muted);
  text-decoration: line-through;
}
.item-vendor {
  font-size: 0.75rem;
  color: var(--color-muted);
}

.item-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.item-costs {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 1px;
}
.item-estimated {
  font-size: 0.88rem;
  font-weight: 700;
  color: var(--color-text);
}
.item-actual {
  font-size: 0.73rem;
  color: var(--color-muted);
}
.item-actual.over {
  color: var(--color-error);
}
.item-actual.under {
  color: var(--color-green);
}

.badge-paid {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 9px;
  border-radius: 10px;
  background: var(--color-green-light);
  color: var(--color-green);
}
.badge-unpaid {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 2px 9px;
  border-radius: 10px;
  background: var(--color-gold-light);
  color: var(--color-gold-dark);
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 3px;
  opacity: 0.45;
  transition: opacity 0.15s;
  line-height: 1;
}
.icon-btn:hover {
  opacity: 1;
}

/* ── Breakdown Chart ─────────────────────────────────────────────────────── */
.breakdown-chart {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.breakdown-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 0.85rem;
}
.breakdown-label {
  width: 110px;
  font-weight: 500;
  color: var(--color-text);
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.breakdown-track {
  flex: 1;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}
.breakdown-bar {
  height: 100%;
  background: linear-gradient(
    90deg,
    var(--color-gold) 0%,
    var(--color-gold-dark) 100%
  );
  border-radius: 4px;
  transition: width 0.4s ease;
}
.breakdown-amount {
  width: 52px;
  text-align: right;
  font-weight: 700;
  color: var(--color-text);
  flex-shrink: 0;
}
.breakdown-pct {
  width: 36px;
  text-align: right;
  color: var(--color-muted);
  font-size: 0.78rem;
  flex-shrink: 0;
}

/* ── Modal ───────────────────────────────────────────────────────────────── */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 16px;
}
.modal {
  background: var(--color-white);
  border-radius: 16px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
  border-bottom: 1px solid var(--color-border);
}
.modal-header h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
}
.modal-close {
  background: none;
  border: none;
  font-size: 1rem;
  cursor: pointer;
  color: var(--color-muted);
}
.modal-body {
  padding: 20px 22px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.field-row {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.field-row label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}
.field-row-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.fi {
  padding: 9px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.92rem;
  outline: none;
  transition: border-color 0.15s;
  background: var(--color-surface);
}
.fi:focus {
  border-color: var(--color-gold);
  background: var(--color-white);
}
.check-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  font-weight: 500;
}
.form-error {
  color: var(--color-error, #e74c3c);
  font-size: 0.83rem;
  margin: 0;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px;
  border-top: 1px solid var(--color-border);
}

/* ── Buttons ─────────────────────────────────────────────────────────────── */
.btn-primary {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 9px 20px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: filter 0.15s;
}
.btn-primary:hover {
  filter: brightness(1.08);
}
.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-ghost {
  background: none;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  padding: 9px 18px;
  font-size: 0.9rem;
  cursor: pointer;
  color: var(--color-text);
}
.btn-save-sm {
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 5px 10px;
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
}
.btn-save-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-cancel-sm {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 5px 8px;
  font-size: 0.82rem;
  cursor: pointer;
  color: var(--color-muted);
}
</style>
