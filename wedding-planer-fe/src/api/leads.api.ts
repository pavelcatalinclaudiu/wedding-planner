import apiClient from "./client";
import type { Lead } from "@/types/lead.types";

export const leadsApi = {
  /** Couple: Create a new lead/enquiry to a vendor */
  create: (payload: {
    vendorId: string;
    eventDate?: string;
    budget?: number;
    message?: string;
  }) => apiClient.post<Lead>("/leads", payload),

  /** Vendor: List all leads for me, with optional status filter */
  listForVendor: (status?: string) =>
    apiClient.get<Lead[]>("/leads", {
      params: status ? { status } : undefined,
    }),

  /** Couple: List my leads */
  listForCouple: () => apiClient.get<Lead[]>("/leads/my"),

  /** Either: Get a single lead */
  get: (id: string) => apiClient.get<Lead>(`/leads/${id}`),

  /** Vendor: Mark as viewed */
  markViewed: (id: string) => apiClient.patch(`/leads/${id}/view`),

  /** Vendor: Accept → creates Conversation, status → IN_DISCUSSION */
  accept: (id: string) => apiClient.patch<Lead>(`/leads/${id}/accept`),

  /** Vendor: Decline */
  decline: (id: string) => apiClient.patch<Lead>(`/leads/${id}/decline`),

  /** Vendor: Export all leads as CSV (PREMIUM only) */
  exportCsv: () =>
    apiClient.get<string>("/leads/export/csv", { responseType: "blob" }),
};
