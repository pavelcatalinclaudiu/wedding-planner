package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ThreadRepository implements PanacheRepositoryBase<Thread, UUID> {

    public Optional<Thread> findByLead(UUID leadId) {
        return find("lead.id", leadId).firstResultOptional();
    }

    /** Find the single GROUP thread that belongs to a couple. */
    public Optional<Thread> findGroupByCouple(UUID coupleId) {
        return find("couple.id = ?1 AND threadType = ?2", coupleId, ThreadType.GROUP)
                .firstResultOptional();
    }

    /** All threads (DIRECT + GROUP) where the user is a participant. */
    public List<Thread> findByUser(UUID userId) {
        return getEntityManager()
                .createQuery(
                        "SELECT t FROM Thread t JOIN ThreadParticipant p ON p.thread.id = t.id " +
                        "WHERE p.user.id = :userId ORDER BY t.lastMessageAt DESC",
                        Thread.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    /** Only the GROUP threads where the user is a participant (vendor's planning chats). */
    public List<Thread> findGroupThreadsByUser(UUID userId) {
        return getEntityManager()
                .createQuery(
                        "SELECT t FROM Thread t JOIN ThreadParticipant p ON p.thread.id = t.id " +
                        "WHERE p.user.id = :userId AND t.threadType = 'GROUP' ORDER BY t.lastMessageAt DESC",
                        Thread.class)
                .setParameter("userId", userId)
                .getResultList();
    }
}
