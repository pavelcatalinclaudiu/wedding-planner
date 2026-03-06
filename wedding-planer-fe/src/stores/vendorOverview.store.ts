import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { vendorApi } from "@/api/vendor.api";
import type { VendorOverviewDTO } from "@/types/vendor.types";

const STALE_MS = 5 * 60 * 1000; // 5 minutes

export const useVendorOverviewStore = defineStore("vendorOverview", () => {
  const data = ref<VendorOverviewDTO | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const lastFetched = ref<number | null>(null);

  const isStale = computed(() => {
    if (!lastFetched.value) return true;
    return Date.now() - lastFetched.value > STALE_MS;
  });

  async function fetchOverview(force = false) {
    if (!force && !isStale.value) return;
    loading.value = true;
    error.value = null;
    try {
      const res = await vendorApi.getOverviewStats();
      data.value = res.data;
      lastFetched.value = Date.now();
    } catch {
      error.value = "Failed to load overview stats";
    } finally {
      loading.value = false;
    }
  }

  /** Call this from other stores after mutations that affect overview data. */
  function invalidate() {
    lastFetched.value = null;
  }

  /** Wipe all data — call on logout or user switch so stale data is never shown. */
  function reset() {
    data.value = null;
    loading.value = false;
    error.value = null;
    lastFetched.value = null;
  }

  return { data, loading, error, isStale, fetchOverview, invalidate, reset };
});
