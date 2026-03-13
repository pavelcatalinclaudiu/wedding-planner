import apiClient from "./client";
import type { Guest, GuestRequest, GuestStats } from "@/types/couple.types";

export const guestsApi = {
  // CRUD
  list: () => apiClient.get<Guest[]>("/guests"),
  create: (data: GuestRequest) => apiClient.post<Guest>("/guests", data),
  update: (id: string, data: GuestRequest) =>
    apiClient.put<Guest>(`/guests/${id}`, data),
  remove: (id: string) => apiClient.delete(`/guests/${id}`),

  // Stats & extras
  getStats: () => apiClient.get<GuestStats>("/guests/stats"),
  exportCsv: () =>
    apiClient.get<string>("/guests/export", { responseType: "blob" }),
  importCsv: (csv: string) =>
    apiClient.post<{ imported: number }>("/guests/import", csv, {
      headers: { "Content-Type": "text/plain" },
    }),

  // Invite link
  /** Generate (or re-use) invite token for a guest. */
  generateInviteLink: (id: string) =>
    apiClient.post<{ token: string }>(`/guests/${id}/invite-link`),
  /** Public: opens the invite link — marks the guest as having viewed it and returns prefill data. */
  openInviteLink: (token: string) =>
    apiClient.get<Guest>(`/guests/invite/${token}`),
};
