import { ref } from "vue";
import { leadsApi } from "@/api/leads.api";
import type { Lead } from "@/types/lead.types";

export function useLeads() {
  const leads = ref<Lead[]>([]);
  const lead = ref<Lead | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchVendorLeads(status?: string) {
    loading.value = true;
    error.value = null;
    try {
      leads.value = (await leadsApi.listForVendor(status)).data;
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to load leads";
    } finally {
      loading.value = false;
    }
  }

  async function fetchCoupleLeads() {
    loading.value = true;
    error.value = null;
    try {
      leads.value = (await leadsApi.listForCouple()).data;
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to load leads";
    } finally {
      loading.value = false;
    }
  }

  async function fetchLead(id: string) {
    loading.value = true;
    error.value = null;
    try {
      lead.value = (await leadsApi.get(id)).data;
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to load lead";
    } finally {
      loading.value = false;
    }
  }

  async function accept(id: string) {
    const updated = (await leadsApi.accept(id)).data;
    _patchInList(updated);
    if (lead.value?.id === id) lead.value = updated;
    return updated;
  }

  async function decline(id: string) {
    const updated = (await leadsApi.decline(id)).data;
    _patchInList(updated);
    if (lead.value?.id === id) lead.value = updated;
    return updated;
  }

  function _patchInList(updated: Lead) {
    const idx = leads.value.findIndex((l) => l.id === updated.id);
    if (idx !== -1) leads.value[idx] = updated;
  }

  return {
    leads,
    lead,
    loading,
    error,
    fetchVendorLeads,
    fetchCoupleLeads,
    fetchLead,
    accept,
    decline,
  };
}
