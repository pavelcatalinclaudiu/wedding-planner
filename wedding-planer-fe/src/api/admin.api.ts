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
