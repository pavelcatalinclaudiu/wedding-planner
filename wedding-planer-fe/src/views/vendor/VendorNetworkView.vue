<script setup lang="ts">
import { ref, onMounted, watch } from "vue";
import { useI18n } from "vue-i18n";
import UpgradeGate from "@/components/ui/UpgradeGate.vue";
import { networkApi } from "@/api/network.api";
import type { VendorPartner } from "@/types/vendor.types";
import { X } from "lucide-vue-next";
import { useFeatureAccess } from "@/composables/useFeatureAccess";

const partners = ref<VendorPartner[]>([]);
const loading = ref(false);
const { canAccess } = useFeatureAccess();

// ── Search / add ────────────────────────────────────────────────────────────
const searchQuery = ref("");
const searchResults = ref<VendorPartner[]>([]);
const searching = ref(false);
const showDropdown = ref(false);
const adding = ref(false);
const error = ref("");
let searchTimer: ReturnType<typeof setTimeout> | null = null;
const { t } = useI18n();

onMounted(async () => {
  if (!canAccess("network")) return;
  loading.value = true;
  try {
    const res = await networkApi.getPartners();
    partners.value = res.data;
  } finally {
    loading.value = false;
  }
});

watch(searchQuery, (val) => {
  if (searchTimer) clearTimeout(searchTimer);
  const trimmed = val.trim();
  if (trimmed.length < 2) {
    searchResults.value = [];
    showDropdown.value = false;
    return;
  }
  searching.value = true;
  showDropdown.value = true;
  searchTimer = setTimeout(async () => {
    try {
      const res = await networkApi.searchVendors(trimmed);
      searchResults.value = res.data;
    } finally {
      searching.value = false;
    }
  }, 320);
});

async function addById(partnerId: string) {
  adding.value = true;
  error.value = "";
  try {
    const res = await networkApi.addPartner({ partnerId });
    partners.value.push(res.data);
    searchQuery.value = "";
    showDropdown.value = false;
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? t("vendor.network.couldNotAdd");
  } finally {
    adding.value = false;
  }
}

async function addByName() {
  const name = searchQuery.value.trim();
  if (!name) return;
  adding.value = true;
  error.value = "";
  try {
    const res = await networkApi.addPartner({ partnerName: name });
    partners.value.push(res.data);
    searchQuery.value = "";
    showDropdown.value = false;
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? t("vendor.network.couldNotAdd");
  } finally {
    adding.value = false;
  }
}

async function remove(id: string) {
  await networkApi.removePartner(id);
  partners.value = partners.value.filter((p) => p.id !== id);
}

function closeDropdown() {
  setTimeout(() => {
    showDropdown.value = false;
  }, 180);
}

function categoryLabel(cat?: string) {
  return cat ? cat.replace(/_/g, " ") : "";
}

function initials(name: string) {
  return name
    .split(" ")
    .slice(0, 2)
    .map((w) => w[0])
    .join("")
    .toUpperCase();
}
</script>

<template>
  <UpgradeGate feature="network">
    <div class="network-view">
      <div class="page-header">
        <h2>{{ t("vendor.network.title") }}</h2>
        <p class="subtitle">{{ t("vendor.network.subtitle") }}</p>
      </div>

      <!-- ── Add partner ── -->
      <div class="add-section">
        <div class="search-wrap">
          <input
            v-model="searchQuery"
            class="search-input"
            :placeholder="t('vendor.network.searchPlaceholder')"
            autocomplete="off"
            @focus="showDropdown = searchQuery.trim().length >= 2"
            @blur="closeDropdown"
          />
          <!-- Dropdown -->
          <div v-if="showDropdown" class="dropdown">
            <div v-if="searching" class="dd-loading">
              {{ t("vendor.network.searching") }}
            </div>
            <template v-else>
              <button
                v-for="r in searchResults"
                :key="r.partnerId"
                class="dd-item"
                @mousedown.prevent="addById(r.partnerId!)"
              >
                <div
                  class="dd-avatar"
                  :style="
                    r.partnerCoverPhoto
                      ? { backgroundImage: `url('${r.partnerCoverPhoto}')` }
                      : {}
                  "
                >
                  <span v-if="!r.partnerCoverPhoto">{{
                    initials(r.partnerName)
                  }}</span>
                </div>
                <div class="dd-info">
                  <span class="dd-name">{{ r.partnerName }}</span>
                  <span class="dd-meta"
                    >{{ categoryLabel(r.partnerCategory)
                    }}<template v-if="r.partnerCity">
                      · {{ r.partnerCity }}</template
                    ></span
                  >
                </div>
                <span class="dd-platform-badge">{{
                  t("vendor.network.onPlatform")
                }}</span>
              </button>

              <div
                v-if="!searchResults.length && !searching"
                class="dd-no-results"
              >
                {{ t("vendor.network.notOnPlatform") }}
              </div>

              <!-- Always offer add-by-name option -->
              <button
                class="dd-item dd-item-name"
                @mousedown.prevent="addByName"
              >
                <div class="dd-avatar dd-avatar-name">＋</div>
                <div class="dd-info">
                  <span class="dd-name">{{
                    t("vendor.network.addAsPartner", {
                      name: searchQuery.trim(),
                    })
                  }}</span>
                  <span class="dd-meta">{{
                    t("vendor.network.notLinked")
                  }}</span>
                </div>
              </button>
            </template>
          </div>
        </div>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </div>

      <!-- ── Partner list ── -->
      <div v-if="loading" class="loading">{{ t("common.loading") }}</div>
      <div v-else-if="partners.length === 0" class="empty">
        <p>{{ t("vendor.network.noPartners") }}</p>
        <p class="empty-hint">{{ t("vendor.network.noPartnersHint") }}</p>
      </div>
      <div v-else class="partner-list">
        <div v-for="p in partners" :key="p.id" class="partner-card">
          <div
            class="pc-avatar"
            :style="
              p.partnerCoverPhoto
                ? { backgroundImage: `url('${p.partnerCoverPhoto}')` }
                : {}
            "
          >
            <span v-if="!p.partnerCoverPhoto">{{
              initials(p.partnerName)
            }}</span>
          </div>
          <div class="pc-info">
            <div class="pc-name-row">
              <span class="pc-name">{{ p.partnerName }}</span>
              <span v-if="p.onPlatform" class="pc-badge">{{
                t("vendor.network.onPlatform")
              }}</span>
            </div>
            <span v-if="p.partnerCategory || p.partnerCity" class="pc-meta">
              {{ categoryLabel(p.partnerCategory)
              }}<template v-if="p.partnerCategory && p.partnerCity">
                · </template
              >{{ p.partnerCity }}
            </span>
          </div>
          <button
            class="pc-remove"
            :title="t('vendor.network.removePartner')"
            @click="remove(p.id)"
          >
            <X :size="16" />
          </button>
        </div>
      </div>
    </div>
  </UpgradeGate>
