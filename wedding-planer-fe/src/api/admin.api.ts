import { apiClient } from "./client";
import type {
  AdminStats,
  AdminUser,
  AdminVendor,
  AdminReview,
  AdminPagedResponse,
} from "@/types/admin.types";

export const adminApi = {
  // Stats
  getStats: () => apiClient.get<AdminStats>("/admin/stats"),

  // Users
  listUsers: (params?: {
    role?: string;
    search?: string;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<AdminPagedResponse<AdminUser>>("/admin/users", { params }),

  deleteUser: (id: string) => apiClient.delete(`/admin/users/${id}`),

  // Vendors
  listVendors: (params?: {
    search?: string;
    category?: string;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<AdminPagedResponse<AdminVendor>>("/admin/vendors", {
      params,
    }),

  suspendVendor: (id: string) => apiClient.put(`/admin/vendors/${id}/suspend`),
  activateVendor: (id: string) =>
    apiClient.put(`/admin/vendors/${id}/activate`),
  toggleVerifyVendor: (id: string) =>
    apiClient.put(`/admin/vendors/${id}/verify`),
  setVendorPlan: (id: string, plan: string) =>
    apiClient.put(`/admin/vendors/${id}/plan`, null, { params: { plan } }),
  setCouplePlan: (id: string, plan: string) =>
    apiClient.put(`/admin/couples/${id}/plan`, null, { params: { plan } }),
  toggleVendorMonetization: (id: string, enabled: boolean) =>
    apiClient.put(`/admin/vendors/${id}/monetization`, null, {
      params: { enabled },
    }),
  toggleCoupleMonetization: (id: string, enabled: boolean) =>
    apiClient.put(`/admin/couples/${id}/monetization`, null, {
      params: { enabled },
    }),
  bulkToggleVendorMonetization: (enabled: boolean) =>
    apiClient.put(`/admin/vendors/monetization/bulk`, null, {
      params: { enabled },
    }),
  bulkToggleCoupleMonetization: (enabled: boolean) =>
    apiClient.put(`/admin/couples/monetization/bulk`, null, {
      params: { enabled },
    }),

  // Reviews
  listReviews: (params?: {
    search?: string;
    status?: string;
    page?: number;
    size?: number;
  }) =>
    apiClient.get<AdminPagedResponse<AdminReview>>("/admin/reviews", {
      params,
    }),

  deleteReview: (id: string) => apiClient.delete(`/admin/reviews/${id}`),
  approveReview: (id: string) => apiClient.put(`/admin/reviews/${id}/approve`),
  rejectReview: (id: string) => apiClient.put(`/admin/reviews/${id}/reject`),
};
