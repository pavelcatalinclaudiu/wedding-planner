package ro.eternelle.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSocketService {

    @Inject
    ObjectMapper objectMapper;

    // channel → sessions
    private final ConcurrentHashMap<String, Set<Session>> channelSessions = new ConcurrentHashMap<>();

    public void subscribe(String channel, Session session) {
        channelSessions.computeIfAbsent(channel, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(session);
    }

    public void unsubscribe(Session session) {
        channelSessions.values().forEach(sessions -> sessions.remove(session));
    }

    /** Convenience overload for plain String payloads (keeps existing callers binary-compatible). */
    public void broadcast(String channel, String event, String payload) {
        broadcast(channel, event, (Object) payload);
    }

    public void broadcast(String channel, String event, Object payload) {
        Set<Session> sessions = channelSessions.getOrDefault(channel, Set.of());
        if (sessions.isEmpty()) return;

        try {
            String json = objectMapper.writeValueAsString(new WebSocketMessage(channel, event, payload));
            sessions.stream()
                    .filter(Session::isOpen)
                    .forEach(s -> s.getAsyncRemote().sendText(json));
        } catch (Exception e) {
            // log silently
        }
    }
}
