import apiClient from "./client";

export interface Review {
  id: string;
  bookingId: string;
  coupleId: string;
  coupleName: string;
  coupleProfilePicture?: string;
  vendorId: string;
  rating: number;
  comment?: string;
  vendorReply?: string;
  isPublic: boolean;
  createdAt: string;
}

export const reviewsApi = {
  list: () => apiClient.get<Review[]>("/reviews"),
  getForVendor: (vendorId: string) =>
    apiClient.get<Review[]>(`/reviews/vendor/${vendorId}`),
  reply: (id: string, reply: string) =>
    apiClient.patch(`/reviews/${id}/reply`, { reply }),
  create: (bookingId: string, rating: number, comment: string) =>
    apiClient.post<Review>(`/reviews/booking/${bookingId}`, {
      rating,
      comment,
    }),
};
