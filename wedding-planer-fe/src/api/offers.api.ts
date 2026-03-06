import apiClient from "./client";
import type { Offer, CreateOfferPayload } from "@/types/offer.types";

export const offersApi = {
  /** Vendor: Create an offer for a lead */
  create: (payload: CreateOfferPayload) =>
    apiClient.post<Offer>("/offers", payload),

  /** Either: List offers for a lead */
  listByLead: (leadId: string) =>
    apiClient.get<Offer[]>("/offers", { params: { leadId } }),

  /** Couple: Accept an offer → booking created */
  accept: (id: string) => apiClient.patch<Offer>(`/offers/${id}/accept`),

  /** Couple: Decline an offer */
  decline: (id: string) => apiClient.patch<Offer>(`/offers/${id}/decline`),

  /** Couple: Request a revision — marks offer REVISED, unlocks vendor to re-send */
  requestRevision: (id: string) =>
    apiClient.patch<Offer>(`/offers/${id}/request-revision`),
};
