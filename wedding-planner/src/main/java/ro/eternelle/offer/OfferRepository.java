package ro.eternelle.offer;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OfferRepository implements PanacheRepositoryBase<Offer, UUID> {

    public List<Offer> findByLead(UUID leadId) {
        return find("lead.id = ?1", Sort.by("createdAt").descending(), leadId).list();
    }

    public List<Offer> findByConversation(UUID conversationId) {
        return find("conversation.id = ?1", Sort.by("createdAt").descending(), conversationId).list();
    }

    public java.util.Optional<Offer> findPendingByLead(UUID leadId) {
        return find("lead.id = ?1 AND status = ?2", leadId, OfferStatus.PENDING)
                .firstResultOptional();
    }
}
