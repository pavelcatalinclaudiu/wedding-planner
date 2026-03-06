<script setup lang="ts">
import type { Thread } from "@/types/message.types";
import { useMessagesStore } from "@/stores/messages.store";

defineProps<{ threads: Thread[]; selectedId: string | null }>();
const emit = defineEmits<{ select: [id: string] }>();
const messagesStore = useMessagesStore();

function formatTime(date: string) {
  const d = new Date(date);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  }
  return d.toLocaleDateString();
}
</script>

<template>
  <div class="thread-list">
    <div class="tl-header">
      <span>Messages</span>
      <span v-if="messagesStore.totalUnread() > 0" class="unread-count">{{
        messagesStore.totalUnread()
      }}</span>
    </div>
    <div v-if="threads.length === 0" class="empty">No conversations yet.</div>
    <div
      v-for="thread in threads"
      :key="thread.id"
      class="thread-item"
      :class="{ selected: selectedId === thread.id }"
      @click="emit('select', thread.id)"
    >
      <div class="thread-avatar">
        {{ thread.participantName?.[0]?.toUpperCase() ?? "?" }}
      </div>
      <div class="thread-info">
        <div class="thread-top">
          <span class="thread-name">{{ thread.participantName }}</span>
          <span class="thread-time">{{
            thread.lastMessageAt ? formatTime(thread.lastMessageAt) : ""
          }}</span>
        </div>
        <p class="thread-preview">
          {{ thread.lastMessage ?? "No messages yet" }}
        </p>
      </div>
      <div v-if="messagesStore.unreadCounts[thread.id]" class="unread-dot">
        {{ messagesStore.unreadCounts[thread.id] }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.thread-list {
  display: flex;
  flex-direction: column;
}
.tl-header {
  padding: 14px 16px;
  font-size: 0.85rem;
  font-weight: 700;
  color: var(--color-text);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.unread-count {
  background: var(--color-gold);
  color: #fff;
  border-radius: 10px;
  padding: 1px 7px;
  font-size: 0.72rem;
  font-weight: 700;
}
.empty {
  padding: 24px 16px;
  text-align: center;
  color: var(--color-muted);
  font-size: 0.85rem;
}
.thread-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid var(--color-border);
  transition: background 0.15s;
}
.thread-item:hover {
  background: var(--color-surface);
}
.thread-item.selected {
  background: var(--color-gold-light, #fdf8ee);
}
.thread-avatar {
  width: 36px;
  height: 36px;
  background: var(--color-gold);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.9rem;
  font-weight: 700;
  flex-shrink: 0;
}
.thread-info {
  flex: 1;
  min-width: 0;
}
.thread-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 3px;
}
.thread-name {
  font-size: 0.87rem;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.thread-time {
  font-size: 0.72rem;
  color: var(--color-muted);
  white-space: nowrap;
  flex-shrink: 0;
  margin-left: 6px;
}
.thread-preview {
  margin: 0;
  font-size: 0.8rem;
  color: var(--color-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-dot {
  background: var(--color-gold);
  color: #fff;
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 0.7rem;
  font-weight: 700;
  flex-shrink: 0;
}
</style>
