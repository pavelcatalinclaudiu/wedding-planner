<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useDebounce } from "@/composables/useDebounce";
import { vendorApi } from "@/api/vendor.api";
import type { VendorProfile } from "@/types/vendor.types";
import type { Component } from "vue";
import {
  Camera,
  Landmark,
  Flower2,
  Utensils,
  Music,
  Search,
  Clock,
} from "lucide-vue-next";

const router = useRouter();

const query = ref("");
const results = ref<VendorProfile[]>([]);
const loading = ref(false);
const open = ref(false);
const inputRef = ref<HTMLInputElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);

const debouncedQuery = useDebounce(() => query.value, 300);

const categories: { key: string; label: string; icon: Component }[] = [
  { key: "PHOTOGRAPHER", label: "Photography", icon: Camera },
  { key: "VENUE", label: "Venue", icon: Landmark },
  { key: "FLORIST", label: "Florals", icon: Flower2 },
  { key: "CATERER", label: "Catering", icon: Utensils },
  { key: "BAND", label: "Music", icon: Music },
];

const RECENT_KEY = "eternelle_recent_searches";

const recentSearches = computed<string[]>(() => {
  try {
    return JSON.parse(localStorage.getItem(RECENT_KEY) ?? "[]");
  } catch {
    return [];
  }
});

function saveRecentSearch(q: string) {
  const existing = recentSearches.value.filter((s) => s !== q);
  const updated = [q, ...existing].slice(0, 3);
  localStorage.setItem(RECENT_KEY, JSON.stringify(updated));
}

watch(debouncedQuery, async (val) => {
  if (!val.trim()) {
    results.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await vendorApi.list({ q: val, limit: 5 });
    results.value = res.data.slice(0, 5);
  } catch {
    results.value = [];
  } finally {
    loading.value = false;
  }
});

function onFocus() {
  open.value = true;
}

function goToVendor(vendor: VendorProfile) {
  open.value = false;
  query.value = "";
  router.push(`/vendors/${vendor.id}`);
}

function goToCategory(key: string) {
  open.value = false;
  query.value = "";
  router.push(`/vendors?category=${key}`);
}

function goToAllResults() {
  if (!query.value.trim()) return;
  saveRecentSearch(query.value.trim());
  open.value = false;
  const q = query.value.trim();
  query.value = "";
  router.push(`/vendors?q=${encodeURIComponent(q)}`);
}

function goToRecentSearch(q: string) {
  open.value = false;
  query.value = "";
  router.push(`/vendors?q=${encodeURIComponent(q)}`);
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === "Escape") {
    open.value = false;
    inputRef.value?.blur();
  }
  if (e.key === "Enter") {
    goToAllResults();
  }
}

function handleClickOutside(e: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
    open.value = false;
  }
}

onMounted(() => document.addEventListener("mousedown", handleClickOutside));
onUnmounted(() =>
  document.removeEventListener("mousedown", handleClickOutside),
);
</script>

<template>
  <div class="dsb" ref="containerRef">
    <div class="dsb-input-wrap" :class="{ focused: open }">
      <span class="dsb-icon"><Search :size="18" /></span>
      <input
        ref="inputRef"
        v-model="query"
        class="dsb-input"
        type="text"
        placeholder="Search photographers, venues, florists…"
        @focus="onFocus"
        @keydown="handleKeydown"
      />
    </div>

    <!-- Overlay panel -->
    <div v-if="open" class="dsb-panel">
      <!-- Idle state: recent + categories -->
      <template v-if="!query.trim()">
        <div v-if="recentSearches.length" class="dsb-section">
          <p class="dsb-section-label">Recent searches</p>
          <button
            v-for="s in recentSearches"
            :key="s"
            class="dsb-row dsb-recent"
            @click="goToRecentSearch(s)"
          >
            <span class="dsb-row-icon"><Clock :size="14" /></span>
            <span>{{ s }}</span>
          </button>
        </div>
        <div class="dsb-section">
          <p class="dsb-section-label">Browse by category</p>
          <div class="dsb-cats">
            <button
              v-for="cat in categories"
              :key="cat.key"
              class="dsb-cat-pill"
              @click="goToCategory(cat.key)"
            >
              <component :is="cat.icon" :size="14" /> {{ cat.label }}
            </button>
          </div>
        </div>
      </template>

      <!-- Active search results -->
      <template v-else>
        <div v-if="loading" class="dsb-loading">Searching…</div>
        <div v-else-if="results.length" class="dsb-section">
          <button
            v-for="v in results"
            :key="v.id"
            class="dsb-row dsb-vendor-row"
            @click="goToVendor(v)"
          >
            <div
              class="dsb-vendor-photo"
              :style="
                v.photos?.[0]?.url
                  ? { backgroundImage: `url('${v.photos[0].url}')` }
                  : {}
              "
            >
              {{ !v.photos?.[0]?.url ? v.businessName[0] : "" }}
            </div>
            <div class="dsb-vendor-info">
              <span class="dsb-vendor-name">{{ v.businessName }}</span>
              <span class="dsb-vendor-meta"
                >{{ v.category?.replace(/_/g, " ") }} · {{ v.city }}</span
              >
            </div>
            <span v-if="v.averageRating" class="dsb-vendor-rating"
              >★ {{ v.averageRating.toFixed(1) }}</span
            >
          </button>
        </div>
        <div v-else class="dsb-empty">No results for "{{ query }}"</div>
        <button class="dsb-see-all" @click="goToAllResults">
          See all results for "{{ query }}" →
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.dsb {
  position: relative;
  width: 320px;
}

