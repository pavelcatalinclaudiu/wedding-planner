<script setup lang="ts">
import { computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useI18n } from "vue-i18n";
import { useVendorOverviewStore } from "@/stores/vendorOverview.store";
import { useVideoCallsStore } from "@/stores/videoCalls.store";
import StatCard from "@/components/vendor/StatCard.vue";
import MonthlyLeadsChart from "@/components/vendor/MonthlyLeadsChart.vue";
import RecentLeadsWidget from "@/components/vendor/RecentLeadsWidget.vue";
import UpcomingCallsWidget from "@/components/vendor/UpcomingCallsWidget.vue";
import ProfilePerformanceWidget from "@/components/vendor/ProfilePerformanceWidget.vue";
import { DollarSign, Inbox, Eye, Star, Zap } from "lucide-vue-next";

const { t, locale } = useI18n();

const store = useVendorOverviewStore();
const { data, loading } = storeToRefs(store);
const videoStore = useVideoCallsStore();

onMounted(() => store.fetchOverview());

async function joinCall(callId: string) {
  await videoStore.joinCall(callId);
}

function formatCurrency(value?: number): string {
  if (value == null) return "—";
  return (
    "€" +
    Math.round(value).toLocaleString(locale.value === "ro" ? "ro-RO" : "en")
  );
}

function formatNumber(value?: number): string {
  if (value == null) return "—";
  return value.toLocaleString(locale.value === "ro" ? "ro-RO" : "en");
}

const revenueSub = computed(() =>
  data.value?.revenue
    ? `${data.value.revenue.bookingCount} ${t("vendor.stats.confirmedBookings")}`
    : "",
);

const leadsSub = computed(() =>
  data.value?.leads
    ? `${data.value.leads.today} ${t("vendor.stats.newToday")}`
    : "",
);

const viewsSub = computed(() =>
  data.value?.profileViews
    ? `${data.value.profileViews.thisWeek} ${t("vendor.stats.thisWeek")}`
    : "",
);

const ratingSub = computed(() =>
  data.value?.avgRating
    ? `${data.value.avgRating.reviewCount} ${t("vendor.stats.reviews")}`
    : "",
);

const responseSub = computed(() =>
  data.value?.responseRate
    ? `${data.value.responseRate.repliedCount} ${t("common.of")} ${data.value.responseRate.totalEnquiries} ${t("vendor.stats.within24h")}`
    : "",
);
</script>

<template>
  <div class="overview">
    <div class="page-header">
      <h2>{{ t("vendor.overview.title") }}</h2>
      <p class="page-sub">{{ t("vendor.overview.subtitle") }}</p>
    </div>
    <!-- ── Stat Cards ── -->
    <div class="stat-cards-grid">
      <StatCard
        :icon="DollarSign"
        :primary="formatCurrency(data?.revenue?.current)"
        :label="t('vendor.stats.revenueBooked')"
        :trend="data?.revenue?.trend"
        trend-format="currency"
        :trend-label="t('vendor.stats.vsLastMonth')"
        :sub="revenueSub"
        :loading="loading"
      />
      <StatCard
        :icon="Inbox"
        :primary="data?.leads ? String(data.leads.total) : '—'"
        :label="t('vendor.stats.totalLeads')"
        :trend="data?.leads?.trend"
        trend-format="number"
        :trend-label="t('vendor.stats.vsLastMonth')"
        :sub="leadsSub"
        :loading="loading"
      />
      <StatCard
        :icon="Eye"
        :primary="formatNumber(data?.profileViews?.total)"
        :label="t('vendor.stats.profileViews')"
        :trend="data?.profileViews?.trendPercent"
        trend-format="percent"
        :trend-label="t('vendor.stats.vsLastWeek')"
        :sub="viewsSub"
        :loading="loading"
      />
      <StatCard
        :icon="Star"
        :primary="data?.avgRating ? String(data.avgRating.value) : '—'"
        :label="t('vendor.stats.avgRating')"
        :sub="ratingSub"
        :loading="loading"
      />
      <StatCard
        :icon="Zap"
        :primary="data?.responseRate ? `${data.responseRate.percent}%` : '—'"
        :label="t('vendor.stats.responseRate')"
        :trend="data?.responseRate?.trend"
        trend-format="percent-points"
        :trend-label="t('vendor.stats.vsLast30Days')"
        :sub="responseSub"
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
        @join="joinCall"
      />
      <ProfilePerformanceWidget
        :data="data?.profilePerformance ?? null"
        :loading="loading"
      />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 4px;
}
.page-header h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.page-sub {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}
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
