import { defineStore } from "pinia";
import { ref } from "vue";
import { notificationsApi } from "@/api/notifications.api";
import type { Notification } from "@/types/notification.types";

export const useNotificationsStore = defineStore("notifications", () => {
  const notifications = ref<Notification[]>([]);
  const unreadCount = ref(0);

  async function fetchNotifications() {
    const res = await notificationsApi.list();
    notifications.value = res.data;
    unreadCount.value = res.data.filter((n) => !n.read).length;
  }

  async function markRead(id: string) {
    await notificationsApi.markRead(id);
    const idx = notifications.value.findIndex((n) => n.id === id);
    if (idx !== -1 && !notifications.value[idx].read) {
      notifications.value[idx] = { ...notifications.value[idx], read: true };
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  async function markAllRead() {
    await notificationsApi.markAllRead();
    notifications.value = notifications.value.map((n) => ({
      ...n,
      read: true,
    }));
    unreadCount.value = 0;
  }

  function pushNotification(notification: Notification) {
    notifications.value.unshift(notification);
    unreadCount.value++;
  }

  /** Called from the WebSocket handler — instantly bumps the badge without waiting for an HTTP round-trip. */
  function bumpUnread() {
    unreadCount.value++;
  }

  function reset() {
    notifications.value = [];
    unreadCount.value = 0;
  }

  return {
    notifications,
    unreadCount,
    fetchNotifications,
    markRead,
    markAllRead,
    pushNotification,
    bumpUnread,
    reset,
  };
});
