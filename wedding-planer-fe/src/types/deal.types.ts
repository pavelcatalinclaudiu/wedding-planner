export type DealStatus =
  | "ENQUIRY"
  | "CONVERSATION"
  | "QUOTE_SENT"
  | "QUOTE_ACCEPTED"
  | "DEPOSIT_PAID"
  | "CONFIRMED";

export interface Deal {
  id: string;
  coupleId: string;
  vendorId: string;
  coupleName: string;
  vendorName: string;
  vendorCategory: string;
  weddingDate?: string;
  coupleLocation?: string;
  status: DealStatus;
  sealedAt?: string;
  sealedTrigger?: string;
  videoCallId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Quote {
  id: string;
  dealId: string;
  packageName: string;
  totalPrice: number;
  depositAmount: number;
  items: QuoteItem[];
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "EXPIRED";
  expiresAt: string;
  acceptedAt?: string;
  createdAt: string;
}

export interface QuoteItem {
  name?: string;
  description?: string;
  quantity?: number;
  unitPrice?: number;
  total?: number;
  /** legacy field from old API */
  price?: number;
  id?: string;
}

export interface QuoteRequest {
  dealId: string;
  packageName: string;
  totalPrice: number;
  depositAmount?: number;
  items: QuoteItem[];
  expiresAt: string;
}

export interface Booking {
  id: string;
  dealId: string;
  quoteId?: string;
  weddingDate: string;
  totalPrice: number;
  depositPaid: number;
  depositPaidAt?: string;
  balanceDueAt?: string;
  contractUrl?: string;
  notes?: string;
  createdAt: string;
  hasReview: boolean;
}

export interface VideoCall {
  id: string;
  dealId: string;
  roomUrl: string;
  participantToken?: string;
  scheduledAt: string;
  scheduledDuration?: number;
  startedAt?: string;
  endedAt?: string;
  status: "SCHEDULED" | "ACTIVE" | "ENDED" | "CANCELLED";
  postCallAction?: "sealed" | "quote_sent" | "pending" | "no_action";
  deal?: Deal;
}

export interface WebSocketMessage {
  type: string;
  payload: Record<string, unknown>;
}
