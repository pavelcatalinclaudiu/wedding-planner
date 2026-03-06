import apiClient from "./client";
import type {
  Thread,
  ThreadParticipant,
  Message,
  SendMessageRequest,
} from "@/types/message.types";

export const messagesApi = {
  // ─── Direct threads ──────────────────────────────────────────────────────
  getThreads: () => apiClient.get<Thread[]>("/messages/threads"),
  getThread: (id: string) => apiClient.get<Thread>(`/messages/threads/${id}`),
  getMessages: (threadId: string) =>
    apiClient.get<Message[]>(`/messages/threads/${threadId}`),
  sendMessage: (threadId: string, data: SendMessageRequest) =>
    apiClient.post<Message>(`/messages/threads/${threadId}/send`, data),
  markRead: (threadId: string) =>
    apiClient.post(`/messages/threads/${threadId}/read`, {}),
  startThread: (dealId: string) =>
    apiClient.get<Thread>(`/messages/threads/lead/${dealId}`),

  // ─── Group chat ──────────────────────────────────────────────────────────
  /** COUPLE → returns a single Thread; VENDOR → returns Thread[] */
  getGroupChat: () => apiClient.get<Thread | Thread[]>("/messages/group"),

  getParticipants: (threadId: string) =>
    apiClient.get<ThreadParticipant[]>(
      `/messages/threads/${threadId}/participants`,
    ),

  /** Couple removes a participant (participantId = ThreadParticipant row id) */
  removeParticipant: (threadId: string, participantId: string) =>
    apiClient.delete(
      `/messages/threads/${threadId}/participants/${participantId}`,
    ),

  /** Vendor leaves a group thread */
  leaveThread: (threadId: string) =>
    apiClient.post(`/messages/threads/${threadId}/leave`, {}),
};
