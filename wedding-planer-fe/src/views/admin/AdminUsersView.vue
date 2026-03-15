<script setup lang="ts">
import { ref, onMounted, watch, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { useConfirm } from "@/composables/useConfirm";
import { useToast } from "@/composables/useToast";
import { Trash2, Search, Zap } from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();
const confirm = useConfirm();
const toast = useToast();

const search = ref("");
const roleFilter = ref("");
const page = ref(0);
const PAGE_SIZE = 20;

function load() {
  adminStore.fetchUsers({
    search: search.value || undefined,
    role: roleFilter.value || undefined,
    page: page.value,
    size: PAGE_SIZE,
  });
}

onMounted(load);

let debounceTimer: ReturnType<typeof setTimeout>;
watch([search, roleFilter], () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 0;
    load();
  }, 300);
});

async function handleDelete(id: string, email: string) {
  const ok = await confirm.ask(t("admin.users.confirmDelete", { email }), {
    danger: true,
  });
  if (!ok) return;
  try {
    await adminStore.deleteUser(id);
    toast.success(t("admin.users.deleteSuccess", { email }));
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

const totalPages = computed(() =>
  Math.max(1, Math.ceil(adminStore.usersTotal / PAGE_SIZE)),
);

function prevPage() {
  if (page.value > 0) {
    page.value--;
    load();
  }
}
function nextPage() {
  if ((page.value + 1) * PAGE_SIZE < adminStore.usersTotal) {
    page.value++;
    load();
  }
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}

function roleBadgeClass(role: string) {
  if (role === "ADMIN") return "badge-admin";
  if (role === "VENDOR") return "badge-vendor";
  return "badge-couple";
}

async function handleSetCouplePlan(
  profileId: string,
  currentPlan: string,
  newPlan: string,
) {
  if (newPlan === currentPlan) return;
  const ok = await confirm.ask(
    t("admin.users.confirmPlanChange", { plan: newPlan }),
    { danger: newPlan === "FREE" },
  );
  if (!ok) return;
  try {
    await adminStore.setCouplePlan(profileId, newPlan);
    toast.success(t("admin.users.planChangeSuccess", { plan: newPlan }));
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

async function handleToggleCoupleMonetization(
  profileId: string,
  current: boolean,
) {
  try {
    await adminStore.toggleCoupleMonetization(profileId, !current);
    toast.success(
      !current
        ? t("admin.vendors.monetizationOnToast")
        : t("admin.vendors.monetizationOffToast"),
    );
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}

// Show bulk buttons only when showing couples (all roles or specifically COUPLE)
const showBulk = computed(
  () => roleFilter.value === "" || roleFilter.value === "COUPLE",
);

async function handleBulkCoupleMonetization(enabled: boolean) {
  const label = enabled
    ? t("admin.users.coupleMonetizationEnableAll")
    : t("admin.users.coupleMonetizationDisableAll");
  const ok = await confirm.ask(label + "?", { danger: !enabled });
  if (!ok) return;
  try {
    await adminStore.bulkToggleCoupleMonetization(enabled);
    toast.success(
      enabled
        ? t("admin.users.coupleMonetizationEnableAll")
        : t("admin.users.coupleMonetizationDisableAll"),
    );
  } catch {
    toast.error(t("common.errorGeneric"));
  }
}
</script>

<template>
  <div class="users-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-wrap">
        <Search :size="15" class="search-icon" />
        <input
          v-model="search"
          type="text"
          :placeholder="t('admin.users.searchPlaceholder')"
          class="search-input"
        />
      </div>
      <select v-model="roleFilter" class="filter-select">
        <option value="">{{ t("admin.users.allRoles") }}</option>
        <option value="COUPLE">COUPLE</option>
        <option value="VENDOR">VENDOR</option>
        <option value="ADMIN">ADMIN</option>
      </select>
      <div v-if="showBulk" class="bulk-monetization">
        <button
          class="bulk-btn bulk-on"
          @click="handleBulkCoupleMonetization(true)"
        >
          <Zap :size="13" /> {{ t("admin.users.coupleMonetizationEnableAll") }}
        </button>
        <button
          class="bulk-btn bulk-off"
          @click="handleBulkCoupleMonetization(false)"
        >
          <Zap :size="13" /> {{ t("admin.users.coupleMonetizationDisableAll") }}
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <!-- Skeleton rows -->
      <template v-if="adminStore.usersLoading">
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t("common.email") }}</th>
                <th>{{ t("admin.users.displayName") }}</th>
                <th>{{ t("common.status") }}</th>
                <th>{{ t("admin.users.verified") }}</th>
                <th>{{ t("admin.users.joined") }}</th>
                <th>{{ t("admin.users.lastLogin") }}</th>
                <th>{{ t("admin.vendors.plan") }}</th>
                <th>{{ t("admin.vendors.monetization") }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in 8" :key="i" class="skeleton-row">
                <td v-for="j in 9" :key="j">
                  <div class="skeleton skeleton-cell"></div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>

      <div v-else-if="adminStore.users.length === 0" class="empty-state">
        {{ t("common.noResults") }}
      </div>
      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t("common.email") }}</th>
                <th>{{ t("admin.users.displayName") }}</th>
                <th>{{ t("common.status") }}</th>
                <th>{{ t("admin.users.verified") }}</th>
                <th>{{ t("admin.users.joined") }}</th>
                <th>{{ t("admin.users.lastLogin") }}</th>
                <th>{{ t("admin.vendors.plan") }}</th>
                <th>{{ t("admin.vendors.monetization") }}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in adminStore.users" :key="user.id">
                <td class="email-cell">{{ user.email }}</td>
                <td>{{ user.displayName || "—" }}</td>
                <td>
                  <span class="role-badge" :class="roleBadgeClass(user.role)">
                    {{ user.role }}
                  </span>
                </td>
                <td>
                  <span :class="user.emailVerified ? 'verified' : 'unverified'">
                    {{ user.emailVerified ? "✓" : "✗" }}
                  </span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>{{ formatDate(user.lastLogin) }}</td>
                <td>
                  <select
                    v-if="user.role === 'COUPLE' && user.profileId"
                    :value="user.couplePlan || 'FREE'"
                    class="plan-select"
                    @change="
                      handleSetCouplePlan(
                        user.profileId!,
                        user.couplePlan || 'FREE',
                        ($event.target as HTMLSelectElement).value,
                      )
                    "
                  >
                    <option value="FREE">FREE</option>
                    <option value="DREAM_WEDDING">DREAM WEDDING</option>
                  </select>
                  <span v-else class="muted">—</span>
                </td>
                <td>
                  <button
                    v-if="user.role === 'COUPLE' && user.profileId"
                    class="action-btn monetization-btn"
                    :class="{
                      'monetization-on': user.coupleMonetizationEnabled,
                    }"
                    :title="
                      user.coupleMonetizationEnabled
                        ? t('admin.vendors.monetizationOn')
                        : t('admin.vendors.monetizationOff')
                    "
                    @click="
                      handleToggleCoupleMonetization(
                        user.profileId!,
                        user.coupleMonetizationEnabled,
                      )
                    "
                  >
                    <Zap :size="14" />
                  </button>
                  <span v-else class="muted">—</span>
                </td>
                <td class="actions-cell">
                  <button
                    class="btn-danger-sm"
                    :title="t('common.delete')"
                    @click="handleDelete(user.id, user.email)"
                  >
                    <Trash2 :size="14" />
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
                to: Math.min((page + 1) * PAGE_SIZE, adminStore.usersTotal),
                total: adminStore.usersTotal,
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
              :disabled="(page + 1) * PAGE_SIZE >= adminStore.usersTotal"
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
.users-page {
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
  padding: 8px 0 8px;
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
  padding: 11px 14px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.data-table tbody tr:hover td {
  background: var(--color-surface);
}

.email-cell {
  font-weight: 500;
}

.role-badge {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 20px;
  letter-spacing: 0.5px;
}

.badge-admin {
  background: rgba(108, 99, 255, 0.12);
  color: #6c63ff;
}
.badge-vendor {
  background: rgba(212, 175, 55, 0.12);
  color: var(--color-gold);
}
.badge-couple {
  background: rgba(229, 62, 124, 0.12);
  color: #e53e7c;
}

.verified {
  color: #38a169;
  font-weight: 700;
}
.unverified {
  color: #e53e3e;
  font-weight: 700;
}

.btn-danger-sm {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  color: var(--color-text-muted);
  display: flex;
  align-items: center;
  transition:
    color 0.15s,
    border-color 0.15s;
}

.btn-danger-sm:hover {
  color: #e53e3e;
  border-color: #e53e3e;
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

.muted {
  color: var(--color-text-muted);
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

.action-btn {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition:
    color 0.15s,
    border-color 0.15s;
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
