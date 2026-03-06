<script setup lang="ts">
import { ref, onMounted, watch, computed } from "vue";
import { useRoute } from "vue-router";
import { useMessagesStore } from "@/stores/messages.store";
import type { Thread } from "@/types/message.types";
import ThreadList from "@/components/messaging/ThreadList.vue";
import MessageWindow from "@/components/messaging/MessageWindow.vue";

const route = useRoute();
const messagesStore = useMessagesStore();
const selectedThreadId = ref<string | null>(null);

const selectedThread = computed<Thread | undefined>(() =>
  messagesStore.threads.find((t) => t.id === selectedThreadId.value),
);

onMounted(async () => {
  await messagesStore.fetchThreads();

  const threadParam = route.query.thread as string | undefined;
  const dealParam = route.query.dealId as string | undefined;

  if (threadParam) {
    selectedThreadId.value = threadParam;
  } else if (dealParam) {
    const match = messagesStore.threads.find((t) => t.dealId === dealParam);
    if (match) selectedThreadId.value = match.id;
  } else if (messagesStore.threads.length > 0) {
    selectedThreadId.value = messagesStore.threads[0].id;
  }
});

watch(selectedThreadId, async (id) => {
  if (id) await messagesStore.openThread(id);
});
</script>

<template>
  <div class="messages-view">
    <div class="threads-panel">
      <ThreadList
        :threads="messagesStore.threads"
        :selected-id="selectedThreadId"
        @select="(id: string) => (selectedThreadId = id)"
      />
    </div>
    <div class="message-panel">
      <MessageWindow
        v-if="selectedThreadId"
        :thread-id="selectedThreadId"
        :deal-id="selectedThread?.dealId"
        :deal-status="selectedThread?.dealStatus"
        :couple-wedding-date="selectedThread?.coupleWeddingDate"
        :couple-location="selectedThread?.coupleLocation"
      />
      <div v-else class="no-thread">
        <p>Select a conversation to read messages</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.messages-view {
  display: flex;
  height: calc(100vh - 116px);
  background: var(--color-white);
  border-radius: 14px;
  border: 1px solid var(--color-border);
  overflow: hidden;
}
.threads-panel {
  width: 280px;
  border-right: 1px solid var(--color-border);
  flex-shrink: 0;
  overflow-y: auto;
}
.message-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.no-thread {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted);
  font-size: 0.9rem;
}
</style>
