package ro.eternelle.conversation;

public enum ConversationMessageType {
    TEXT,
    /** System event: couple requested a revised offer. Content is a JSON payload with the offerId. */
    OFFER_REVISION_REQUEST
}
