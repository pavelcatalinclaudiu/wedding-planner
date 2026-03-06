<script setup lang="ts">
import { onMounted, onUnmounted } from "vue";
import { useNotificationsStore } from "@/stores/notifications.store";
import NotificationItem from "./NotificationItem.vue";

const emit = defineEmits<{ close: [] }>();
const notificationsStore = useNotificationsStore();

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement;
  if (
    !target.closest(".notification-dropdown") &&
    !target.closest(".bell-btn")
  ) {
    emit("close");
  }
}

onMounted(() => document.addEventListener("mousedown", handleClickOutside));
onUnmounted(() =>
  document.removeEventListener("mousedown", handleClickOutside),
);
</script>

<template>
  <div class="notification-dropdown">
    <div class="dropdown-header">
      <span class="header-title">Notifications</span>
      <button
        v-if="notificationsStore.unreadCount > 0"
        class="mark-all-btn"
        @click="notificationsStore.markAllRead()"
      >
        Mark all read
      </button>
    </div>
    <div class="dropdown-body">
      <div
        v-if="notificationsStore.notifications.length === 0"
        class="empty-state"
      >
        No notifications
      </div>
      <NotificationItem
        v-for="n in notificationsStore.notifications.slice(0, 30)"
        :key="n.id"
        :notification="n"
        @close="emit('close')"
      />
    </div>
  </div>
</template>

<style scoped>
.notification-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 340px;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  z-index: 200;
  overflow: hidden;
}
.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border);
}
.header-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
}
.mark-all-btn {
  font-size: 0.8rem;
  color: var(--color-gold);
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 600;
}
.mark-all-btn:hover {
  text-decoration: underline;
}
.dropdown-body {
  max-height: 400px;
  overflow-y: auto;
}
.empty-state {
  padding: 32px;
  text-align: center;
  color: var(--color-muted);
  font-size: 0.9rem;
}
</style>
