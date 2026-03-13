import apiClient from "./client";
import type {
  CoupleProfile,
  CoupleProfileRequest,
  ChecklistItem,
  BudgetItem,
  Guest,
  SeatingLayout,
  WeddingWebsite,
  PublicWeddingWebsite,
  RsvpRequest,
} from "@/types/couple.types";

export const coupleApi = {
  getProfile: () => apiClient.get<CoupleProfile>("/couples/me"),

  createProfile: (data: CoupleProfileRequest) =>
    apiClient.post<CoupleProfile>("/couples", data),

  updateProfile: (data: Partial<CoupleProfileRequest>) =>
    apiClient.put<CoupleProfile>("/couples/me", data),

  uploadPicture: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiClient.post<{ profilePicture: string }>(
      "/couples/me/picture",
      form,
      {
        headers: { "Content-Type": "multipart/form-data" },
      },
    );
  },

  deletePicture: () => apiClient.delete("/couples/me/picture"),

  // Checklist
  getChecklist: () => apiClient.get<ChecklistItem[]>("/checklist"),
  toggleChecklistItem: (id: string) =>
    apiClient.post<ChecklistItem>(`/checklist/${id}/toggle`, {}),
  addChecklistItem: (label: string, dueDate?: string, timePeriod?: string) =>
    apiClient.post<ChecklistItem>("/checklist", {
      label,
      dueDate,
      timePeriod,
    }),
  deleteChecklistItem: (id: string) => apiClient.delete(`/checklist/${id}`),

  // Budget
  getBudget: () => apiClient.get<BudgetItem[]>("/budget"),
  addBudgetItem: (data: Partial<BudgetItem>) =>
    apiClient.post<BudgetItem>("/budget", data),
  updateBudgetItem: (id: string, data: Partial<BudgetItem>) =>
    apiClient.put<BudgetItem>(`/budget/${id}`, data),
  deleteBudgetItem: (id: string) => apiClient.delete(`/budget/${id}`),

  // Guests
  getGuests: () => apiClient.get<Guest[]>("/guests"),
  addGuest: (data: Partial<Guest>) => apiClient.post<Guest>("/guests", data),
  deleteGuest: (id: string) => apiClient.delete(`/guests/${id}`),
  submitRsvp: (guestId: string, status: string) =>
    apiClient.patch(`/guests/${guestId}/rsvp`, null, { params: { status } }),

  // Seating
  getSeating: () => apiClient.get<SeatingLayout>("/couples/me/seating"),
  updateSeating: (layout: SeatingLayout) =>
    apiClient.put<SeatingLayout>("/couples/me/seating", layout),

  // Website
  getWebsite: () => apiClient.get<WeddingWebsite>("/couples/me/website"),
  updateWebsite: (data: Partial<WeddingWebsite> & { subdomain?: string }) =>
    apiClient.put<WeddingWebsite>("/couples/me/website", data),
  uploadCoverPhoto: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiClient.post<WeddingWebsite>("/couples/me/website/photo", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  previewWebsite: () =>
    apiClient.get<PublicWeddingWebsite>("/couples/me/website/preview"),
  getPublicWebsite: (subdomain: string) =>
    apiClient.get<PublicWeddingWebsite>(`/couples/website/${subdomain}`),
  submitPublicRsvp: (subdomain: string, data: RsvpRequest) =>
    apiClient.post(`/couples/website/${subdomain}/rsvp`, data),

  trackVisit: (subdomain: string) =>
    apiClient.post(`/couples/website/${subdomain}/visit`, {}),

  // Documents
  getDocuments: () => apiClient.get("/couples/me/documents"),
  uploadDocument: (file: File, category?: string) => {
    const form = new FormData();
    form.append("file", file);
    if (category) form.append("category", category);
    return apiClient.post("/couples/me/documents", form, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  deleteDocument: (id: string) =>
    apiClient.delete(`/couples/me/documents/${id}`),
};
