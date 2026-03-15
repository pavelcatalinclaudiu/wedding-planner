<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import {
  Users,
  Store,
  Heart,
  TrendingUp,
  Star,
  Eye,
  CreditCard,
} from "lucide-vue-next";

const { t } = useI18n();
const adminStore = useAdminStore();

onMounted(() => {
  if (!adminStore.stats) adminStore.fetchStats();
});

const stats = computed(() => adminStore.stats);

const kpis = computed(() => {
  if (!stats.value) return [];
  return [
    {
      label: t("admin.overview.totalUsers"),
      value: stats.value.totalUsers,
      sub: `+${stats.value.newUsersThisWeek} ${t("admin.overview.thisWeek")}`,
      icon: Users,
      color: "#6c63ff",
      link: "/admin/users",
    },
    {
      label: t("admin.overview.totalVendors"),
      value: stats.value.totalVendors,
      sub: `${stats.value.activeVendors} ${t("admin.overview.active")}`,
      icon: Store,
      color: "#d4af37",
      link: "/admin/vendors",
    },
    {
      label: t("admin.overview.totalCouples"),
      value: stats.value.totalCouples,
      sub: null,
      icon: Heart,
      color: "#e53e7c",
      link: "/admin/users?role=COUPLE",
    },
    {
      label: t("admin.overview.totalLeads"),
      value: stats.value.totalLeads,
      sub: null,
      icon: TrendingUp,
      color: "#38a169",
      link: null,
    },
    {
      label: t("admin.overview.totalReviews"),
      value: stats.value.totalReviews,
      sub: `⭐ ${stats.value.avgRating.toFixed(2)} ${t("admin.overview.avgRating")}`,
      icon: Star,
      color: "#dd6b20",
      link: "/admin/reviews",
      badge:
        stats.value.pendingReviewsCount > 0
          ? stats.value.pendingReviewsCount
          : null,
    },
    {
      label: t("admin.overview.platformVisits"),
      value: stats.value.landingPageVisits,
      sub: `+${stats.value.landingPageVisitsThisWeek} ${t("admin.overview.thisWeek")} · ${stats.value.uniqueLandingVisitors} ${t("admin.overview.uniqueVisitors")}`,
      icon: Eye,
      color: "#0bc5ea",
      link: null,
    },
    {
      label: t("admin.overview.paidVendors"),
      value: stats.value.paidVendors,
      sub: `${t("admin.overview.ofTotal", { total: stats.value.totalVendors })}`,
      icon: CreditCard,
      color: "#805ad5",
      link: "/admin/vendors",
    },
    {
      label: t("admin.overview.paidCouples"),
      value: stats.value.paidCouples,
      sub: `${t("admin.overview.ofTotal", { total: stats.value.totalCouples })}`,
      icon: CreditCard,
      color: "#e53e7c",
      link: "/admin/users?role=COUPLE",
    },
  ];
});

// Bar chart: signups by day — max 30 entries
const chartData = computed(() => {
  const days = stats.value?.signupsByDay ?? [];
  const max = Math.max(...days.map((d) => d.count), 1);
  return days.map((d) => ({
    date: d.date.slice(5), // "MM-DD"
    count: d.count,
    pct: Math.round((d.count / max) * 100),
  }));
});

const categoryEntries = computed(() =>
  Object.entries(stats.value?.vendorsByCategory ?? {}).sort(
    (a, b) => b[1] - a[1],
  ),
);
</script>

