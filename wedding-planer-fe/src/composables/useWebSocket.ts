import { ref } from "vue";

type MessageHandler = (payload: Record<string, unknown>) => void;

// ── Module-level singleton ────────────────────────────────────────────────────
// All composables share ONE WebSocket connection. This prevents duplicate
// connections to the same channel and ensures every handler always receives
// events regardless of which composable connected first.
const socket = ref<WebSocket | null>(null);
const connected = ref(false);
const handlers = new Map<string, MessageHandler[]>();
let currentDealId: string | null = null;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

function connect(dealId: string) {
  // Already open or connecting on this deal — nothing to do
  if (
    currentDealId === dealId &&
    socket.value != null &&
    (socket.value.readyState === WebSocket.OPEN ||
      socket.value.readyState === WebSocket.CONNECTING)
  )
    return;

  if (socket.value) {
    const prev = socket.value;
    prev.onclose = null;
    prev.onerror = null;
    prev.close();
  }
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  currentDealId = dealId;
  const token = localStorage.getItem("access_token");
  const url = `${import.meta.env.VITE_WS_BASE_URL}/deals/${dealId}?token=${token}`;
  socket.value = new WebSocket(url);

  socket.value.onopen = () => {
    connected.value = true;
  };

  socket.value.onmessage = (event: MessageEvent) => {
    try {
      const msg = JSON.parse(event.data as string) as {
        channel: string;
        event: string;
        payload: Record<string, unknown>;
      };
      const fns = handlers.get(msg.event);
      if (fns) fns.forEach((fn) => fn(msg.payload));
    } catch {
      // ignore malformed messages
    }
  };

  socket.value.onclose = () => {
    connected.value = false;
    if (currentDealId) {
      reconnectTimer = setTimeout(() => connect(currentDealId!), 3000);
    }
  };

  socket.value.onerror = () => {
    socket.value?.close();
  };
}

function disconnect() {
  currentDealId = null;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (socket.value) {
    socket.value.onclose = null;
    socket.value.onerror = null;
    socket.value.close();
    socket.value = null;
  }
  connected.value = false;
}

/**
 * Register a handler and return a cleanup function that removes it.
 * Call the returned function in onUnmounted to deregister without closing the socket.
 */
function on(eventType: string, callback: MessageHandler): () => void {
  if (!handlers.has(eventType)) handlers.set(eventType, []);
  handlers.get(eventType)!.push(callback);
  return () => off(eventType, callback);
}

function off(eventType: string, callback: MessageHandler) {
  const fns = handlers.get(eventType);
  if (fns) {
    handlers.set(
      eventType,
      fns.filter((f) => f !== callback),
    );
  }
}

function send(type: string, payload: Record<string, unknown>) {
  if (socket.value?.readyState === WebSocket.OPEN) {
    socket.value.send(JSON.stringify({ type, payload }));
  }
}

export function useWebSocket() {
  return { connect, disconnect, on, off, send, connected };
}
