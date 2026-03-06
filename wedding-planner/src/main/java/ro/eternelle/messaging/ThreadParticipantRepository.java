package ro.eternelle.messaging;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ThreadParticipantRepository
        implements PanacheRepositoryBase<ThreadParticipant, UUID> {

    public List<ThreadParticipant> findByThread(UUID threadId) {
        return find("thread.id", threadId).list();
    }

    public Optional<ThreadParticipant> findByThreadAndUser(UUID threadId, UUID userId) {
        return find("thread.id = ?1 AND user.id = ?2", threadId, userId)
                .firstResultOptional();
    }
}
