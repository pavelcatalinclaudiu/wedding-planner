<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useI18n } from "vue-i18n";
import { useAdminStore } from "@/stores/admin.store";
import { Users, Store, Heart, TrendingUp, Star, Eye } from "lucide-vue-next";

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
    },
    {
      label: t("admin.overview.totalVendors"),
      value: stats.value.totalVendors,
      sub: `${stats.value.activeVendors} ${t("admin.overview.active")}`,
      icon: Store,
      color: "#d4af37",
    },
    {
      label: t("admin.overview.totalCouples"),
      value: stats.value.totalCouples,
      sub: null,
      icon: Heart,
      color: "#e53e7c",
    },
    {
      label: t("admin.overview.totalLeads"),
      value: stats.value.totalLeads,
      sub: null,
      icon: TrendingUp,
      color: "#38a169",
    },
    {
      label: t("admin.overview.totalReviews"),
      value: stats.value.totalReviews,
      sub: `⭐ ${stats.value.avgRating.toFixed(2)} ${t("admin.overview.avgRating")}`,
      icon: Star,
      color: "#dd6b20",
    },
    {
      label: t("admin.overview.platformVisits"),
      value: stats.value.totalProfileViews,
      sub: `+${stats.value.profileViewsThisWeek} ${t("admin.overview.thisWeek")} · ${stats.value.uniqueVisitors} ${t("admin.overview.uniqueVisitors")}`,
      icon: Eye,
      color: "#0bc5ea",
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
    <div v-if="adminStore.statsLoading" class="loading-state">
      {{ t("common.loading") }}
    </div>

    <template v-else-if="stats">
      <!-- KPI Cards -->
      <div class="kpi-grid">
        <div v-for="kpi in kpis" :key="kpi.label" class="kpi-card">
          <div
            class="kpi-icon"
            :style="{ background: kpi.color + '1a', color: kpi.color }"
          >
            <component :is="kpi.icon" :size="22" />
          </div>
          <div class="kpi-body">
            <div class="kpi-value">{{ kpi.value.toLocaleString() }}</div>
            <div class="kpi-label">{{ kpi.label }}</div>
            <div v-if="kpi.sub" class="kpi-sub">{{ kpi.sub }}</div>
          </div>
        </div>
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
                <td class="bold-cell">{{ v.businessName }}</td>
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

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 3px;
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

.empty-state,
.loading-state {
  color: var(--color-text-muted);
  font-size: 14px;
  padding: 24px 0;
  text-align: center;
}
</style>
