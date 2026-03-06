export type LeadStatus =
  | "NEW"
  | "VIEWED"
  | "IN_DISCUSSION"
  | "QUOTED"
  | "BOOKED"
  | "DECLINED";

export interface Lead {
  id: string;
  coupleId: string;
  coupleName: string;
  vendorId: string;
  vendorName: string;
  vendorCategory: string;
  eventDate?: string;
  budget?: number;
  message?: string;
  status: LeadStatus;
  createdAt: string;
  updatedAt: string;
}

export interface Booking {
  id: string;
  leadId: string;
  offerId?: string;
  weddingDate?: string;
  totalPrice: number;
  depositPaid: number;
  depositPaidAt?: string;
  balanceDueAt?: string;
  contractUrl?: string;
  notes?: string;
  createdAt: string;
  hasReview: boolean;
  vendorName?: string;
  vendorCategory?: string;
}

export interface VideoCall {
  id: string;
  leadId: string;
  roomUrl: string;
  roomName: string;
  token?: string | null;
  scheduledAt: string;
  startedAt?: string;
  endedAt?: string;
  status: "PENDING" | "SCHEDULED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  postCallAction?: string;
  coupleName?: string;
  vendorName?: string;
  vendorId?: string;
  /** "COUPLE" or "VENDOR" — whoever proposed / last rescheduled this call */
  proposedBy?: "COUPLE" | "VENDOR";
}

export interface BlockedDate {
  id: string;
  vendorId: string;
  date: string; // "yyyy-MM-dd"
  reason?: string;
}
