package ro.eternelle.websocket;

import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebSocket endpoint that clients connect to for lead/enquiry real-time updates.
 * Connect: ws://host/ws/leads/{leadId}
 * Events broadcast: LEAD_UPDATED
 */
@ServerEndpoint("/ws/leads/{leadId}")
public class LeadWebSocket {

    @Inject
    WebSocketService webSocketService;

    @OnOpen
    public void onOpen(Session session, @PathParam("leadId") String leadId) {
        webSocketService.subscribe("lead:" + leadId, session);
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
        // server-push only
    }
}
