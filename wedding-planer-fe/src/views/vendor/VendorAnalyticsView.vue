<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { vendorApi } from "@/api/vendor.api";
import type { VendorDeepAnalytics } from "@/types/vendor.types";

const data = ref<VendorDeepAnalytics | null>(null);
const loading = ref(true);
const error = ref("");

onMounted(async () => {
  try {
    data.value = (await vendorApi.getDeepAnalytics()).data;
  } catch {
    error.value = "Could not load analytics.";
  } finally {
    loading.value = false;
  }
});

// ── Revenue chart helpers ─────────────────────────────────────────────────────
const maxRevenue = computed(() =>
  Math.max(1, ...(data.value?.revenueByMonth.map((m) => m.revenue) ?? [1])),
);
function revenueBarPct(val: number) {
  return Math.max(2, (val / maxRevenue.value) * 100);
}
function formatRevenue(val: number) {
  if (val >= 1000) return (val / 1000).toFixed(1) + "k";
  return val.toLocaleString();
}

// ── Lead funnel helpers ───────────────────────────────────────────────────────
const funnelTotal = computed(() => {
  const f = data.value?.leadFunnel;
  if (!f) return 1;
  return Math.max(
    1,
    f.newLeads + f.inDiscussion + f.quoted + f.booked + f.declined,
  );
});
function funnelPct(val: number) {
  return Math.round((val / funnelTotal.value) * 100);
}
const funnelStages = computed(() => {
  const f = data.value?.leadFunnel;
  if (!f) return [];
  return [
    { label: "New / Viewed", value: f.newLeads, color: "#6c8ebf" },
    { label: "In Discussion", value: f.inDiscussion, color: "#82b366" },
    { label: "Quoted", value: f.quoted, color: "#d4a017" },
    { label: "Booked", value: f.booked, color: "#27ae60" },
    { label: "Declined", value: f.declined, color: "#e84040" },
  ];
});

// ── Booking peaks helpers ─────────────────────────────────────────────────────
const maxPeak = computed(() =>
  Math.max(1, ...(data.value?.bookingPeaks.map((p) => p.count) ?? [1])),
);
function peakBarPct(val: number) {
  return Math.max(2, (val / maxPeak.value) * 100);
}

// ── Rating helpers ────────────────────────────────────────────────────────────
function stars(rating: number) {
  if (rating === 0) return "—";
  const full = Math.floor(rating);
  const half = rating - full >= 0.5 ? 1 : 0;
  return "★".repeat(full) + (half ? "½" : "") + " " + rating.toFixed(1);
}
function ratingColor(r: number) {
  if (r === 0) return "var(--color-muted)";
  if (r >= 4.5) return "#27ae60";
  if (r >= 3.5) return "var(--color-gold)";
  return "#e84040";
}
</script>

