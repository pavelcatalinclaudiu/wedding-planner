package ro.eternelle.websocket;

public class WebSocketMessage {

    public String channel;
    public String event;
    public Object payload;

    public WebSocketMessage() {}

    public WebSocketMessage(String channel, String event, Object payload) {
        this.channel = channel;
        this.event = event;
        this.payload = payload;
    }
}
