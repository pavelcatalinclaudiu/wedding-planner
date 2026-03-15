import { defineStore } from "pinia";
import { ref } from "vue";
import { adminApi } from "@/api/admin.api";
import type {
  AdminStats,
  AdminUser,
  AdminVendor,
  AdminReview,
} from "@/types/admin.types";

export const useAdminStore = defineStore("admin", () => {
  const stats = ref<AdminStats | null>(null);
  const statsLoading = ref(false);

  const users = ref<AdminUser[]>([]);
  const usersTotal = ref(0);
  const usersLoading = ref(false);

  const vendors = ref<AdminVendor[]>([]);
  const vendorsTotal = ref(0);
  const vendorsLoading = ref(false);

  const reviews = ref<AdminReview[]>([]);
  const reviewsTotal = ref(0);
  const reviewsLoading = ref(false);

  async function fetchStats() {
    statsLoading.value = true;
    try {
      const res = await adminApi.getStats();
      stats.value = res.data;
    } finally {
      statsLoading.value = false;
    }
  }

  async function fetchUsers(params?: {
    role?: string;
    search?: string;
    page?: number;
    size?: number;
  }) {
    usersLoading.value = true;
    try {
      const res = await adminApi.listUsers(params);
      users.value = res.data.items;
      usersTotal.value = res.data.total;
    } finally {
      usersLoading.value = false;
    }
  }

  async function deleteUser(id: string) {
    await adminApi.deleteUser(id);
    users.value = users.value.filter((u) => u.id !== id);
    usersTotal.value = Math.max(0, usersTotal.value - 1);
  }

  async function fetchVendors(params?: {
    search?: string;
    category?: string;
    page?: number;
    size?: number;
  }) {
    vendorsLoading.value = true;
    try {
      const res = await adminApi.listVendors(params);
      vendors.value = res.data.items;
      vendorsTotal.value = res.data.total;
    } finally {
      vendorsLoading.value = false;
    }
  }

  async function suspendVendor(id: string) {
    await adminApi.suspendVendor(id);
    const v = vendors.value.find((x) => x.id === id);
    if (v) v.isActive = false;
  }

  async function activateVendor(id: string) {
    await adminApi.activateVendor(id);
    const v = vendors.value.find((x) => x.id === id);
    if (v) v.isActive = true;
  }

  async function toggleVerifyVendor(id: string) {
    await adminApi.toggleVerifyVendor(id);
    const v = vendors.value.find((x) => x.id === id);
    if (v) v.isVerified = !v.isVerified;
  }

  async function setVendorPlan(id: string, plan: string) {
    await adminApi.setVendorPlan(id, plan);
    const v = vendors.value.find((x) => x.id === id);
    if (v) v.tier = plan as "FREE" | "STANDARD" | "PREMIUM";
  }

  async function setCouplePlan(id: string, plan: string) {
    await adminApi.setCouplePlan(id, plan);
    const u = users.value.find((x) => x.profileId === id);
    if (u) u.couplePlan = plan as "FREE" | "DREAM_WEDDING";
  }

  async function toggleVendorMonetization(id: string, enabled: boolean) {
    await adminApi.toggleVendorMonetization(id, enabled);
    const v = vendors.value.find((x) => x.id === id);
    if (v) v.monetizationEnabled = enabled;
  }

  async function toggleCoupleMonetization(id: string, enabled: boolean) {
    await adminApi.toggleCoupleMonetization(id, enabled);
    const u = users.value.find((x) => x.profileId === id);
    if (u) u.coupleMonetizationEnabled = enabled;
  }

  async function bulkToggleVendorMonetization(enabled: boolean) {
    await adminApi.bulkToggleVendorMonetization(enabled);
    vendors.value.forEach((v) => (v.monetizationEnabled = enabled));
  }

  async function bulkToggleCoupleMonetization(enabled: boolean) {
    await adminApi.bulkToggleCoupleMonetization(enabled);
    users.value
      .filter((u) => u.role === "COUPLE")
      .forEach((u) => (u.coupleMonetizationEnabled = enabled));
  }

  async function fetchReviews(params?: {
    search?: string;
    status?: string;
    page?: number;
    size?: number;
  }) {
    reviewsLoading.value = true;
    try {
      const res = await adminApi.listReviews(params);
      reviews.value = res.data.items;
      reviewsTotal.value = res.data.total;
    } finally {
      reviewsLoading.value = false;
    }
  }

  async function deleteReview(id: string) {
    await adminApi.deleteReview(id);
    reviews.value = reviews.value.filter((r) => r.id !== id);
    reviewsTotal.value = Math.max(0, reviewsTotal.value - 1);
  }

  async function approveReview(id: string) {
    await adminApi.approveReview(id);
    const r = reviews.value.find((x) => x.id === id);
    if (r) r.status = "APPROVED";
  }

  async function rejectReview(id: string) {
    await adminApi.rejectReview(id);
    const r = reviews.value.find((x) => x.id === id);
    if (r) r.status = "REJECTED";
  }

  return {
    stats,
    statsLoading,
    fetchStats,
    users,
    usersTotal,
    usersLoading,
    fetchUsers,
    deleteUser,
    vendors,
    vendorsTotal,
    vendorsLoading,
    fetchVendors,
    suspendVendor,
    activateVendor,
    toggleVerifyVendor,
    setVendorPlan,
    setCouplePlan,
    toggleVendorMonetization,
    toggleCoupleMonetization,
    bulkToggleVendorMonetization,
    bulkToggleCoupleMonetization,
    reviews,
    reviewsTotal,
    reviewsLoading,
    fetchReviews,
    deleteReview,
    approveReview,
    rejectReview,
  };
});
