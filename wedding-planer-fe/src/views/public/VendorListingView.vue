<script setup lang="ts">
import { ref, computed, watch, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useI18n } from "vue-i18n";
import { vendorApi } from "@/api/vendor.api";
import { useDebounce } from "@/composables/useDebounce";
import VendorCard from "@/components/vendor/VendorCard.vue";
import PublicNavbar from "@/components/public/PublicNavbar.vue";
import type { VendorProfile } from "@/types/vendor.types";
import { Search, Calendar, X } from "lucide-vue-next";

const route = useRoute();
const router = useRouter();
const { t } = useI18n();

const vendors = ref<VendorProfile[]>([]);
const loading = ref(false);
const search = ref((route.query.q as string) ?? "");
const category = ref((route.query.category as string) ?? "");
const city = ref((route.query.city as string) ?? "");
const priceMin = ref<number | null>(Number(route.query.price_min) || null);
const priceMax = ref<number | null>(Number(route.query.price_max) || null);
const minRating = ref(Number(route.query.rating) || 0);
const featuredOnly = ref(route.query.featured === "1");
const sortBy = ref((route.query.sort as string) ?? "");
const weddingDate = ref((route.query.date as string) ?? "");
const page = ref(Number(route.query.page) || 1);
const PAGE_SIZE = 24;

const debouncedSearch = useDebounce(() => search.value, 500);
const debouncedCity = useDebounce(() => city.value, 500);

const categories = computed(() => [
  { key: "PHOTOGRAPHER", label: t("categories.PHOTOGRAPHER") },
  { key: "VIDEOGRAPHER", label: t("categories.VIDEOGRAPHER") },
  { key: "VENUE", label: t("categories.VENUE") },
  { key: "FLORIST", label: t("categories.FLORIST") },
  { key: "CATERER", label: t("categories.CATERER") },
  { key: "CAKE", label: t("categories.CAKE") },
  { key: "BAND", label: t("categories.BAND") },
  { key: "DJ", label: t("categories.DJ") },
  { key: "MAKEUP_ARTIST", label: t("categories.MAKEUP_ARTIST") },
  { key: "HAIR_STYLIST", label: t("categories.HAIR_STYLIST") },
  { key: "OFFICIANT", label: t("categories.OFFICIANT") },
  { key: "PLANNER", label: t("categories.PLANNER") },
  { key: "TRANSPORTATION", label: t("categories.TRANSPORTATION") },
  { key: "LIGHTING", label: t("categories.LIGHTING") },
  {
    key: "INVITATION_STATIONERY",
    label: t("categories.INVITATION_STATIONERY"),
  },
  { key: "JEWELRY", label: t("categories.JEWELRY") },
  { key: "OTHER", label: t("categories.OTHER") },
]);

const activeChips = computed(() => {
  const chips: { key: string; label: string }[] = [];
  if (search.value) chips.push({ key: "q", label: `"${search.value}"` });
  if (category.value)
    chips.push({
      key: "category",
      label:
        categories.value.find((c) => c.key === category.value)?.label ??
        category.value,
    });
  if (city.value) chips.push({ key: "city", label: city.value });
  if (priceMin.value)
    chips.push({
      key: "price_min",
      label: `From ${priceMin.value.toLocaleString()} RON`,
    });
  if (priceMax.value)
    chips.push({
      key: "price_max",
      label: `Up to ${priceMax.value.toLocaleString()} RON`,
    });
  if (minRating.value)
    chips.push({ key: "rating", label: `★ ${minRating.value}+` });
  if (featuredOnly.value)
    chips.push({ key: "featured", label: t("vendors.filterFeatured") });
  if (weddingDate.value)
    chips.push({ key: "date", label: `${weddingDate.value}` });
  return chips;
});

const displayed = computed(() => {
  let list = [...vendors.value];
  if (sortBy.value === "price_asc")
    list.sort((a, b) => (a.basePrice ?? 0) - (b.basePrice ?? 0));
  else if (sortBy.value === "price_desc")
    list.sort((a, b) => (b.basePrice ?? 0) - (a.basePrice ?? 0));
  else if (sortBy.value === "rating")
    list.sort((a, b) => (b.averageRating ?? 0) - (a.averageRating ?? 0));
  const start = (page.value - 1) * PAGE_SIZE;
  return list.slice(start, start + PAGE_SIZE);
});
const totalPages = computed(() =>
  Math.max(1, Math.ceil(vendors.value.length / PAGE_SIZE)),
);

