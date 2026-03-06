<script setup lang="ts">
import type { Message } from "@/types/message.types";
import { useAuthStore } from "@/stores/auth.store";
import QuoteMessageCard from "./QuoteMessageCard.vue";

defineProps<{ message: Message; dealStatus?: string }>();
const emit = defineEmits<{ (e: "refresh"): void }>();
const authStore = useAuthStore();
const isOwn = (msg: Message) => msg.sender?.id === authStore.user?.id;

function formatTime(date: string) {
  return new Date(date).toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });
}
</script>

<template>
  <!-- SYSTEM messages → centred notice strip -->
  <div v-if="message.type === 'SYSTEM'" class="system-msg">
    <span class="system-text">{{ message.content }}</span>
    <span class="system-time">{{ formatTime(message.createdAt) }}</span>
  </div>

  <!-- QUOTE messages → rich card (not owned-bubble style) -->
  <div
    v-else-if="message.type === 'QUOTE'"
    class="quote-row"
    :class="{ own: isOwn(message) }"
  >
    <QuoteMessageCard
      :content="message.content"
      :deal-status="dealStatus"
      @refresh="emit('refresh')"
    />
    <span class="bubble-time quote-time">{{
      formatTime(message.createdAt)
    }}</span>
  </div>

  <!-- TEXT / FILE / IMAGE → normal chat bubble -->
  <div v-else class="bubble-wrapper" :class="{ own: isOwn(message) }">
    <div class="bubble" :class="{ own: isOwn(message) }">
      <p class="bubble-text">{{ message.content }}</p>
      <span class="bubble-time">{{ formatTime(message.createdAt) }}</span>
    </div>
  </div>
</template>

<style scoped>
/* ─── System message ─── */
.system-msg {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  margin: 10px 0;
}
.system-text {
  background: var(--color-surface);
  border-radius: 20px;
  padding: 6px 16px;
  font-size: 0.78rem;
  color: var(--color-muted);
  text-align: center;
  white-space: pre-line;
}
.system-time {
  font-size: 0.68rem;
  color: var(--color-muted);
}

/* ─── Quote card row ─── */
.quote-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 8px;
}
.quote-row.own {
  align-items: flex-end;
}
.quote-time {
  margin-top: 4px;
  font-size: 0.7rem;
  color: var(--color-muted);
}

/* ─── Normal bubble ─── */
.bubble-wrapper {
  display: flex;
  margin-bottom: 8px;
}
.bubble-wrapper.own {
  justify-content: flex-end;
}
.bubble {
  max-width: 68%;
  background: var(--color-surface);
  border-radius: 14px 14px 14px 3px;
  padding: 10px 14px;
}
.bubble.own {
  background: var(--color-gold);
  border-radius: 14px 14px 3px 14px;
}
.bubble-text {
  margin: 0 0 4px;
  font-size: 0.88rem;
  line-height: 1.45;
  color: var(--color-text);
  word-break: break-word;
}
.bubble.own .bubble-text {
  color: #fff;
}
.bubble-time {
  font-size: 0.7rem;
  color: var(--color-muted);
}
.bubble.own .bubble-time {
  color: rgba(255, 255, 255, 0.7);
}
</style>
