export interface Conversation {
  id: string;
  leadId: string;
  coupleId: string;
  coupleName: string;
  vendorId: string;
  vendorName: string;
  createdAt: string;
  lastMessageAt: string;
}

export interface ConversationMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderRole: "COUPLE" | "VENDOR";
  content: string;
  type: "TEXT" | "OFFER_REVISION_REQUEST";
  sentAt: string;
}
