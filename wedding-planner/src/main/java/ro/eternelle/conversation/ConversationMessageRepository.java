package ro.eternelle.conversation;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ConversationMessageRepository implements PanacheRepositoryBase<ConversationMessage, UUID> {

    public List<ConversationMessage> findByConversation(UUID conversationId) {
        return find("conversation.id = ?1", Sort.by("sentAt").ascending(), conversationId).list();
    }
}