<template>
  <div class="analytics-view">
    <div class="page-header">
      <h2>Analytics</h2>
      <p class="subtitle">
        Historical trends and pipeline depth — the numbers behind your Overview.
      </p>
    </div>

    <div v-if="loading" class="state-msg">Loading…</div>
    <div v-else-if="error" class="state-msg err">{{ error }}</div>

    <template v-else-if="data">
      <!-- ── 1. Revenue trend ─────────────────────────────────────────────── -->
      <section class="card">
        <h3 class="card-title">Revenue — Last 12 Months</h3>
        <p class="card-sub">Actual booking value confirmed each month</p>
        <div class="bar-chart">
          <div
            v-for="m in data.revenueByMonth"
            :key="m.month + m.year"
            class="bar-col"
          >
            <span class="bar-label-top">{{
              m.revenue > 0 ? formatRevenue(m.revenue) : ""
            }}</span>
            <div
              class="bar"
              :style="{ height: revenueBarPct(m.revenue) + '%' }"
              :class="{ 'bar--active': m.bookings > 0 }"
            ></div>
            <span class="bar-label">{{ m.month }}</span>
          </div>
        </div>
        <p class="chart-note">
          Total:
          <strong
            >{{
              formatRevenue(
                data.revenueByMonth.reduce((s, m) => s + m.revenue, 0),
              )
            }}
            RON</strong
          >
          across
          <strong
            >{{
              data.revenueByMonth.reduce((s, m) => s + m.bookings, 0)
            }}
            bookings</strong
          >
        </p>
      </section>

      <!-- ── 2. Lead funnel ──────────────────────────────────────────────── -->
      <section class="card">
        <h3 class="card-title">Lead Pipeline</h3>
        <p class="card-sub">Where your enquiries end up</p>
        <div class="funnel">
          <div
            v-for="stage in funnelStages"
            :key="stage.label"
            class="funnel-stage"
          >
            <div class="funnel-meta">
              <span class="funnel-label">{{ stage.label }}</span>
              <span class="funnel-count">{{ stage.value }}</span>
              <span class="funnel-pct">{{ funnelPct(stage.value) }}%</span>
            </div>
            <div class="funnel-bar-wrap">
              <div
                class="funnel-bar"
                :style="{
                  width: funnelPct(stage.value) + '%',
                  background: stage.color,
                }"
              ></div>
            </div>
          </div>
        </div>
        <p class="chart-note">
          Total leads: <strong>{{ funnelTotal }}</strong>
        </p>
      </section>

      <!-- ── 3. Booking peaks ────────────────────────────────────────────── -->
      <section class="card">
        <h3 class="card-title">Wedding Month Distribution</h3>
        <p class="card-sub">
          How your confirmed bookings' event dates fall across the year
        </p>
        <div class="bar-chart peak-chart">
          <div
            v-for="p in data.bookingPeaks"
            :key="p.monthNumber"
            class="bar-col"
          >
            <span class="bar-label-top">{{ p.count || "" }}</span>
            <div
              class="bar bar--peak"
              :style="{ height: peakBarPct(p.count) + '%' }"
            ></div>
            <span class="bar-label">{{ p.month }}</span>
          </div>
        </div>
      </section>

      <!-- ── 4. Rating trend ─────────────────────────────────────────────── -->
      <section class="card">
        <h3 class="card-title">Rating by Quarter</h3>
        <p class="card-sub">How your average score has moved over time</p>
        <div class="rating-table">
          <div
            v-for="q in data.ratingByQuarter"
            :key="q.label"
            class="rating-row"
          >
            <span class="q-label">{{ q.label }}</span>
            <span class="q-stars" :style="{ color: ratingColor(q.avgRating) }">
              {{ stars(q.avgRating) }}
            </span>
            <span class="q-count">{{
              q.reviewCount
                ? `${q.reviewCount} review${q.reviewCount !== 1 ? "s" : ""}`
                : "no reviews"
            }}</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.analytics-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.page-header {
  margin-bottom: 4px;
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.subtitle {
  margin: 0;
  font-size: 0.88rem;
  color: var(--color-muted);
}
.state-msg {
  padding: 48px;
  text-align: center;
  color: var(--color-muted);
}
.err {
  color: #e84040;
}

/* ── Card ──────────────────────────────────────────────────────────── */
.card {
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 24px 28px;
}
.card-title {
  margin: 0 0 4px;
  font-size: 1rem;
  font-weight: 700;
}
.card-sub {
  margin: 0 0 20px;
  font-size: 0.8rem;
  color: var(--color-muted);
}
.chart-note {
  margin: 12px 0 0;
  font-size: 0.8rem;
  color: var(--color-muted);
}

/* ── Bar chart (revenue + peaks) ──────────────────────────────────── */
.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 160px;
  padding-bottom: 24px; /* room for labels */
  position: relative;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  height: 100%;
  position: relative;
}
.bar-label-top {
  font-size: 0.62rem;
  color: var(--color-muted);
  margin-bottom: 2px;
  white-space: nowrap;
}
.bar {
  width: 100%;
  background: var(--color-gold-light, #f5eddb);
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
  min-height: 3px;
}
.bar--active {
  background: var(--color-gold, #c9a84c);
}
.bar--peak {
  background: #7b9dd1;
}
.bar-label {
  position: absolute;
  bottom: -22px;
  font-size: 0.65rem;
  color: var(--color-muted);
  white-space: nowrap;
}

/* ── Lead funnel ───────────────────────────────────────────────────── */
.funnel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.funnel-stage {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.funnel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.funnel-label {
  font-size: 0.82rem;
  font-weight: 600;
  width: 130px;
  flex-shrink: 0;
}
.funnel-count {
  font-size: 0.82rem;
  font-weight: 700;
  min-width: 28px;
  text-align: right;
}
.funnel-pct {
  font-size: 0.75rem;
  color: var(--color-muted);
  min-width: 36px;
}
.funnel-bar-wrap {
  height: 8px;
  background: var(--color-surface, #f5f5f5);
  border-radius: 4px;
  overflow: hidden;
}
.funnel-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.4s ease;
  min-width: 4px;
}

/* ── Rating table ──────────────────────────────────────────────────── */
.rating-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.rating-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 14px;
  background: var(--color-surface, #fafafa);
  border-radius: 8px;
}
.q-label {
  font-size: 0.82rem;
  font-weight: 700;
  width: 70px;
  flex-shrink: 0;
}
.q-stars {
  font-size: 0.88rem;
  font-weight: 600;
  flex: 1;
}
.q-count {
  font-size: 0.75rem;
  color: var(--color-muted);
  white-space: nowrap;
}
</style>
