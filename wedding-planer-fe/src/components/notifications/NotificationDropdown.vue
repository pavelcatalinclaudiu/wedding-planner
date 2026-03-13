<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { isToday, isYesterday, format } from "date-fns";
import { useNotificationsStore } from "@/stores/notifications.store";
import NotificationItem from "./NotificationItem.vue";

const emit = defineEmits<{ close: [] }>();
const notificationsStore = useNotificationsStore();

// ── Group by day ───────────────────────────────────────────────────────────
type Group = { label: string; items: typeof notificationsStore.notifications };

const grouped = computed<Group[]>(() => {
  const map = new Map<string, Group>();
  const slice = notificationsStore.notifications.slice(0, 40);

  for (const n of slice) {
    const d = new Date(n.createdAt);
    let label: string;
    if (isToday(d)) label = "Today";
    else if (isYesterday(d)) label = "Yesterday";
    else label = format(d, "dd MMMM yyyy");

    if (!map.has(label)) map.set(label, { label, items: [] });
    map.get(label)!.items.push(n);
  }

  return Array.from(map.values());
});

// ── Close on Escape or outside click ──────────────────────────────────────
function handleKeyDown(e: KeyboardEvent) {
  if (e.key === "Escape") emit("close");
}
function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement;
  if (
    !target.closest(".notification-dropdown") &&
    !target.closest(".bell-btn")
  ) {
    emit("close");
  }
}

onMounted(() => {
  document.addEventListener("mousedown", handleClickOutside);
  document.addEventListener("keydown", handleKeyDown);
});
onUnmounted(() => {
  document.removeEventListener("mousedown", handleClickOutside);
  document.removeEventListener("keydown", handleKeyDown);
});
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
        class="empty-notif"
      >
        <span class="empty-notif-icon">🔔</span>
        <p class="empty-notif-text">You're all caught up!</p>
      </div>
      <template v-for="group in grouped" :key="group.label">
        <div class="day-separator">{{ group.label }}</div>
        <NotificationItem
          v-for="n in group.items"
          :key="n.id"
          :notification="n"
          @close="emit('close')"
        />
      </template>
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
  max-height: 420px;
  overflow-y: auto;
}
.day-separator {
  padding: 6px 16px 4px;
  font-size: 0.7rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--color-muted);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: 1;
}
.empty-notif {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 36px 24px;
  text-align: center;
}
.empty-notif-icon {
  font-size: 2rem;
  opacity: 0.5;
}
.empty-notif-text {
  margin: 0;
  font-size: 0.9rem;
  color: var(--color-muted);
}
</style>
