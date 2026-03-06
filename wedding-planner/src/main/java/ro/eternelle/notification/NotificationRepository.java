package ro.eternelle.notification;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepository implements PanacheRepositoryBase<Notification, UUID> {

    public List<Notification> findByUser(UUID userId) {
        return find("user.id = ?1", Sort.descending("createdAt"), userId).list();
    }

    public List<Notification> findUnreadByUser(UUID userId) {
        return find("user.id = ?1 and read = false", Sort.descending("createdAt"), userId).list();
    }

    public long countUnread(UUID userId) {
        return count("user.id = ?1 and read = false", userId);
    }
}