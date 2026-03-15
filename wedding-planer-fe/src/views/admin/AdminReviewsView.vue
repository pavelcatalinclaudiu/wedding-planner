<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { useConfirm } from "@/composables/useConfirm";
import { Trash2, Search, Check, X } from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();
const confirm = useConfirm();

const search = ref("");
const statusFilter = ref<"" | "PENDING" | "APPROVED" | "REJECTED">("");
const page = ref(0);
const PAGE_SIZE = 20;

function load() {
  adminStore.fetchReviews({
    search: search.value || undefined,
    status: statusFilter.value || undefined,
    page: page.value,
    size: PAGE_SIZE,
  });
}

onMounted(load);

let debounceTimer: ReturnType<typeof setTimeout>;
watch(search, () => {
  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    page.value = 0;
    load();
  }, 300);
});

function setStatus(s: typeof statusFilter.value) {
  statusFilter.value = s;
  page.value = 0;
  load();
}

async function handleApprove(id: string) {
  await adminStore.approveReview(id);
}

async function handleReject(id: string) {
  await adminStore.rejectReview(id);
}

async function handleDelete(id: string, vendorName: string) {
  const ok = await confirm.ask(
    t("admin.reviews.confirmDelete", { vendor: vendorName }),
    { danger: true },
  );
  if (!ok) return;
  await adminStore.deleteReview(id);
}

function prevPage() {
  if (page.value > 0) {
    page.value--;
    load();
  }
}

function nextPage() {
  if ((page.value + 1) * PAGE_SIZE < adminStore.reviewsTotal) {
    page.value++;
    load();
  }
}

function formatDate(iso: string | null) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString();
}
</script>

<template>
  <div class="reviews-page">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-wrap">
        <Search :size="15" class="search-icon" />
        <input
          v-model="search"
          type="text"
          :placeholder="t('admin.reviews.searchPlaceholder')"
          class="search-input"
        />
      </div>

      <!-- Status filter tabs -->
      <div class="status-tabs">
        <button
          v-for="tab in [
            { key: '', label: t('admin.reviews.statusAll') },
            { key: 'PENDING', label: t('admin.reviews.statusPending') },
            { key: 'APPROVED', label: t('admin.reviews.statusApproved') },
            { key: 'REJECTED', label: t('admin.reviews.statusRejected') },
          ]"
          :key="tab.key"
          class="status-tab"
          :class="{ active: statusFilter === tab.key }"
          @click="setStatus(tab.key as typeof statusFilter)"
        >
          {{ tab.label }}
        </button>
      </div>
    </div>

    <!-- Cards -->
    <div v-if="adminStore.reviewsLoading" class="loading-state">
      {{ t("common.loading") }}
    </div>

    <div v-else-if="adminStore.reviews.length === 0" class="empty-state">
      {{ t("common.noResults") }}
    </div>

    <template v-else>
      <div class="reviews-list">
        <div
          v-for="review in adminStore.reviews"
          :key="review.id"
          class="review-card"
          :class="`card-${review.status.toLowerCase()}`"
        >
          <div class="review-header">
            <div class="review-meta">
              <span class="stars">{{ review.rating.toFixed(1) }} ⭐</span>
              <span class="separator">·</span>
              <span class="vendor-name">{{ review.vendorName }}</span>
              <span class="separator">·</span>
              <span class="couple-name">{{ review.coupleName }}</span>
            </div>
            <div class="review-actions">
              <span class="date">{{ formatDate(review.createdAt) }}</span>
              <span class="status-badge" :class="review.status.toLowerCase()">
                {{
                  t(
                    `admin.reviews.status${review.status.charAt(0) + review.status.slice(1).toLowerCase()}`,
                  )
                }}
              </span>

              <!-- Approve / Reject — only shown when not already in that state -->
              <button
                v-if="review.status !== 'APPROVED'"
                class="btn-action approve"
                :title="t('admin.reviews.approve')"
                @click="handleApprove(review.id)"
              >
                <Check :size="14" />
              </button>
              <button
                v-if="review.status !== 'REJECTED'"
                class="btn-action reject"
                :title="t('admin.reviews.reject')"
                @click="handleReject(review.id)"
              >
                <X :size="14" />
              </button>

              <button
                class="btn-danger-sm"
                :title="t('common.delete')"
                @click="handleDelete(review.id, review.vendorName)"
              >
                <Trash2 :size="14" />
              </button>
            </div>
          </div>

          <p v-if="review.comment" class="review-comment">
            {{ review.comment }}
          </p>
          <p v-else class="review-comment muted">
            {{ t("admin.reviews.noComment") }}
          </p>

          <div v-if="review.vendorReply" class="vendor-reply">
            <span class="reply-label"
              >{{ t("admin.reviews.vendorReply") }}:</span
            >
            {{ review.vendorReply }}
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="pagination">
        <span class="page-info">
          {{
            t("admin.pagination.showing", {
              from: page * PAGE_SIZE + 1,
              to: Math.min((page + 1) * PAGE_SIZE, adminStore.reviewsTotal),
              total: adminStore.reviewsTotal,
            })
          }}
        </span>
        <div class="page-btns">
          <button :disabled="page === 0" @click="prevPage" class="page-btn">
            ←
          </button>
          <button
            :disabled="(page + 1) * PAGE_SIZE >= adminStore.reviewsTotal"
            @click="nextPage"
            class="page-btn"
          >
            →
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.reviews-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.search-wrap {
  position: relative;
  flex: 1;
  max-width: 400px;
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

/* Status filter tabs */
.status-tabs {
  display: flex;
  gap: 6px;
}

.status-tab {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.status-tab:hover {
  border-color: var(--color-gold);
  color: var(--color-text);
}

.status-tab.active {
  background: var(--color-gold);
  border-color: var(--color-gold);
  color: #fff;
  font-weight: 600;
}

.reviews-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px 18px;
  border-left: 4px solid var(--color-border);
}

.review-card.card-pending {
  border-left-color: #d69e2e;
}

.review-card.card-approved {
  border-left-color: #38a169;
}

.review-card.card-rejected {
  border-left-color: #e53e3e;
  opacity: 0.75;
}

.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.review-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.stars {
  font-weight: 600;
  color: #dd6b20;
}

.separator {
  color: var(--color-text-muted);
}

.vendor-name {
  font-weight: 600;
}

.couple-name {
  color: var(--color-text-muted);
}

.review-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.date {
  font-size: 12px;
  color: var(--color-text-muted);
}

/* Status badge */
.status-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 20px;
  text-transform: capitalize;
}

