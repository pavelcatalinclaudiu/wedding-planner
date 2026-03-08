export type OfferStatus = "PENDING" | "ACCEPTED" | "DECLINED" | "REVISED";

export interface Offer {
  id: string;
  leadId: string;
  conversationId?: string;
  vendorId: string;
  vendorName: string;
  packageDetails?: string;
  price: number;
  expiresAt?: string;
  status: OfferStatus;
  viewedAt?: string;
  createdAt: string;
}

export interface CreateOfferPayload {
  leadId: string;
  conversationId?: string;
  packageDetails?: string;
  price: number;
  expiresAt?: string;
}
