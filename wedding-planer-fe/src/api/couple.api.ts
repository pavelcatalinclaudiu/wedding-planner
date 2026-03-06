import apiClient from "./client";
import type {
  CoupleProfile,
  CoupleProfileRequest,
  ChecklistItem,
  BudgetItem,
  Guest,
  SeatingLayout,
  WeddingWebsite,
} from "@/types/couple.types";

export const coupleApi = {
  getProfile: () => apiClient.get<CoupleProfile>("/couples/me"),

  createProfile: (data: CoupleProfileRequest) =>
    apiClient.post<CoupleProfile>("/couples", data),

  updateProfile: (data: Partial<CoupleProfileRequest>) =>
    apiClient.put<CoupleProfile>("/couples/me", data),

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
  updateWebsite: (data: Partial<WeddingWebsite>) =>
    apiClient.put<WeddingWebsite>("/couples/me/website", data),
  getPublicWebsite: (slug: string) =>
    apiClient.get<WeddingWebsite>(`/couples/website/${slug}`),

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
