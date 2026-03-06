export interface ThreadParticipant {
  id: string;
  userId: string;
  name: string;
  role: string; // "COUPLE" | "VENDOR"
  avatarUrl?: string;
}

export interface Thread {
  id: string;
  threadType?: "DIRECT" | "GROUP";
  name?: string;
  participantName?: string; // backward-compat alias
  participantCount?: number;
  participants?: ThreadParticipant[];
  lastMessage?: string;
  lastMessageAt?: string;
  unreadCount: number;
  createdAt: string;
  // Deal context — populated when the thread is linked to a deal
  dealId?: string;
  dealStatus?: string;
  coupleWeddingDate?: string;
  coupleLocation?: string;
}

export interface MessageSender {
  id: string;
  email: string;
  role: string;
}

export interface QuotePayload {
  quoteId: string;
  packageName: string;
  totalPrice: number;
  depositAmount?: number;
  items: Array<{
    name: string;
    description?: string;
    quantity: number;
    unitPrice: number;
    total: number;
  }>;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED";
  expiresAt?: string;
}

export interface Message {
  id: string;
  threadId: string;
  sender?: MessageSender;
  content: string;
  type: "TEXT" | "QUOTE" | "SYSTEM" | "IMAGE" | "FILE";
  readBy?: string[];
  createdAt: string;
}

export interface SendMessageRequest {
  content: string;
  type?: "TEXT";
}
