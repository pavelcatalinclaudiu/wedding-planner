import { defineStore } from "pinia";
import { ref } from "vue";
import { vendorApi } from "@/api/vendor.api";
import type { VendorProfile } from "@/types/vendor.types";

export const useVendorStore = defineStore("vendor", () => {
  const profile = ref<VendorProfile | null>(null);
  const vendors = ref<VendorProfile[]>([]);
  const loading = ref(false);

  async function fetchMyProfile() {
    loading.value = true;
    try {
      const res = await vendorApi.getMyProfile();
      profile.value = res.data;
    } finally {
      loading.value = false;
    }
  }

  async function updateMyProfile(data: Partial<VendorProfile>) {
    const res = await vendorApi.update(profile.value!.id, data);
    profile.value = res.data;
  }

  async function fetchVendors(params?: Record<string, unknown>) {
    loading.value = true;
    try {
      const res = await vendorApi.list(params);
      vendors.value = res.data;
    } finally {
      loading.value = false;
    }
  }

  async function getVendor(id: string): Promise<VendorProfile> {
    const res = await vendorApi.get(id);
    return res.data;
  }

  return {
    profile,
    vendors,
    loading,
    fetchMyProfile,
    updateMyProfile,
    fetchVendors,
    getVendor,
  };
});