</template>

<style scoped>
h2 {
  margin: 0 0 6px;
  font-size: 1.4rem;
  font-weight: 700;
}
.page-header {
  margin-bottom: 28px;
}
.subtitle {
  color: var(--color-muted);
  margin: 0 0 28px;
  font-size: 0.88rem;
  max-width: 540px;
  line-height: 1.5;
}

/* ── Add section ── */
.add-section {
  margin-bottom: 28px;
}
.search-wrap {
  position: relative;
}
.search-input {
  width: 100%;
  padding: 11px 14px;
  border: 1.5px solid var(--color-border);
  border-radius: 10px;
  font-size: 0.9rem;
  font-family: inherit;
  outline: none;
  background: var(--color-white);
  transition: border-color 0.15s;
  box-sizing: border-box;
}
.search-input:focus {
  border-color: var(--color-gold);
}
.dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  z-index: 100;
  overflow: hidden;
}
.dd-loading {
  padding: 14px 16px;
  font-size: 0.85rem;
  color: var(--color-muted);
}
.dd-no-results {
  padding: 10px 16px;
  font-size: 0.83rem;
  color: var(--color-muted);
}
.dd-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  width: 100%;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  transition: background 0.12s;
  font-family: inherit;
  border-top: 1px solid var(--color-border);
}
.dd-item:first-child {
  border-top: none;
}
.dd-item:hover {
  background: var(--color-surface, #f9f9f7);
}
.dd-item-name {
  border-top: 1px solid var(--color-border);
}
.dd-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--color-gold);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.dd-avatar-name {
  background: var(--color-muted, #999);
  font-size: 1rem;
}
.dd-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}
.dd-name {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-text);
}
.dd-meta {
  font-size: 0.78rem;
  color: var(--color-muted);
  text-transform: capitalize;
}
.dd-platform-badge {
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--chip-green-bg, #e8f9f0);
  color: var(--color-green, #27ae60);
  border-radius: 8px;
  padding: 2px 8px;
  flex-shrink: 0;
}
.error-msg {
  color: var(--color-error, #e74c3c);
  font-size: 0.83rem;
  margin: 8px 0 0;
}

/* ── Partner list ── */
.loading,
.empty {
  color: var(--color-muted);
  text-align: center;
  padding: 48px 32px;
}
.empty-hint {
  font-size: 0.82rem;
  margin-top: 6px;
}
.partner-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.partner-card {
  display: flex;
  align-items: center;
  gap: 14px;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 14px 16px;
}
.pc-avatar {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--color-gold);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}
.pc-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.pc-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pc-name {
  font-size: 0.92rem;
  font-weight: 600;
}
.pc-badge {
  font-size: 0.7rem;
  font-weight: 700;
  background: var(--chip-green-bg, #e8f9f0);
  color: var(--color-green, #27ae60);
  border-radius: 8px;
  padding: 2px 8px;
}
.pc-meta {
  font-size: 0.79rem;
  color: var(--color-muted);
  text-transform: capitalize;
}
.pc-remove {
  background: none;
  border: 1px solid var(--color-border);
  border-radius: 7px;
  width: 30px;
  height: 30px;
  font-size: 0.75rem;
  cursor: pointer;
  color: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background 0.12s,
    color 0.12s;
  flex-shrink: 0;
}
.pc-remove:hover {
  background: #fef2f2;
  color: #e74c3c;
  border-color: #fca5a5;
}
</style>
