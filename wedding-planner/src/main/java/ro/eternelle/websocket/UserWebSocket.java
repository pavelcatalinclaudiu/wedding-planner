package ro.eternelle.websocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * User-scoped WebSocket endpoint for real-time notification delivery.
 * Connect: ws://host/ws/user/{userId}
 * Events broadcast: NOTIFICATION
 */
@ServerEndpoint("/ws/user/{userId}")
public class UserWebSocket {

    @Inject
    WebSocketService webSocketService;

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        webSocketService.subscribe("user:" + userId, session);
    }

    @OnClose
    public void onClose(Session session) {
        webSocketService.unsubscribe(session);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        webSocketService.unsubscribe(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // no-op — server-push only
    }
}
