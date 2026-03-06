import apiClient from "./client";
import type { VendorPartner } from "@/types/vendor.types";

export const networkApi = {
  /** My own partner list (authenticated vendor) */
  getPartners: () => apiClient.get<VendorPartner[]>("/network"),

  /** Partner list for a vendor's public profile page (no auth required) */
  getPublicPartners: (vendorId: string) =>
    apiClient.get<VendorPartner[]>(`/network/vendor/${vendorId}`),

  /** Search platform vendors by business name */
  searchVendors: (q: string) =>
    apiClient.get<VendorPartner[]>(`/network/search`, { params: { q } }),

  /** Add a partner — either a platform vendor via partnerId, or a name-only entry */
  addPartner: (req: { partnerId?: string; partnerName?: string }) =>
    apiClient.post<VendorPartner>("/network", req),

  removePartner: (id: string) => apiClient.delete(`/network/${id}`),
};
