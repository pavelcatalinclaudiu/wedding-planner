<script setup lang="ts">
import { ref, nextTick, watch } from "vue";
import type { ConversationMessage } from "@/types/conversation.types";

const props = defineProps<{
  messages: ConversationMessage[];
  myRole: "COUPLE" | "VENDOR";
  sending: boolean;
}>();

const emit = defineEmits<{
  (e: "send", content: string): void;
}>();

const text = ref("");
const chatEnd = ref<HTMLElement | null>(null);

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    chatEnd.value?.scrollIntoView({ behavior: "smooth" });
  },
);

function submit() {
  const content = text.value.trim();
  if (!content) return;
  emit("send", content);
  text.value = "";
}
</script>

<template>
  <div class="thread-wrap">
    <div class="message-list">
      <template v-for="msg in messages" :key="msg.id">
        <!-- System event: offer revision request -->
        <div v-if="msg.type === 'OFFER_REVISION_REQUEST'" class="system-event">
          <span class="system-event-icon">🔄</span>
          <span class="system-event-text">Revision requested</span>
          <time class="system-event-time">{{
            new Date(msg.sentAt).toLocaleTimeString([], {
              hour: "2-digit",
              minute: "2-digit",
            })
          }}</time>
        </div>

        <!-- Regular chat bubble -->
        <div
          v-else
          class="bubble-row"
          :class="msg.senderRole === myRole ? 'mine' : 'theirs'"
        >
          <div class="bubble">
            <p class="msg-content">{{ msg.content }}</p>
            <time class="msg-time">{{
              new Date(msg.sentAt).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit",
              })
            }}</time>
          </div>
        </div>
      </template>
      <div ref="chatEnd" />
    </div>

    <form class="compose" @submit.prevent="submit">
      <input
        v-model="text"
        placeholder="Type a message…"
        :disabled="sending"
        class="compose-input"
      />
      <button
        type="submit"
        :disabled="sending || !text.trim()"
        class="send-btn"
      >
        Send
      </button>
    </form>
  </div>
</template>

<style scoped>
.thread-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bubble-row {
  display: flex;
}
.bubble-row.mine {
  justify-content: flex-end;
}
.bubble-row.theirs {
  justify-content: flex-start;
}

.bubble {
  max-width: 70%;
  background: var(--color-surface);
  border-radius: 14px;
  padding: 8px 14px;
  border: 1px solid var(--color-border);
}
.bubble-row.mine .bubble {
  background: var(--color-gold);
  color: #fff;
  border-color: var(--color-gold);
}

.msg-content {
  margin: 0 0 2px;
  font-size: 0.9rem;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-time {
  font-size: 0.7rem;
  opacity: 0.65;
}

.compose {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid var(--color-border);
}
.compose-input {
  flex: 1;
  padding: 8px 12px;
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  font-size: 0.9rem;
  outline: none;
}
.compose-input:focus {
  border-color: var(--color-gold);
}
.send-btn {
  padding: 8px 18px;
  background: var(--color-gold);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
}
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* System event row (e.g. offer revision request) */
.system-event {
  display: flex;
  align-items: center;
  gap: 6px;
  align-self: center;
  background: var(--color-surface, #f9f5f0);
  border: 1px solid var(--color-border, #e8ddd3);
  border-radius: 20px;
  padding: 5px 14px;
  font-size: 0.8rem;
  color: var(--color-text-muted, #888);
  margin: 4px auto;
}
.system-event-icon {
  font-size: 0.95rem;
}
.system-event-text {
  font-weight: 500;
  color: var(--color-text, #333);
}
.system-event-time {
  font-size: 0.7rem;
  opacity: 0.6;
}
</style>