.dsb-input-wrap {
  display: flex;
  align-items: center;
  border: 1.5px solid var(--color-border, #e5e5e5);
  border-radius: 10px;
  padding: 0 14px;
  transition: border-color 0.2s;
}

.dsb-input-wrap.focused {
  border-color: var(--color-gold, #c9a84c);
}

.dsb-icon {
  font-size: 0.85rem;
  margin-right: 8px;
  opacity: 0.5;
}

.dsb-input {
  border: none;
  background: transparent;
  width: 100%;
  padding: 9px 0;
  font-size: 0.875rem;
  color: var(--color-text, #1c1c1c);
  outline: none;
}

.dsb-input::placeholder {
  color: var(--color-muted, #999);
}

/* Panel */
.dsb-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: var(--color-white, #fff);
  border: 1px solid var(--color-border, #e5e5e5);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 200;
  min-width: 360px;
}

.dsb-section {
  padding: 12px 0 4px;
}

.dsb-section-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-muted, #999);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  padding: 0 16px 8px;
  margin: 0;
}

.dsb-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 16px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s;
}

.dsb-row:hover {
  background: var(--color-surface, #f5f5f5);
}

.dsb-row-icon {
  font-size: 0.85rem;
  opacity: 0.5;
}

.dsb-recent {
  font-size: 0.875rem;
  color: var(--color-text, #1c1c1c);
}

.dsb-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 12px;
}

.dsb-cat-pill {
  background: var(--color-surface, #f5f5f5);
  border: 1px solid var(--color-border, #e5e5e5);
  border-radius: 20px;
  padding: 5px 14px;
  font-size: 0.8rem;
  cursor: pointer;
  transition:
    border-color 0.15s,
    background 0.15s;
}

.dsb-cat-pill:hover {
  border-color: var(--color-gold, #c9a84c);
  background: var(--color-gold-light);
}

.dsb-vendor-row {
  align-items: center;
}

.dsb-vendor-photo {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--color-gold, #c9a84c);
  color: #fff;
  font-weight: 700;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.dsb-vendor-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.dsb-vendor-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text, #1c1c1c);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dsb-vendor-meta {
  font-size: 0.75rem;
  color: var(--color-muted, #999);
  text-transform: capitalize;
}

.dsb-vendor-rating {
  font-size: 0.8rem;
  color: var(--color-gold, #c9a84c);
  font-weight: 600;
}

.dsb-see-all {
  width: 100%;
  padding: 12px 16px;
  background: var(--color-surface, #f5f5f5);
  border: none;
  border-top: 1px solid var(--color-border, #e5e5e5);
  font-size: 0.875rem;
  color: var(--color-gold, #c9a84c);
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  transition: background 0.15s;
}

.dsb-see-all:hover {
  background: var(--color-gold-light);
}

.dsb-loading,
.dsb-empty {
  padding: 16px;
  text-align: center;
  font-size: 0.875rem;
  color: var(--color-muted, #999);
}
</style>