function removeChip(key: string) {
  if (key === "q") search.value = "";
  if (key === "category") category.value = "";
  if (key === "city") city.value = "";
  if (key === "price_min") priceMin.value = null;
  if (key === "price_max") priceMax.value = null;
  if (key === "rating") minRating.value = 0;
  if (key === "featured") featuredOnly.value = false;
  if (key === "date") weddingDate.value = "";
}

function updateURL() {
  router.replace({
    query: {
      q: search.value || undefined,
      category: category.value || undefined,
      city: city.value || undefined,
      price_min: priceMin.value || undefined,
      price_max: priceMax.value || undefined,
      rating: minRating.value || undefined,
      featured: featuredOnly.value ? "1" : undefined,
      sort: sortBy.value || undefined,
      date: weddingDate.value || undefined,
      page: page.value > 1 ? page.value : undefined,
    },
  });
}

async function fetchVendors() {
  loading.value = true;
  try {
    const res = await vendorApi.list({
      q: search.value || undefined,
      category: category.value || undefined,
      city: city.value || undefined,
      date: weddingDate.value || undefined,
    });
    let data = res.data ?? [];
    if (priceMin.value)
      data = data.filter((v) => !v.basePrice || v.basePrice >= priceMin.value!);
    if (priceMax.value)
      data = data.filter((v) => !v.basePrice || v.basePrice <= priceMax.value!);
    if (minRating.value)
      data = data.filter((v) => (v.averageRating ?? 0) >= minRating.value);
    if (featuredOnly.value) data = data.filter((v) => v.tier === "PREMIUM");
    vendors.value = data;
    page.value = 1;
  } finally {
    loading.value = false;
  }
}

watch(
  [
    debouncedSearch,
    debouncedCity,
    category,
    priceMin,
    priceMax,
    minRating,
    featuredOnly,
    weddingDate,
  ],
  () => {
    updateURL();
    fetchVendors();
  },
);

watch(sortBy, updateURL);

onMounted(fetchVendors);
</script>

