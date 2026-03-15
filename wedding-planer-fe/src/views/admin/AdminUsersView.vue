<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { useConfirm } from "@/composables/useConfirm";
import { Trash2, Search } from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();
const confirm = useConfirm();

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
  await adminStore.deleteUser(id);
}

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
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="adminStore.usersLoading" class="loading-state">
        {{ t("common.loading") }}
      </div>
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
            <button :disabled="page === 0" @click="prevPage" class="page-btn">
              ←
            </button>
            <button
              :disabled="(page + 1) * PAGE_SIZE >= adminStore.usersTotal"
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
