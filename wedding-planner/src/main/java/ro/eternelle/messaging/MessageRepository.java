package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class MessageRepository implements PanacheRepositoryBase<Message, UUID> {

    public List<Message> findByThread(UUID threadId) {
        return find("thread.id = ?1 ORDER BY createdAt ASC", threadId).list();
    }

    public List<Message> findByThreadPaged(UUID threadId, int page, int size) {
        return find("thread.id = ?1 ORDER BY createdAt ASC", threadId)
                .page(page, size)
                .list();
    }

    public long countUnread(UUID threadId, UUID userId) {
        return findByThread(threadId).stream()
                .filter(m -> m.readBy == null || !m.readBy.contains(userId))
                .count();
    }

    /** First message in a thread sent by a specific user, ordered by time. */
    public java.util.Optional<Message> findFirstByThreadAndSender(UUID threadId, UUID senderId) {
        return find("thread.id = ?1 AND sender.id = ?2 ORDER BY createdAt ASC", threadId, senderId)
                .firstResultOptional();
    }

    /** Very first message ever posted in a thread (the opening enquiry). */
    public java.util.Optional<Message> findFirstByThread(UUID threadId) {
        return find("thread.id = ?1 ORDER BY createdAt ASC", threadId).firstResultOptional();
    }
}
