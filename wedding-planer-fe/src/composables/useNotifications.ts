import { computed } from "vue";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useWebSocket } from "./useWebSocket";
import type { Notification } from "@/types/notification.types";

export function useNotifications() {
  const store = useNotificationsStore();
  const { on } = useWebSocket();

  on("notification.new", (payload) => {
    store.pushNotification(payload as unknown as Notification);
  });

  return {
    notifications: computed(() => store.notifications),
    unreadCount: computed(() => store.unreadCount),
    markRead: store.markRead.bind(store),
    markAllRead: store.markAllRead.bind(store),
    fetchNotifications: store.fetchNotifications.bind(store),
  };
}
