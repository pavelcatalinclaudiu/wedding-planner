<script setup lang="ts">
import { ref, onMounted } from "vue";
import { messagesApi } from "@/api/messages.api";
import { useMessagesStore } from "@/stores/messages.store";
import type { Thread } from "@/types/message.types";
import GroupChat from "@/components/messaging/GroupChat.vue";

const messagesStore = useMessagesStore();
const thread = ref<Thread | null>(null);
const loading = ref(true);
const error = ref("");

onMounted(async () => {
  try {
    const res = await messagesApi.getGroupChat();
    // For a couple the backend returns a single Thread object
    const data = res.data as Thread;
    thread.value = data;
    // Merge into the store so GroupChat.vue can resolve the thread metadata
    const existing = messagesStore.threads.find((t) => t.id === data.id);
    if (!existing) {
      messagesStore.threads.push(data);
    } else {
      Object.assign(existing, data);
    }
  } catch (e) {
    error.value = "Could not load group chat.";
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="group-chat-view">
    <h2>Group Chat — Planning Team</h2>
    <p class="subtitle">
      All your vendors and planning team in one conversation.
    </p>

    <div class="chat-wrapper">
      <div v-if="loading" class="empty">Loading…</div>
      <div v-else-if="error" class="empty error">{{ error }}</div>
      <GroupChat v-else-if="thread" :thread-id="thread.id" />
      <div v-else class="empty">
        Group chat will appear once you have confirmed vendors.
      </div>
    </div>
  </div>
</template>

<style scoped>
.group-chat-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 116px);
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.subtitle {
  color: var(--color-muted);
  margin: 0 0 20px;
  font-size: 0.9rem;
}
.chat-wrapper {
  flex: 1;
  background: var(--color-white);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.empty {
  padding: 48px;
  text-align: center;
  color: var(--color-muted);
}
.error {
  color: var(--color-error);
}
</style>
