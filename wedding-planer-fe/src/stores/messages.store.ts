import { defineStore } from "pinia";
import { ref } from "vue";
import { messagesApi } from "@/api/messages.api";
import { useVendorOverviewStore } from "@/stores/vendorOverview.store";
import type { Thread, Message } from "@/types/message.types";

export const useMessagesStore = defineStore("messages", () => {
  const threads = ref<Thread[]>([]);
  const currentThread = ref<Thread | null>(null);
  const messages = ref<Record<string, Message[]>>({});
  const unreadCounts = ref<Record<string, number>>({});

  async function fetchThreads() {
    const res = await messagesApi.getThreads();
    threads.value = res.data;
    res.data.forEach((t) => {
      unreadCounts.value[t.id] = t.unreadCount ?? 0;
    });
  }

  async function fetchMessages(threadId: string) {
    const res = await messagesApi.getMessages(threadId);
    messages.value[threadId] = res.data;
  }

  async function sendMessage(threadId: string, content: string) {
    const res = await messagesApi.sendMessage(threadId, { content });
    if (!messages.value[threadId]) messages.value[threadId] = [];
    messages.value[threadId].push(res.data);
    // update thread last message preview
    const idx = threads.value.findIndex((t) => t.id === threadId);
    if (idx !== -1) {
      threads.value[idx] = {
        ...threads.value[idx],
        lastMessage: res.data.content,
        lastMessageAt: res.data.createdAt,
      };
    }
    // Invalidate vendor overview so response rate recalculates on next visit.
    useVendorOverviewStore().invalidate();
    return res.data;
  }

  async function markRead(threadId: string) {
    await messagesApi.markRead(threadId);
    unreadCounts.value[threadId] = 0;
    const idx = threads.value.findIndex((t) => t.id === threadId);
    if (idx !== -1)
      threads.value[idx] = { ...threads.value[idx], unreadCount: 0 };
  }

  async function startThread(vendorId: string, firstMessage: string) {
    const res = await messagesApi.startThread(vendorId, firstMessage);
    threads.value.unshift(res.data);
    return res.data;
  }

  async function openThread(threadId: string) {
    currentThread.value = threads.value.find((t) => t.id === threadId) ?? null;
    await fetchMessages(threadId);
    await markRead(threadId);
  }

  async function fetchGroupThread(_dealId: string) {
    const res = await messagesApi.getGroupChat();
    return res.data;
  }

  function appendMessage(threadId: string, message: Message) {
    if (!messages.value[threadId]) messages.value[threadId] = [];
    messages.value[threadId].push(message);
    const idx = threads.value.findIndex((t) => t.id === threadId);
    if (idx !== -1) {
      unreadCounts.value[threadId] = (unreadCounts.value[threadId] || 0) + 1;
      threads.value[idx] = {
        ...threads.value[idx],
        lastMessage: message.content,
        unreadCount: unreadCounts.value[threadId],
      };
    }
  }

  const totalUnread = () =>
    Object.values(unreadCounts.value).reduce((a, b) => a + b, 0);

  function reset() {
    threads.value = [];
    currentThread.value = null;
    messages.value = {};
    unreadCounts.value = {};
  }

  return {
    threads,
    currentThread,
    messages,
    unreadCounts,
    fetchThreads,
    fetchMessages,
    openThread,
    sendMessage,
    markRead,
    startThread,
    fetchGroupThread,
    appendMessage,
    totalUnread,
    reset,
  };
});
