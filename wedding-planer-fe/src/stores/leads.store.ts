import { defineStore } from "pinia";
import { ref, computed } from "vue";
import { leadsApi } from "@/api/leads.api";
import type { Lead } from "@/types/lead.types";

export const useLeadsStore = defineStore("leads", () => {
  const leads = ref<Lead[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // ── Computed ──────────────────────────────────────────────────────────────

  const newLeads = computed(() =>
    leads.value.filter((l) => l.status === "NEW"),
  );

  const activeLeads = computed(() =>
    leads.value.filter((l) => !["BOOKED", "DECLINED"].includes(l.status)),
  );

  const newLeadsCount = computed(() => newLeads.value.length);

  // ── Actions ───────────────────────────────────────────────────────────────

  async function fetchVendorLeads(status?: string) {
    loading.value = true;
    error.value = null;
    try {
      const res = await leadsApi.listForVendor(status);
      leads.value = res.data;
    } catch (e: any) {
      error.value = e.message ?? "Failed to load leads";
    } finally {
      loading.value = false;
    }
  }

  async function fetchCoupleLeads() {
    loading.value = true;
    error.value = null;
    try {
      const res = await leadsApi.listForCouple();
      leads.value = res.data;
    } catch (e: any) {
      error.value = e.message ?? "Failed to load leads";
    } finally {
      loading.value = false;
    }
  }

  async function acceptLead(id: string) {
    const res = await leadsApi.accept(id);
    _replace(res.data);
    return res.data;
  }

  async function declineLead(id: string) {
    const res = await leadsApi.decline(id);
    _replace(res.data);
    return res.data;
  }

  function _replace(updated: Lead) {
    const idx = leads.value.findIndex((l) => l.id === updated.id);
    if (idx !== -1) leads.value[idx] = updated;
    else leads.value.unshift(updated);
  }

  function reset() {
    leads.value = [];
    loading.value = false;
    error.value = null;
  }

  return {
    leads,
    loading,
    error,
    newLeads,
    activeLeads,
    newLeadsCount,
    fetchVendorLeads,
    fetchCoupleLeads,
    acceptLead,
    declineLead,
    reset,
  };
});
