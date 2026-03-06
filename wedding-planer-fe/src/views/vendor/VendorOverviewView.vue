<script setup lang="ts">
import { onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useVendorOverviewStore } from "@/stores/vendorOverview.store";
import StatCard from "@/components/vendor/StatCard.vue";
import MonthlyLeadsChart from "@/components/vendor/MonthlyLeadsChart.vue";
import RecentLeadsWidget from "@/components/vendor/RecentLeadsWidget.vue";
import UpcomingCallsWidget from "@/components/vendor/UpcomingCallsWidget.vue";
import ProfilePerformanceWidget from "@/components/vendor/ProfilePerformanceWidget.vue";

const store = useVendorOverviewStore();
const { data, loading } = storeToRefs(store);

onMounted(() => store.fetchOverview());

function formatCurrency(value?: number): string {
  if (value == null) return "—";
  return "€" + Math.round(value).toLocaleString("en");
}

function formatNumber(value?: number): string {
  if (value == null) return "—";
  return value.toLocaleString("en");
}
</script>

<template>
  <div class="overview">
    <!-- ── Stat Cards ── -->
    <div class="stat-cards-grid">
      <StatCard
        emoji="💰"
        :primary="formatCurrency(data?.revenue?.current)"
        label="Revenue booked"
        :trend="data?.revenue?.trend"
        trend-format="currency"
        trend-label="vs last month"
        :sub="
          data?.revenue ? `${data.revenue.bookingCount} confirmed bookings` : ''
        "
        :loading="loading"
      />
      <StatCard
        emoji="📩"
        :primary="data?.leads ? String(data.leads.total) : '—'"
        label="Total leads"
        :trend="data?.leads?.trend"
        trend-format="number"
        trend-label="vs last month"
        :sub="data?.leads ? `${data.leads.today} new today` : ''"
        :loading="loading"
      />
      <StatCard
        emoji="👁️"
        :primary="formatNumber(data?.profileViews?.total)"
        label="Profile views"
        :trend="data?.profileViews?.trendPercent"
        trend-format="percent"
        trend-label="vs last week"
        :sub="
          data?.profileViews ? `${data.profileViews.thisWeek} this week` : ''
        "
        :loading="loading"
      />
      <StatCard
        emoji="⭐"
        :primary="data?.avgRating ? String(data.avgRating.value) : '—'"
        label="Average rating"
        :sub="data?.avgRating ? `${data.avgRating.reviewCount} reviews` : ''"
        :loading="loading"
      />
      <StatCard
        emoji="⚡"
        :primary="data?.responseRate ? `${data.responseRate.percent}%` : '—'"
        label="Response rate"
        :trend="data?.responseRate?.trend"
        trend-format="percent-points"
        trend-label="vs last 30 days"
        :sub="
          data?.responseRate
            ? `${data.responseRate.repliedCount} of ${data.responseRate.totalEnquiries} within 24h`
            : ''
        "
        :loading="loading"
        :color-by-value="true"
        :value="data?.responseRate?.percent"
        :thresholds="{ green: 80, amber: 50 }"
      />
    </div>

    <!-- ── Monthly Leads Chart ── -->
    <MonthlyLeadsChart
      class="chart-section"
      :data="data?.monthlyLeads ?? []"
      :loading="loading"
    />

    <!-- ── Bottom Row: widgets ── -->
    <div class="bottom-grid">
      <RecentLeadsWidget :leads="data?.recentLeads ?? []" :loading="loading" />
      <UpcomingCallsWidget
        :calls="data?.upcomingCalls ?? []"
        :loading="loading"
      />
      <ProfilePerformanceWidget
        :data="data?.profilePerformance ?? null"
        :loading="loading"
      />
    </div>
  </div>
</template>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.stat-cards-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}
.chart-section {
  /* MonthlyLeadsChart fills its card container */
}
.bottom-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
}
</style>