<template>
  <div class="listing-page">
    <!-- Nav -->
    <PublicNavbar />

    <!-- Hero bar -->
    <div class="listing-hero">
      <div class="lh-inner">
        <h1 class="lh-title">{{ t("vendors.heroTitle") }}</h1>
        <p class="lh-sub">
          {{
            loading
              ? t("vendors.loadingVendors")
              : vendors.length + " " + t("vendors.professionals")
          }}
        </p>
        <div class="lh-search">
          <span class="lh-icon"><Search :size="24" /></span>
          <input
            v-model="search"
            class="lh-input"
            :placeholder="t('vendors.searchByKeyword')"
          />
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="listing-body">
      <!-- Sidebar -->
      <aside class="sidebar">
        <p class="sidebar-title">{{ t("vendors.filtersTitle") }}</p>

        <div class="filter-group">
          <p class="filter-label">{{ t("vendors.filterCategory") }}</p>
          <label class="filter-radio">
            <input v-model="category" type="radio" value="" />
            <span>{{ t("vendors.allCategories") }}</span>
          </label>
          <label v-for="cat in categories" :key="cat.key" class="filter-radio">
            <input v-model="category" type="radio" :value="cat.key" />
            <span>{{ cat.label }}</span>
          </label>
        </div>

        <hr class="filter-sep" />

        <div class="filter-group">
          <p class="filter-label">{{ t("vendors.filterCity") }}</p>
          <input
            v-model="city"
            class="filter-input"
            placeholder="e.g. București"
          />
        </div>

        <hr class="filter-sep" />

        <div class="filter-group">
          <p class="filter-label">{{ t("vendors.filterWeddingDate") }}</p>
          <input
            v-model="weddingDate"
            type="date"
            class="filter-input"
            :min="new Date().toISOString().slice(0, 10)"
          />
          <p class="filter-hint">{{ t("vendors.filterDateHint") }}</p>
        </div>

        <hr class="filter-sep" />

        <div class="filter-group">
          <p class="filter-label">{{ t("vendors.filterPrice") }}</p>
          <div class="price-row">
            <input
              v-model.number="priceMin"
              type="number"
              class="filter-input half"
              placeholder="Min"
              min="0"
            />
            <span class="price-dash">–</span>
            <input
              v-model.number="priceMax"
              type="number"
              class="filter-input half"
              placeholder="Max"
              min="0"
            />
          </div>
        </div>

        <hr class="filter-sep" />

        <div class="filter-group">
          <p class="filter-label">{{ t("vendors.filterRating") }}</p>
          <div class="star-btns">
            <button
              v-for="n in [0, 3, 4, 4.5]"
              :key="n"
              class="star-btn"
              :class="{ active: minRating === n }"
              @click="minRating = n"
            >
              <template v-if="n === 0">{{ t("vendors.anyRating") }}</template>
              <template v-else>★ {{ n }}+</template>
            </button>
          </div>
        </div>

        <hr class="filter-sep" />

        <div class="filter-group">
          <label class="featured-toggle">
            <input v-model="featuredOnly" type="checkbox" />
            <span class="toggle-label">{{ t("vendors.filterFeatured") }}</span>
          </label>
        </div>

        <button
          class="reset-btn"
          @click="
            category = '';
            city = '';
            priceMin = null;
            priceMax = null;
            minRating = 0;
            featuredOnly = false;
            search = '';
            weddingDate = '';
          "
        >
          {{ t("vendors.resetFilters") }}
        </button>
      </aside>

      <!-- Results -->
      <div class="results-pane">
        <!-- Bar -->
        <div class="results-bar">
          <span class="results-count">
            <span v-if="!loading"
              >{{ vendors.length }} {{ t("vendors.professionals") }}</span
            >
            <span v-else>{{ t("vendors.loadingVendors") }}</span>
          </span>
          <div class="sort-wrap">
            <label class="sort-label">{{ t("vendors.sortLabel") }}</label>
            <select v-model="sortBy" class="sort-select">
              <option value="">{{ t("vendors.sortRelevance") }}</option>
              <option value="price_asc">{{ t("vendors.sortPriceLow") }}</option>
              <option value="price_desc">
                {{ t("vendors.sortPriceHigh") }}
              </option>
              <option value="rating">{{ t("vendors.sortTopRated") }}</option>
            </select>
          </div>
        </div>

        <!-- Chips -->
        <div v-if="activeChips.length" class="chips-row">
          <button
            v-for="chip in activeChips"
            :key="chip.key"
            class="chip"
            @click="removeChip(chip.key)"
          >
            {{ chip.label }} <X :size="12" />
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="pane-loading">
          <span class="spinner" /><span>{{ t("vendors.loadingVendors") }}</span>
        </div>

        <!-- Empty -->
        <div v-else-if="vendors.length === 0" class="pane-empty">
          <p class="empty-icon"><Search :size="32" /></p>
          <p>{{ t("vendors.noVendorsMatch") }}</p>
          <button
            class="clear-btn"
            @click="
              category = '';
              city = '';
              search = '';
            "
          >
            {{ t("vendors.clearSearch") }}
          </button>
        </div>

        <!-- Grid -->
        <div v-else class="vendor-grid">
          <VendorCard v-for="v in displayed" :key="v.id" :vendor="v" />
        </div>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="pagination">
          <button
            class="pg-btn"
            :disabled="page <= 1"
            @click="
              page--;
              updateURL();
            "
          >
            {{ t("vendors.prev") }}
          </button>
          <span class="pg-info">{{
            t("vendors.pageOf", { page, total: totalPages })
          }}</span>
          <button
            class="pg-btn"
            :disabled="page >= totalPages"
            @click="
              page++;
              updateURL();
            "
          >
            {{ t("vendors.next") }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}
.listing-page {
  min-height: 100vh;
  background: #f8f8f6;
  padding-top: 64px;
}

/* Nav */
.listing-nav {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid #eee;
}
.nav-logo {
  font-size: 1.4rem;
  font-weight: 900;
  color: #1c1c1c;
  text-decoration: none;
}
.nav-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-right a {
  color: #555;
  text-decoration: none;
  font-size: 0.875rem;
}
.nav-cta {
  background: #1c1c1c;
  color: #fff !important;
  padding: 7px 16px;
  border-radius: 7px;
  font-weight: 600 !important;
}

/* Hero */
.listing-hero {
  background: #1c1c1c;
  padding: 40px 32px;
}
.lh-inner {
  max-width: 680px;
}
.lh-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: #fff;
  margin-bottom: 6px;
}
.lh-sub {
  font-size: 0.87rem;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 18px;
}
.lh-search {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 10px;
  padding: 0 14px;
  gap: 8px;
  max-width: 520px;
}
.lh-icon {
  font-size: 1rem;
  color: #aaa;
}
.lh-input {
  border: none;
  outline: none;
  padding: 12px 0;
  font-size: 0.9rem;
  flex: 1;
  background: transparent;
}
.lh-input::placeholder {
  color: #bbb;
}

