<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { useConfirm } from "@/composables/useConfirm";
import { useToast } from "@/composables/useToast";
import {
  Search,
  BadgeCheck,
  Power,
  PowerOff,
  Zap,
  ExternalLink,
} from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();
const confirm = useConfirm();
const toast = useToast();

const search = ref("");
const categoryFilter = ref("");
const page = ref(0);
const PAGE_SIZE = 20;

const totalPages = computed(() =>
  Math.max(1, Math.ceil(adminStore.vendorsTotal / PAGE_SIZE)),
);

const CATEGORIES = [
  "PHOTOGRAPHER",
  "VIDEOGRAPHER",
  "FLORIST",
  "CATERING",
  "VENUE",
  "DJ",
  "BAND",
  "WEDDING_PLANNER",
  "HAIR_MAKEUP",
  "CAKE",
  "TRANSPORT",
  "OFFICIANT",
  "DECOR",
  "STATIONERY",
  "JEWELRY",
];

function load() {
  adminStore.fetchVendors({
    search: search.value || undefined,
    category: categoryFilter.value || undefined,
    page: page.value,
    size: PAGE_SIZE,
  });
}

onMounted(load);

let debounceTimer: ReturnType<typeof setTimeout>;
watch([search, categoryFilter], () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 0;
    load();
  }, 300);
});

