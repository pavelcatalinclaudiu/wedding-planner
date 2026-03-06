import { ref, onUnmounted } from "vue";
import { conversationsApi } from "@/api/conversations.api";
import { useWebSocket } from "@/composables/useWebSocket";
import type {
  Conversation,
  ConversationMessage,
} from "@/types/conversation.types";

export function useConversation() {
  const conversation = ref<Conversation | null>(null);
  const messages = ref<ConversationMessage[]>([]);
  const loading = ref(false);
  const sending = ref(false);
  const error = ref<string | null>(null);

  const ws = useWebSocket();

  // NEW_MESSAGE payload now contains the full ConversationMessage DTO.
  // Append it directly — no re-fetch, no race against DB commit.
  const offNewMessage = ws.on("NEW_MESSAGE", (payload) => {
    const incoming = payload as unknown as ConversationMessage;
    if (!incoming?.id) return;
    // Only handle messages for the currently open conversation
    if (
      !conversation.value ||
      incoming.conversationId !== conversation.value.id
    )
      return;
    // Deduplicate: skip if we already have it (e.g. optimistic local add)
    if (messages.value.some((m) => m.id === incoming.id)) return;
    messages.value.push(incoming);
  });

  onUnmounted(() => {
    offNewMessage();
    ws.disconnect();
  });

  async function loadForLead(leadId: string) {
    loading.value = true;
    error.value = null;
    try {
      conversation.value = (await conversationsApi.getByLead(leadId)).data;
      await loadMessages(conversation.value.id);
      // Connect (or switch) to this lead's WebSocket channel
      ws.connect(leadId);
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to load conversation";
    } finally {
      loading.value = false;
    }
  }

  async function loadMessages(conversationId: string) {
    messages.value = (await conversationsApi.getMessages(conversationId)).data;
  }

  async function send(content: string) {
    if (!conversation.value) return;
    sending.value = true;
    try {
      const msg = (
        await conversationsApi.sendMessage(conversation.value.id, content)
      ).data;
      // Guard against duplicate if WS event arrived before HTTP response
      if (!messages.value.some((m) => m.id === msg.id)) {
        messages.value.push(msg);
      }
      return msg;
    } catch (e: any) {
      error.value = e?.response?.data?.message ?? "Failed to send message";
    } finally {
      sending.value = false;
    }
  }

  return {
    conversation,
    messages,
    loading,
    sending,
    error,
    loadForLead,
    send,
  };
}
