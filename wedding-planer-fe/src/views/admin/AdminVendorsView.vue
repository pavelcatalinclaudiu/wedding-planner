<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { useConfirm } from "@/composables/useConfirm";
import { Search, BadgeCheck, Power, PowerOff } from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();
const confirm = useConfirm();

const search = ref("");
const categoryFilter = ref("");
const page = ref(0);
const PAGE_SIZE = 20;

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
  await adminStore.suspendVendor(id);
}

async function handleActivate(id: string) {
  await adminStore.activateVendor(id);
}

async function handleToggleVerify(id: string) {
  await adminStore.toggleVerifyVendor(id);
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
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="adminStore.vendorsLoading" class="loading-state">
        {{ t("common.loading") }}
      </div>
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
                <th>{{ t("common.edit") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in adminStore.vendors" :key="v.id">
                <td>
                  <div class="vendor-name">{{ v.businessName }}</div>
                  <div class="vendor-email">{{ v.email }}</div>
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
                    @click="handleToggleVerify(v.id)"
                  >
                    <BadgeCheck :size="16" />
                  </button>
                </td>
                <td>{{ formatDate(v.createdAt) }}</td>
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
            <button :disabled="page === 0" @click="prevPage" class="page-btn">
              ←
            </button>
            <button
              :disabled="(page + 1) * PAGE_SIZE >= adminStore.vendorsTotal"
              @click="nextPage"
              class="page-btn"
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

.vendor-name {
  font-weight: 600;
}
.vendor-email {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 1px;
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
</style>
