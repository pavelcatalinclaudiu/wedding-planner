import apiClient from "./client";
import type {
  VendorProfile,
  VendorProfileRequest,
  AvailabilityBlock,
  VendorDeepAnalytics,
} from "@/types/vendor.types";

export const vendorApi = {
  list: (params?: Record<string, unknown>) =>
    apiClient.get<VendorProfile[]>("/vendors", { params }),

  get: (id: string) => apiClient.get<VendorProfile>(`/vendors/${id}`),

  update: (id: string, data: Partial<VendorProfileRequest>) =>
    apiClient.put<VendorProfile>(`/vendors/${id}`, data),

  getMyProfile: () => apiClient.get<VendorProfile>("/vendors/me"),

  createProfile: (data: Partial<VendorProfileRequest>) =>
    apiClient.post<VendorProfile>("/vendors", data),

  // Photos
  uploadPhoto: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiClient.post(`/vendors/me/photos`, form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  deletePhoto: (photoId: string) =>
    apiClient.delete(`/vendors/me/photos/${photoId}`),
  reorderPhotos: (order: string[]) =>
    apiClient.patch(`/vendors/me/photos/reorder`, { order }),

  // Packages
  addPackage: (id: string, data: Record<string, unknown>) =>
    apiClient.post(`/vendors/${id}/packages`, data),
  updatePackage: (id: string, pkgId: string, data: Record<string, unknown>) =>
    apiClient.put(`/vendors/${id}/packages/${pkgId}`, data),
  deletePackage: (id: string, pkgId: string) =>
    apiClient.delete(`/vendors/${id}/packages/${pkgId}`),

  // Availability
  getAvailability: () =>
    apiClient.get<AvailabilityBlock[]>(`/vendors/me/availability`),
  getPublicAvailability: (vendorId: string) =>
    apiClient.get<AvailabilityBlock[]>(`/vendors/${vendorId}/availability`),
  blockDate: (date: string) =>
    apiClient.post(`/vendors/me/availability/block`, { date }),
  unblockDate: (date: string) =>
    apiClient.delete(`/vendors/me/availability/block/${date}`),

  // Network helpers are centralised in network.api.ts

  // Visibility
  toggleVisibility: () =>
    apiClient.patch<{ active: boolean }>("/vendors/me/visibility"),

  // Analytics
  getAnalytics: (range?: string) =>
    apiClient.get("/vendors/analytics", { params: { range } }),
  getDeepAnalytics: () =>
    apiClient.get<VendorDeepAnalytics>("/vendors/analytics/deep"),

  // Public stats (response time, etc.) — no auth required
  getStats: (id: string) =>
    apiClient.get<{ avgResponseTimeHours: number }>(`/vendors/${id}/stats`),

  // Overview stats for vendor dashboard — auth required (VENDOR role)
  getOverviewStats: () =>
    apiClient.get<import("@/types/vendor.types").VendorOverviewDTO>(
      "/vendor/overview-stats",
    ),
};
