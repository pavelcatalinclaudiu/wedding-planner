package ro.eternelle.websocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint that clients connect to for deal-level real-time updates.
 * Connect: ws://host/ws/deals/{dealId}
 * The client will receive events: STATUS_CHANGED, DEAL_CREATED, NEW_MESSAGE, etc.
 */
@ServerEndpoint("/ws/deals/{dealId}")
public class DealWebSocket {

    @Inject
    WebSocketService webSocketService;

    @OnOpen
    public void onOpen(Session session, @PathParam("dealId") String dealId) {
        webSocketService.subscribe("deal:" + dealId, session);
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
        // Ping/pong or channel subscribe messages could be handled here
    }
}
