<script setup lang="ts">
import { useRouter } from "vue-router";
import type { Component } from "vue";
import type { Notification } from "@/types/notification.types";
import { useNotificationsStore } from "@/stores/notifications.store";
import { useAuthStore } from "@/stores/auth.store";
import {
  ClipboardList,
  CheckCircle,
  XCircle,
  Mail,
  Sparkles,
  Phone,
  Video,
  RefreshCw,
  Star,
  Bell,
} from "lucide-vue-next";

const props = defineProps<{ notification: Notification }>();
const emit = defineEmits<{ close: [] }>();
const notificationsStore = useNotificationsStore();
const authStore = useAuthStore();
const router = useRouter();

function formatTime(date: string) {
  const d = new Date(date);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  if (diffMins < 1) return "Just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  const diffHrs = Math.floor(diffMins / 60);
  if (diffHrs < 24) return `${diffHrs}h ago`;
  return d.toLocaleDateString();
}

const iconMap: Record<string, string> = {
  NEW_LEAD: "\uD83D\uDCCB",
  LEAD_ACCEPTED: "\u2705",
  LEAD_DECLINED: "\u274C",
  NEW_OFFER: "\uD83D\uDC8C",
  OFFER_ACCEPTED: "\uD83C\uDF89",
  NEW_MESSAGE: "\u2709\uFE0F",
  call_reminder: "\uD83D\uDCDE",
  VIDEO_CALL_PROPOSED: "\uD83D\uDCF9",
  VIDEO_CALL_RESCHEDULED: "\uD83D\uDD04",
  VIDEO_CALL_ACCEPTED: "\u2705",
  VIDEO_CALL_CANCELLED: "\u274C",
  review_request: "\u2B50",
  // Booking
  BOOKING_CANCELLED: "\uD83D\uDEAB",
  BOOKING_RESCHEDULE_PROPOSED: "\uD83D\uDCC5",
  BOOKING_RESCHEDULE_ACCEPTED: "\u2705",
  BOOKING_RESCHEDULE_DECLINED: "\u274C",
};

function resolveRoute(): string | null {
  const { type, entityId } = props.notification;
  const role = authStore.user?.role;
  const id = entityId;

  switch (type) {
    // Sent to the vendor
    case "NEW_LEAD":
      return id ? `/vendor/leads?lead=${id}` : "/vendor/leads";
    case "OFFER_REVISION_REQUESTED":
      return id ? `/vendor/leads?lead=${id}` : "/vendor/leads";
    case "OFFER_ACCEPTED":
      return "/vendor/leads";
    // Sent to the couple
    case "LEAD_ACCEPTED":
    case "LEAD_DECLINED":
      return id ? `/couple/enquiries?lead=${id}` : "/couple/enquiries";
    case "NEW_OFFER":
      return "/couple/enquiries";
    // Messages — entityId is the conversationId
    case "NEW_MESSAGE":
      if (role === "COUPLE")
        return id
          ? `/couple/enquiries?conversation=${id}`
          : "/couple/enquiries";
      return id ? `/vendor/leads?conversation=${id}` : "/vendor/leads";
    // Calls
    case "call_reminder":
    case "VIDEO_CALL_PROPOSED":
    case "VIDEO_CALL_RESCHEDULED":
    case "VIDEO_CALL_ACCEPTED":
    case "VIDEO_CALL_CANCELLED":
      return role === "COUPLE" ? "/couple/calls" : "/vendor/calls";
    // Reviews
    case "review_request":
      return "/vendor/reviews";
    // Bookings — couple receives PROPOSED / CANCELLED, vendor receives ACCEPTED / DECLINED
    case "BOOKING_RESCHEDULE_PROPOSED":
    case "BOOKING_CANCELLED":
      return "/couple/calendar";
    case "BOOKING_RESCHEDULE_ACCEPTED":
    case "BOOKING_RESCHEDULE_DECLINED":
      return "/vendor/calendar";
    default:
      return null;
  }
}

function handleClick() {
  if (!props.notification.read) {
    notificationsStore.markRead(props.notification.id);
  }
  const route = resolveRoute();
  if (route) {
    router.push(route);
  }
  // Close the dropdown explicitly — don't rely on event fallthrough
  emit("close");
}
</script>

<template>
  <div
    class="notification-item"
    :class="{ unread: !notification.read }"
    @click="handleClick"
  >
    <div class="notif-icon">
      {{ iconMap[notification.type] ?? "\uD83D\uDD14" }}
    </div>
    <div class="notif-body">
      <p class="notif-title">{{ notification.title }}</p>
      <p v-if="notification.body" class="notif-message">
        {{ notification.body }}
      </p>
      <span class="notif-time">{{ formatTime(notification.createdAt) }}</span>
    </div>
    <div v-if="!notification.read" class="unread-dot"></div>
  </div>
</template>

<style scoped>
.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--color-border);
}
.notification-item:last-child {
  border-bottom: none;
}
.notification-item:hover {
  background: var(--color-surface);
}
.notification-item.unread {
  background: var(--color-gold-light, #fdf8ee);
}
.notif-icon {
  font-size: 1.1rem;
  flex-shrink: 0;
  margin-top: 2px;
}
.notif-body {
  flex: 1;
}
.notif-title {
  margin: 0 0 2px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.3;
}
.notif-message {
  margin: 0 0 3px;
  font-size: 0.8rem;
  color: var(--color-muted);
  line-height: 1.4;
}
.notif-time {
  font-size: 0.75rem;
  color: var(--color-muted);
}
.unread-dot {
  width: 8px;
  height: 8px;
  background: var(--color-gold);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