<template>
  <div class="overview-page">
    <!-- Loading skeleton -->
    <template v-if="adminStore.statsLoading">
      <div class="kpi-grid">
        <div v-for="i in 8" :key="i" class="kpi-card skeleton-card">
          <div class="skeleton skeleton-icon"></div>
          <div class="kpi-body">
            <div class="skeleton skeleton-value"></div>
            <div class="skeleton skeleton-label"></div>
          </div>
        </div>
      </div>
      <div class="lower-grid">
        <div class="card skeleton-chart-wrap">
          <div class="skeleton" style="height: 140px; border-radius: 8px"></div>
        </div>
        <div class="card">
          <div class="skeleton" style="height: 140px; border-radius: 8px"></div>
        </div>
      </div>
    </template>

    <template v-else-if="stats">
      <!-- KPI Cards -->
      <div class="kpi-grid">
        <component
          v-for="kpi in kpis"
          :key="kpi.label"
          :is="kpi.link ? 'RouterLink' : 'div'"
          :to="kpi.link ?? undefined"
          class="kpi-card"
          :class="{ 'kpi-card--link': !!kpi.link }"
        >
          <div
            class="kpi-icon"
            :style="{ background: kpi.color + '1a', color: kpi.color }"
          >
            <component :is="kpi.icon" :size="22" />
          </div>
          <div class="kpi-body">
            <div class="kpi-value-row">
              <div class="kpi-value">{{ kpi.value.toLocaleString() }}</div>
              <span v-if="(kpi as any).badge" class="kpi-badge"
                >{{ (kpi as any).badge }}
                {{ t("admin.overview.pending") }}</span
              >
            </div>
            <div class="kpi-label">{{ kpi.label }}</div>
            <div v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</div>
          </div>
        </component>
      </div>

      <!-- Two-column lower section -->
      <div class="lower-grid">
        <!-- Signups chart -->
        <div class="card chart-card">
          <h3 class="card-title">{{ t("admin.overview.signupsChart") }}</h3>
          <div v-if="chartData.length === 0" class="empty-state">
            {{ t("common.noResults") }}
          </div>
          <div v-else class="bar-chart">
            <div
              v-for="bar in chartData"
              :key="bar.date"
              class="bar-col"
              :title="`${bar.date}: ${bar.count}`"
            >
              <div class="bar-fill" :style="{ height: bar.pct + '%' }"></div>
              <div class="bar-label">{{ bar.date }}</div>
            </div>
          </div>
        </div>

        <!-- Vendors by category -->
        <div class="card">
          <h3 class="card-title">
            {{ t("admin.overview.vendorsByCategory") }}
          </h3>
          <div v-if="categoryEntries.length === 0" class="empty-state">
            {{ t("common.noResults") }}
          </div>
          <div v-else class="category-list">
            <div
              v-for="[cat, count] in categoryEntries"
              :key="cat"
              class="category-row"
            >
              <span class="cat-label">{{ cat.replace(/_/g, " ") }}</span>
              <div class="cat-bar-wrap">
                <div
                  class="cat-bar"
                  :style="{
                    width:
                      Math.round((count / (stats?.totalVendors || 1)) * 100) +
                      '%',
                  }"
                ></div>
              </div>
              <span class="cat-count">{{ count }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Top vendors -->
      <div class="card top-vendors-card">
        <h3 class="card-title">{{ t("admin.overview.topVendors") }}</h3>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th>{{ t("common.name") }}</th>
                <th>{{ t("common.category") }}</th>
                <th>{{ t("common.city") }}</th>
                <th>{{ t("admin.vendors.avgRating") }}</th>
                <th>{{ t("admin.vendors.reviews") }}</th>
                <th>{{ t("admin.vendors.leads") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="v in stats.topVendors" :key="v.id">
                <td class="bold-cell">
                  <RouterLink
                    :to="`/vendors/${v.id}`"
                    target="_blank"
                    class="vendor-link"
                  >
                    {{ v.businessName }}
                  </RouterLink>
                </td>
                <td>{{ v.category.replace(/_/g, " ") }}</td>
                <td>{{ v.city }}</td>
                <td>⭐ {{ v.avgRating.toFixed(1) }}</td>
                <td>{{ v.reviewCount }}</td>
                <td>{{ v.leadCount }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.overview-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* KPI grid */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 16px;
}

.kpi-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 18px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  text-decoration: none;
  color: inherit;
  transition:
    box-shadow 0.15s,
    border-color 0.15s;
}

.kpi-card--link {
  cursor: pointer;
}

.kpi-card--link:hover {
  border-color: var(--color-gold);
  box-shadow: 0 2px 12px rgba(212, 175, 55, 0.12);
}

.kpi-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-value-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 3px;
}

.kpi-badge {
  font-size: 11px;
  font-weight: 700;
  background: rgba(229, 62, 62, 0.12);
  color: #e53e3e;
  border-radius: 20px;
  padding: 2px 8px;
  white-space: nowrap;
}

.kpi-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.kpi-sub {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 2px;
}

/* Lower grid */
.lower-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

@media (max-width: 900px) {
  .lower-grid {
    grid-template-columns: 1fr;
  }
}

/* Card */
.card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 20px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px;
}

/* Bar chart */
.chart-card {
  overflow: hidden;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 140px;
  overflow-x: auto;
}

.bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 20px;
  height: 100%;
  justify-content: flex-end;
}

.bar-fill {
  width: 100%;
  min-height: 2px;
  background: var(--color-gold);
  border-radius: 3px 3px 0 0;
  transition: height 0.3s;
}

.bar-label {
  font-size: 9px;
  color: var(--color-text-muted);
  writing-mode: vertical-lr;
  transform: rotate(180deg);
  white-space: nowrap;
}

/* Category list */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.cat-label {
  width: 130px;
  flex-shrink: 0;
  color: var(--color-text-muted);
  text-transform: capitalize;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cat-bar-wrap {
  flex: 1;
  height: 8px;
  background: var(--color-surface);
  border-radius: 4px;
  overflow: hidden;
}

.cat-bar {
  height: 100%;
  background: var(--color-gold);
  border-radius: 4px;
  min-width: 4px;
}

.cat-count {
  width: 28px;
  text-align: right;
  font-weight: 600;
  font-size: 13px;
}

/* Top vendors table */
.top-vendors-card {
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
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--color-border);
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
}

.data-table tbody tr:last-child td {
  border-bottom: none;
}

.data-table tbody tr:hover td {
  background: var(--color-surface);
}

.bold-cell {
  font-weight: 600;
}

.vendor-link {
  color: inherit;
  text-decoration: none;
  font-weight: 600;
}

.vendor-link:hover {
  color: var(--color-gold);
  text-decoration: underline;
}

/* Skeleton */
.skeleton-card {
  pointer-events: none;
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-border) 25%,
    var(--color-surface) 50%,
    var(--color-border) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 6px;
}

.skeleton-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  flex-shrink: 0;
}

.skeleton-value {
  height: 22px;
  width: 60px;
  margin-bottom: 8px;
}

.skeleton-label {
  height: 13px;
  width: 90px;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.skeleton-chart-wrap {
  overflow: hidden;
}

.empty-state,
.loading-state {
  color: var(--color-text-muted);
  font-size: 14px;
  padding: 24px 0;
  text-align: center;
}
</style>
