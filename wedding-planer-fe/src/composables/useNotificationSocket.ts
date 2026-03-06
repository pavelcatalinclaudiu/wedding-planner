/**
 * Thin WebSocket wrapper for user-scoped real-time notification delivery.
 * Connects to /ws/user/{userId} and calls onNotification() whenever a
 * NOTIFICATION event arrives, so the bell badge updates without polling.
 */
export function useNotificationSocket() {
  let socket: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let currentUserId: string | null = null;
  let handler: (() => void) | null = null;

  function connect(userId: string, onNotification: () => void) {
    disconnect();
    currentUserId = userId;
    handler = onNotification;

    const token = localStorage.getItem("access_token");
    socket = new WebSocket(
      `${import.meta.env.VITE_WS_BASE_URL}/user/${userId}?token=${token}`,
    );

    socket.onmessage = (event: MessageEvent) => {
      try {
        const msg = JSON.parse(event.data as string) as {
          event: string;
        };
        if (msg.event === "NOTIFICATION" && handler) handler();
      } catch {
        // ignore malformed frames
      }
    };

    socket.onclose = () => {
      if (currentUserId) {
        reconnectTimer = setTimeout(
          () => connect(currentUserId!, handler!),
          3000,
        );
      }
    };

    socket.onerror = () => socket?.close();
  }

  function disconnect() {
    currentUserId = null;
    handler = null;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    if (socket) {
      socket.onclose = null;
      socket.onerror = null;
      socket.close();
      socket = null;
    }
  }

  return { connect, disconnect };
}