.status-badge.pending {
  background: rgba(214, 158, 46, 0.15);
  color: #d69e2e;
}

.status-badge.approved {
  background: rgba(56, 161, 105, 0.12);
  color: #38a169;
}

.status-badge.rejected {
  background: rgba(229, 62, 62, 0.12);
  color: #e53e3e;
}

/* Approve / Reject buttons */
.btn-action {
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 4px 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition:
    color 0.15s,
    border-color 0.15s,
    background 0.15s;
}

.btn-action.approve {
  color: #38a169;
  border-color: rgba(56, 161, 105, 0.4);
}

.btn-action.approve:hover {
  background: rgba(56, 161, 105, 0.12);
  border-color: #38a169;
}

.btn-action.reject {
  color: #e53e3e;
  border-color: rgba(229, 62, 62, 0.4);
}

.btn-action.reject:hover {
  background: rgba(229, 62, 62, 0.1);
  border-color: #e53e3e;
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

.review-comment {
  font-size: 14px;
  line-height: 1.5;
  margin: 0 0 8px;
}

.review-comment.muted {
  color: var(--color-text-muted);
  font-style: italic;
}

.vendor-reply {
  background: var(--color-surface);
  border-left: 3px solid var(--color-gold);
  padding: 8px 12px;
  border-radius: 0 6px 6px 0;
  font-size: 13px;
  color: var(--color-text-muted);
}

.reply-label {
  font-weight: 600;
  color: var(--color-gold);
  margin-right: 4px;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 8px;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-muted);
}

.page-btns {
  display: flex;
  gap: 8px;
}

.page-btn {
  padding: 6px 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: var(--color-card);
  color: var(--color-text);
  cursor: pointer;
  font-size: 14px;
  transition: border-color 0.15s;
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-btn:not(:disabled):hover {
  border-color: var(--color-gold);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--color-text-muted);
  font-size: 15px;
}
</style>
