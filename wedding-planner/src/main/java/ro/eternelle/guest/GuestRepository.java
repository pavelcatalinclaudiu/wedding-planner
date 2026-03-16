package ro.eternelle.guest;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GuestRepository implements PanacheRepositoryBase<Guest, UUID> {

    public List<Guest> findByCouple(UUID coupleId) {
        return find("couple.id = ?1 ORDER BY firstName, lastName", coupleId).list();
    }

    public long countByCouple(UUID coupleId) {
        return count("couple.id = ?1", coupleId);
    }

    public long countByCoupleAndRsvp(UUID coupleId, String status) {
        return count("couple.id = ?1 AND rsvpStatus = ?2", coupleId, status);
    }

    public List<Guest> findByCoupleAndRsvp(UUID coupleId, String status) {
        return find("couple.id = ?1 AND rsvpStatus = ?2", coupleId, status).list();
    }

    public List<Guest> findByCoupleAndDietary(UUID coupleId, String dietary) {
        return find("couple.id = ?1 AND dietary = ?2", coupleId, dietary).list();
    }

    public List<String> findSongRequests(UUID coupleId) {
        return getEntityManager()
            .createQuery(
                "SELECT DISTINCT g.songRequest FROM Guest g "
              + "WHERE g.couple.id = :cid AND g.songRequest IS NOT NULL AND g.songRequest <> ''",
                String.class)
            .setParameter("cid", coupleId)
            .getResultList();
    }

    public java.util.Optional<Guest> findByInviteToken(String token) {
        return find("inviteToken = ?1", token).firstResultOptional();
    }
}
