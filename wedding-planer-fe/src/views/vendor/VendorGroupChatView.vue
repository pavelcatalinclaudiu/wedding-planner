<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useI18n } from "vue-i18n";
import { messagesApi } from "@/api/messages.api";
import { useMessagesStore } from "@/stores/messages.store";
import type { Thread } from "@/types/message.types";
import GroupChat from "@/components/messaging/GroupChat.vue";

const { t } = useI18n();
const messagesStore = useMessagesStore();
const threads = ref<Thread[]>([]);
const activeThreadId = ref<string | null>(null);
const loading = ref(true);
const error = ref("");

onMounted(async () => {
  try {
    const res = await messagesApi.getGroupChat();
    // For vendors the backend returns an array
    const data = res.data as Thread[];
    threads.value = data;

    // Merge threads into the store
    data.forEach((t) => {
      const existing = messagesStore.threads.find((x) => x.id === t.id);
      if (!existing) {
        messagesStore.threads.push(t);
      } else {
        Object.assign(existing, t);
      }
    });

    // Auto-open the first (most recent) thread
    if (data.length > 0) {
      activeThreadId.value = data[0].id;
    }
  } catch (e) {
    error.value = t("messaging.groupChatError");
  } finally {
    loading.value = false;
  }
});

function selectThread(id: string) {
  activeThreadId.value = id;
}

function onLeft() {
  // Remove the thread from the list after vendor leaves
  threads.value = threads.value.filter((t) => t.id !== activeThreadId.value);
  activeThreadId.value = threads.value[0]?.id ?? null;
}

function formatDate(iso?: string) {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });
}
</script>

<template>
  <div class="vendor-group-chat">
    <div class="page-header">
      <h2>{{ t("messaging.groupChat") }}</h2>
      <p class="subtitle">{{ t("messaging.groupChatSubtitle") }}</p>
    </div>

    <div v-if="loading" class="state-msg">{{ t("common.loading") }}</div>
    <div v-else-if="error" class="state-msg error">{{ error }}</div>

    <div v-else class="chat-layout">
      <!-- ── Thread list ──────────────────────────────────────────── -->
      <aside class="thread-list">
        <button
          v-for="thread in threads"
          :key="thread.id"
          class="thread-item"
          :class="{ active: thread.id === activeThreadId }"
          @click="selectThread(thread.id)"
        >
          <div class="ti-name">
            {{ thread.name ?? t("messaging.groupChat") }}
          </div>
          <div class="ti-last">
            {{ thread.lastMessage ?? t("messaging.noMessages") }}
          </div>
          <div class="ti-meta">
            <span v-if="thread.unreadCount" class="ti-badge">{{
              thread.unreadCount
            }}</span>
            <span class="ti-date">{{ formatDate(thread.lastMessageAt) }}</span>
          </div>
        </button>
      </aside>

      <!-- ── Chat area ───────────────────────────────────────────── -->
      <div class="chat-area">
        <GroupChat
          v-if="activeThreadId"
          :thread-id="activeThreadId"
          @left="onLeft"
        />
        <div v-else class="empty">
          {{ t("messaging.selectConversation") }}
          <span v-if="threads.length === 0">{{
            t("messaging.groupChatEmpty")
          }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vendor-group-chat {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 116px);
}
.page-header {
  margin-bottom: 20px;
}
h2 {
  margin: 0 0 4px;
  font-size: 1.4rem;
}
.subtitle {
  color: var(--color-muted);
  margin: 0;
  font-size: 0.9rem;
}
.state-msg {
  padding: 48px;
  text-align: center;
  color: var(--color-muted);
}
.error {
  color: #e84040;
}

/* ── Layout ─────────────────────────────────────────────────────── */
.chat-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: var(--color-white);
}

/* ── Thread list sidebar ────────────────────────────────────────── */
.thread-list {
  width: 260px;
  border-right: 1px solid var(--color-border);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}
.thread-item {
  padding: 14px 16px;
  border: none;
  border-bottom: 1px solid var(--color-border);
  background: none;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.thread-item:hover {
  background: var(--color-surface);
}
.thread-item.active {
  background: var(--color-gold-light, #fef8ee);
  border-left: 3px solid var(--color-gold);
}
.ti-name {
  font-weight: 600;
  font-size: 0.88rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ti-last {
  font-size: 0.78rem;
  color: var(--color-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ti-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}
.ti-badge {
  background: var(--color-gold);
  color: #fff;
  font-size: 0.7rem;
  font-weight: 700;
  border-radius: 10px;
  padding: 1px 7px;
}
.ti-date {
  font-size: 0.72rem;
  color: var(--color-muted);
}

/* ── Chat area ─────────────────────────────────────────────────── */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.empty {
  flex: 1;
  padding: 48px;
  text-align: center;
  color: var(--color-muted);
  font-size: 0.88rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
