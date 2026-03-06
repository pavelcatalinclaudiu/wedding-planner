import apiClient from "./client";
import type { Notification } from "@/types/notification.types";

export const notificationsApi = {
  list: () => apiClient.get<Notification[]>("/notifications"),
  markRead: (id: string) => apiClient.post(`/notifications/${id}/read`, {}),
  markAllRead: () => apiClient.post("/notifications/read-all", {}),
};