async function handleSuspend(id: string, name: string) {
  const ok = await confirm.ask(t("admin.vendors.confirmSuspend", { name }), {
    danger: true,
  });
  if (!ok) return;
  try {
    await adminStore.suspendVendor(id);
    toast.success(t("admin.vendors.suspendSuccess", { name }));
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleActivate(id: string) {
  try {
    await adminStore.activateVendor(id);
    toast.success(t("admin.vendors.activateSuccess"));
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleToggleVerify(id: string, current: boolean) {
  try {
    await adminStore.toggleVerifyVendor(id);
    toast.success(
      current
        ? t("admin.vendors.unverifySuccess")
        : t("admin.vendors.verifySuccess"),
    );
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleSetPlan(id: string, currentPlan: string, newPlan: string) {
  if (newPlan === currentPlan) return;
  const ok = await confirm.ask(
    t("admin.vendors.confirmPlanChange", { plan: newPlan }),
    { danger: false },
  );
  if (!ok) return;
  try {
    await adminStore.setVendorPlan(id, newPlan);
    toast.success(t("admin.vendors.planChangeSuccess", { plan: newPlan }));
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleToggleMonetization(id: string, current: boolean) {
  try {
    await adminStore.toggleVendorMonetization(id, !current);
    toast.success(
      current
        ? t("admin.vendors.monetizationOffToast")
        : t("admin.vendors.monetizationOnToast"),
    );
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleBulkMonetization(enabled: boolean) {
  const label = enabled
    ? t("admin.vendors.monetizationEnableAll")
    : t("admin.vendors.monetizationDisableAll");
  let message = label + "?";
  if (search.value || categoryFilter.value) {
    message += " " + t("admin.vendors.bulkFilterWarning");
  }
  const ok = await confirm.ask(message, { danger: !enabled });
  if (!ok) return;
  try {
    await adminStore.bulkToggleVendorMonetization(enabled);
    toast.success(label);
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

function prevPage() {
  if (page.value > 0) {
    page.value--;
    load();
  }
}

function nextPage() {
  if ((page.value + 1) * PAGE_SIZE < adminStore.vendorsTotal) {
    page.value++;
    load();
  }
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

function formatPrice(price: number | null) {
  if (price == null) return "—";
  return "€" + price.toLocaleString();
}
</script>

<template>
  <div class="vendors-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-wrap">
        <Search :size="15" class="search-icon" />
        <input
          v-model="search"
          type="text"
          :placeholder="t('admin.vendors.searchPlaceholder')"
          class="search-input"
        />
      </div>
      <select v-model="categoryFilter" class="filter-select">
        <option value="">{{ t("admin.vendors.allCategories") }}</option>
        <option v-for="cat in CATEGORIES" :key="cat" :value="cat">
          {{ cat.replace(/_/g, " ") }}
        </option>
      </select>
      <div class="bulk-monetization">
        <button class="bulk-btn bulk-on" @click="handleBulkMonetization(true)">
          <Zap :size="13" /> {{ t("admin.vendors.monetizationEnableAll") }}
        </button>
        <button
          class="bulk-btn bulk-off"
          @click="handleBulkMonetization(false)"
        >
          <Zap :size="13" /> {{ t("admin.vendors.monetizationDisableAll") }}
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <!-- Skeleton rows -->
      <template v-if="adminStore.vendorsLoading">
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t("admin.vendors.business") }}</th>
                <th>{{ t("common.category") }}</th>
                <th>{{ t("common.city") }}</th>
                <th>{{ t("admin.vendors.basePrice") }}</th>
                <th>{{ t("admin.vendors.avgRating") }}</th>
                <th>{{ t("admin.vendors.leads") }}</th>
                <th>{{ t("admin.vendors.views") }}</th>
                <th>{{ t("common.status") }}</th>
                <th>{{ t("admin.vendors.verified") }}</th>
                <th>{{ t("admin.users.joined") }}</th>
                <th>{{ t("admin.vendors.plan") }}</th>
                <th>{{ t("admin.vendors.monetization") }}</th>
                <th>{{ t("common.edit") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in 8" :key="i" class="skeleton-row">
                <td v-for="j in 13" :key="j">
                  <div class="skeleton skeleton-cell"></div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <div v-else-if="adminStore.vendors.length === 0" class="empty-state">
        {{ t("common.noResults") }}
      </div>
      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t("admin.vendors.business") }}</th>
                <th>{{ t("common.category") }}</th>
                <th>{{ t("common.city") }}</th>
                <th>{{ t("admin.vendors.basePrice") }}</th>
                <th>{{ t("admin.vendors.avgRating") }}</th>
                <th>{{ t("admin.vendors.leads") }}</th>
                <th>{{ t("admin.vendors.views") }}</th>
                <th>{{ t("common.status") }}</th>
                <th>{{ t("admin.vendors.verified") }}</th>
                <th>{{ t("admin.users.joined") }}</th>
                <th>{{ t("admin.vendors.plan") }}</th>
                <th>{{ t("admin.vendors.monetization") }}</th>
                <th>{{ t("common.edit") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in adminStore.vendors" :key="v.id">
                <td>
                  <div class="vendor-name-row">
                    <div>
                      <div class="vendor-name">{{ v.businessName }}</div>
                      <div class="vendor-email">{{ v.email }}</div>
                    </div>
                    <a
                      :href="`/vendors/${v.id}`"
                      target="_blank"
                      rel="noopener"
                      class="ext-link"
                      :title="t('admin.vendors.viewProfile')"
                    >
                      <ExternalLink :size="13" />
                    </a>
                  </div>
                </td>
                <td>{{ v.category?.replace(/_/g, " ") || "—" }}</td>
                <td>{{ v.city }}</td>
                <td>{{ formatPrice(v.basePrice) }}</td>
                <td>
                  <span v-if="v.reviewCount > 0"
                    >⭐ {{ v.avgRating.toFixed(1) }}</span
                  >
                  <span v-else class="muted">—</span>
                </td>
                <td>{{ v.leadCount }}</td>
                <td>{{ v.profileViews }}</td>
                <td>
                  <span
                    class="status-badge"
                    :class="v.isActive ? 'active' : 'suspended'"
                  >
                    {{
                      v.isActive
                        ? t("admin.vendors.active")
                        : t("admin.vendors.suspended")
                    }}
                  </span>
                </td>
                <td>
                  <button
                    class="verify-btn"
                    :class="{ verified: v.isVerified }"
                    :title="
                      v.isVerified
                        ? t('admin.vendors.unverify')
                        : t('admin.vendors.verify')
                    "
                    @click="handleToggleVerify(v.id, v.isVerified)"
                  >
                    <BadgeCheck :size="16" />
                  </button>
                </td>
                <td>{{ formatDate(v.createdAt) }}</td>
                <td>
                  <select
                    :value="v.tier || 'FREE'"
                    class="plan-select"
                    @change="
                      handleSetPlan(
                        v.id,
                        v.tier || 'FREE',
                        ($event.target as HTMLSelectElement).value,
                      )
                    "
                  >
                    <option value="FREE">FREE</option>
                    <option value="STANDARD">STANDARD</option>
                    <option value="PREMIUM">PREMIUM</option>
                  </select>
                </td>
                <td>
                  <button
                    class="action-btn monetization-btn"
                    :class="{ 'monetization-on': v.monetizationEnabled }"
                    :title="
                      v.monetizationEnabled
                        ? t('admin.vendors.monetizationOn')
                        : t('admin.vendors.monetizationOff')
                    "
                    @click="
                      handleToggleMonetization(v.id, v.monetizationEnabled)
                    "
                  >
                    <Zap :size="14" />
                  </button>
                </td>
                <td>
                  <button
                    v-if="v.isActive"
                    class="action-btn suspend-btn"
                    :title="t('admin.vendors.suspend')"
                    @click="handleSuspend(v.id, v.businessName)"
                  >
                    <PowerOff :size="14" />
                  </button>
                  <button
                    v-else
                    class="action-btn activate-btn"
                    :title="t('admin.vendors.activate')"
                    @click="handleActivate(v.id)"
                  >
                    <Power :size="14" />
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination">
          <span class="page-info">
            {{
              t("admin.pagination.showing", {
                from: page * PAGE_SIZE + 1,
                to: Math.min((page + 1) * PAGE_SIZE, adminStore.vendorsTotal),
                total: adminStore.vendorsTotal,
              })
            }}
          </span>
          <div class="page-btns">
            <button :disabled="page === 0" class="page-btn" @click="prevPage">
              ←
            </button>
            <span class="page-num">{{
              t("admin.pagination.page", { page: page + 1, total: totalPages })
            }}</span>
            <button
              :disabled="(page + 1) * PAGE_SIZE >= adminStore.vendorsTotal"
              class="page-btn"
              @click="nextPage"
            >
              →
            </button>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.vendors-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--color-surface);
  padding: 8px 0;
}

.search-wrap {
  position: relative;
  flex: 1;
  min-width: 200px;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--color-text-muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  color: var(--color-text);
  font-size: 14px;
  box-sizing: border-box;
}

.search-input:focus {
  outline: none;
  border-color: var(--color-gold);
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-card);
  color: var(--color-text);
  font-size: 14px;
  cursor: pointer;
}

.card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.data-table th {
  text-align: left;
  padding: 10px 14px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.data-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  white-space: nowrap;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}
.data-table tbody tr:hover td {
  background: var(--color-surface);
}

.vendor-name-row {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}

.vendor-name {
  font-weight: 600;
}
.vendor-email {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 1px;
}

.ext-link {
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  margin-top: 2px;
  flex-shrink: 0;
  transition: color 0.15s;
}
.ext-link:hover {
  color: var(--color-gold);
}
.muted {
  color: var(--color-text-muted);
}

.status-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 20px;
  letter-spacing: 0.5px;
}

.status-badge.active {
  background: rgba(56, 161, 105, 0.12);
  color: #38a169;
}
.status-badge.suspended {
  background: rgba(229, 62, 62, 0.12);
  color: #e53e3e;
}

.verify-btn {
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  transition: color 0.15s;
}

.verify-btn.verified {
  color: #38a169;
}
.verify-btn:hover {
  color: #38a169;
}

.action-btn {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition:
    color 0.15s,
    border-color 0.15s;
}

.suspend-btn {
  color: var(--color-text-muted);
}
.suspend-btn:hover {
  color: #e53e3e;
  border-color: #e53e3e;
}

.activate-btn {
  color: var(--color-text-muted);
}
.activate-btn:hover {
  color: #38a169;
  border-color: #38a169;
}

.monetization-btn {
  color: var(--color-text-muted);
}
.monetization-btn:hover {
  color: #d97706;
  border-color: #d97706;
}
.monetization-btn.monetization-on {
  color: #d97706;
  border-color: #d97706;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
  font-size: 13px;
  color: var(--color-text-muted);
}

.page-btns {
  display: flex;
  gap: 6px;
  align-items: center;
}

.page-num {
  font-size: 13px;
  color: var(--color-text-muted);
  padding: 0 4px;
}

.page-btn {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text);
  transition: background 0.15s;
}

.page-btn:hover:not(:disabled) {
  background: var(--color-gold);
  color: #fff;
  border-color: var(--color-gold);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: default;
}

.loading-state,
.empty-state {
  padding: 32px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: 14px;
}

.plan-select {
  padding: 4px 8px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-card);
  color: var(--color-text);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}
.plan-select:focus {
  outline: none;
  border-color: var(--color-gold);
}

.bulk-monetization {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.bulk-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  border-radius: 8px;
  border: 1px solid;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  background: none;
}

.bulk-on {
  color: #d97706;
  border-color: #d97706;
}
.bulk-on:hover {
  background: rgba(217, 119, 6, 0.08);
}

.bulk-off {
  color: var(--color-text-muted);
  border-color: var(--color-border);
}
.bulk-off:hover {
  color: #e53e3e;
  border-color: #e53e3e;
}

/* Skeleton */
.skeleton-row td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--color-border);
}
.skeleton {
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--color-border) 25%,
    var(--color-surface) 50%,
    var(--color-border) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
.skeleton-cell {
  height: 14px;
  width: 80%;
}
@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
