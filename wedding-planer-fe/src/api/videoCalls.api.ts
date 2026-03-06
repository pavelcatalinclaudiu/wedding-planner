import apiClient from "./client";
import type { VideoCall, BlockedDate } from "@/types/lead.types";

export const videoCallsApi = {
  list: () => apiClient.get<VideoCall[]>("/video-calls"),
  getByLead: (leadId: string) =>
    apiClient.get<VideoCall[]>(`/video-calls/lead/${leadId}`),
  getActiveForLead: (leadId: string) =>
    apiClient.get<VideoCall>(`/video-calls/lead/${leadId}/active`),
  schedule: (leadId: string, scheduledAt: string, postCallAction?: string) =>
    apiClient.post<VideoCall>(`/video-calls/lead/${leadId}/schedule`, null, {
      params: { scheduledAt, postCallAction },
    }),
  accept: (id: string) =>
    apiClient.post<VideoCall>(`/video-calls/${id}/accept`),
  start: (id: string) => apiClient.post<VideoCall>(`/video-calls/${id}/start`),
  end: (id: string) => apiClient.post<VideoCall>(`/video-calls/${id}/end`),
  cancel: (id: string) =>
    apiClient.patch<VideoCall>(`/video-calls/${id}/cancel`),
  reschedule: (id: string, scheduledAt: string) =>
    apiClient.patch<VideoCall>(`/video-calls/${id}/reschedule`, null, {
      params: { scheduledAt },
    }),
  setPostCallAction: (id: string, action: string) =>
    apiClient.patch<VideoCall>(`/video-calls/${id}/post-call-action`, null, {
      params: { action },
    }),
  getBlockedDates: (vendorId: string) =>
    apiClient.get<BlockedDate[]>(`/vendors/${vendorId}/availability`),
};
