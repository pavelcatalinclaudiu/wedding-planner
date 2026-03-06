<script setup lang="ts">
import { ref } from "vue";
import { useNotificationsStore } from "@/stores/notifications.store";
import NotificationDropdown from "./NotificationDropdown.vue";

const notificationsStore = useNotificationsStore();
const open = ref(false);

function toggle() {
  open.value = !open.value;
  if (open.value) {
    notificationsStore.fetchNotifications();
  }
}

function close() {
  open.value = false;
}
</script>

<template>
  <div class="bell-wrapper">
    <button class="bell-btn" @click="toggle" :class="{ active: open }">
      <span class="bell-icon">🔔</span>
      <span v-if="notificationsStore.unreadCount > 0" class="bell-count">
        {{
          notificationsStore.unreadCount > 99
            ? "99+"
            : notificationsStore.unreadCount
        }}
      </span>
    </button>
    <NotificationDropdown v-if="open" @close="close" />
  </div>
</template>

<style scoped>
.bell-wrapper {
  position: relative;
}
.bell-btn {
  position: relative;
  background: none;
  border: none;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.bell-btn:hover,
.bell-btn.active {
  background: var(--color-surface);
}
.bell-icon {
  font-size: 1.1rem;
}
.bell-count {
  position: absolute;
  top: 2px;
  right: 2px;
  background: var(--color-error);
  color: #fff;
  border-radius: 10px;
  min-width: 16px;
  height: 16px;
  font-size: 0.65rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3px;
}
</style>
