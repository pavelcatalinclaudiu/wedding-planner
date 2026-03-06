import apiClient from "./client";
import type {
  Conversation,
  ConversationMessage,
} from "@/types/conversation.types";

export const conversationsApi = {
  /** Get the conversation linked to a specific lead */
  getByLead: (leadId: string) =>
    apiClient.get<Conversation>(`/conversations/lead/${leadId}`),

  /** List all my conversations (as couple or vendor) */
  listMine: () => apiClient.get<Conversation[]>("/conversations/my"),

  /** Get all messages in a conversation */
  getMessages: (conversationId: string) =>
    apiClient.get<ConversationMessage[]>(
      `/conversations/${conversationId}/messages`,
    ),

  /** Send a message */
  sendMessage: (conversationId: string, content: string) =>
    apiClient.post<ConversationMessage>(
      `/conversations/${conversationId}/messages`,
      { content },
    ),
};
