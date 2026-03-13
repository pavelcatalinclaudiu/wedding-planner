<script setup lang="ts">
import { ref, watch, onMounted, computed } from "vue";
import { useRoute } from "vue-router";
import { useI18n } from "vue-i18n";
import { useMessagesStore } from "@/stores/messages.store";
import type { Thread } from "@/types/message.types";
import { ChevronLeft } from "lucide-vue-next";
import ThreadList from "@/components/messaging/ThreadList.vue";
import MessageWindow from "@/components/messaging/MessageWindow.vue";

const { t } = useI18n();

const route = useRoute();
const messagesStore = useMessagesStore();
const selectedThreadId = ref<string | null>(null);
const showDetail = ref(false);

const selectedThread = computed<Thread | undefined>(() =>
  messagesStore.threads.find((t) => t.id === selectedThreadId.value),
);

function selectThread(id: string) {
  selectedThreadId.value = id;
  showDetail.value = true;
}

function closeDetail() {
  showDetail.value = false;
}

onMounted(async () => {
  await messagesStore.fetchThreads();

  // Auto-select via ?thread=<threadId> or ?dealId=<dealId>
  const threadParam = route.query.thread as string | undefined;
  const dealParam = route.query.dealId as string | undefined;

  if (threadParam) {
    selectedThreadId.value = threadParam;
    showDetail.value = true;
  } else if (dealParam) {
    const match = messagesStore.threads.find((t) => t.dealId === dealParam);
    if (match) {
      selectedThreadId.value = match.id;
      showDetail.value = true;
    }
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
        @select="selectThread"
      />
    </div>
    <div class="message-panel" :class="{ 'detail-visible': showDetail }">
      <button v-if="showDetail" class="mobile-back" @click="closeDetail">
        <ChevronLeft :size="18" /> {{ t("messaging.allConversations") }}
      </button>
      <MessageWindow
        v-if="selectedThreadId"
        :thread-id="selectedThreadId"
        :deal-id="selectedThread?.dealId"
        :deal-status="selectedThread?.dealStatus"
        :couple-wedding-date="selectedThread?.coupleWeddingDate"
        :couple-location="selectedThread?.coupleLocation"
      />
      <div v-else class="no-thread">
        {{ t("messaging.selectConversation") }}
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

/* ─── Mobile back button (hidden on desktop) ─── */
.mobile-back {
  display: none;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--color-gold);
  cursor: pointer;
  padding: 10px 16px;
  flex-shrink: 0;
  width: 100%;
}

@media (max-width: 768px) {
  .messages-view {
    flex-direction: column;
    position: relative;
    overflow: hidden;
    border-radius: 0;
  }
  .threads-panel {
    width: 100%;
    flex: 1;
    border-right: none;
  }
  .message-panel {
    position: absolute;
    inset: 0;
    background: var(--color-white);
    transform: translateX(100%);
    transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
    z-index: 10;
  }
  .message-panel.detail-visible {
    transform: translateX(0);
  }
  .mobile-back {
    display: flex;
  }
}
</style>
