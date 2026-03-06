package ro.eternelle.messaging;

public enum MessageType {
    TEXT, IMAGE, FILE, SYSTEM,
    /** A quote sent by the vendor — content is a JSON payload rendered as a card. */
    QUOTE
}