/* Body layout */
.listing-body {
  display: flex;
  align-items: flex-start;
  max-width: 1400px;
  margin: 0 auto;
  padding: 28px 24px;
  gap: 28px;
}

/* Sidebar */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 14px;
  padding: 24px;
  position: sticky;
  top: 80px;
}
.sidebar-title {
  font-size: 0.75rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  color: #aaa;
  margin-bottom: 16px;
}
.filter-group {
  margin-bottom: 4px;
}
.filter-label {
  font-size: 0.825rem;
  font-weight: 600;
  color: #444;
  margin-bottom: 10px;
}
.filter-radio {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.83rem;
  color: #555;
  cursor: pointer;
  padding: 4px 0;
}
.filter-radio:hover span {
  color: #c8974a;
}
.filter-input {
  width: 100%;
  border: 1.5px solid #ddd;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 0.83rem;
  outline: none;
  font-family: inherit;
}
.filter-input:focus {
  border-color: #c8974a;
}
.filter-input.half {
  width: calc(50% - 10px);
}
.filter-hint {
  font-size: 0.75rem;
  color: #aaa;
  margin: 5px 0 0;
}
.price-row {
  display: flex;
  align-items: center;
  gap: 6px;
}
.price-dash {
  color: #aaa;
  font-size: 0.85rem;
}
.star-btns {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.star-btn {
  background: #f2f2f0;
  border: 1.5px solid transparent;
  border-radius: 20px;
  padding: 5px 12px;
  font-size: 0.78rem;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.15s;
}
.star-btn.active {
  background: #c8974a;
  color: #fff;
  border-color: #c8974a;
}
.star-btn:hover:not(.active) {
  border-color: #c8974a;
  color: #c8974a;
}
.featured-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.toggle-label {
  font-size: 0.83rem;
  color: #444;
  font-weight: 500;
}
.filter-sep {
  border: none;
  border-top: 1px solid #f0f0f0;
  margin: 16px 0;
}
.reset-btn {
  margin-top: 16px;
  width: 100%;
  background: transparent;
  border: 1.5px solid #ddd;
  border-radius: 8px;
  padding: 8px;
  font-size: 0.8rem;
  color: #888;
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color 0.15s,
    color 0.15s;
}
.reset-btn:hover {
  border-color: #c8974a;
  color: #c8974a;
}

/* Results pane */
.results-pane {
  flex: 1;
  min-width: 0;
}
.results-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.results-count {
  font-size: 0.875rem;
  color: #777;
}
.sort-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sort-label {
  font-size: 0.8rem;
  color: #aaa;
}
.sort-select {
  border: 1.5px solid #ddd;
  border-radius: 7px;
  padding: 6px 10px;
  font-size: 0.83rem;
  outline: none;
  background: #fff;
  cursor: pointer;
}

/* Chips */
.chips-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.chip {
  background: #fdf8ee;
  border: 1.5px solid #e8d4b0;
  color: #8a6030;
  border-radius: 20px;
  padding: 5px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
  font-family: inherit;
}
.chip:hover {
  background: #f5ead5;
}

/* Loading / empty */
.pane-loading {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 80px;
  justify-content: center;
  color: #888;
}
.spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid #e0e0e0;
  border-top-color: #c8974a;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.pane-empty {
  text-align: center;
  padding: 80px 0;
  color: #888;
}
.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 10px;
}
.clear-btn {
  margin-top: 12px;
  background: #c8974a;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 20px;
  cursor: pointer;
  font-family: inherit;
  font-weight: 600;
  font-size: 0.85rem;
}

/* Grid */
.vendor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 20px;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
}
.pg-btn {
  background: #fff;
  border: 1.5px solid #ddd;
  border-radius: 8px;
  padding: 8px 18px;
  font-size: 0.875rem;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.15s;
}
.pg-btn:hover:not(:disabled) {
  border-color: #c8974a;
  color: #c8974a;
}
.pg-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
.pg-info {
  font-size: 0.85rem;
  color: #888;
}

@media (max-width: 900px) {
  .listing-body {
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    position: static;
  }
  .filter-group {
    max-height: 120px;
    overflow-y: auto;
  }
}
@media (max-width: 600px) {
  .listing-hero {
    padding: 28px 20px;
  }
  .listing-body {
    padding: 16px;
  }
}
</style>
