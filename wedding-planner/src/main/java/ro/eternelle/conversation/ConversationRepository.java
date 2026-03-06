package ro.eternelle.conversation;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ConversationRepository implements PanacheRepositoryBase<Conversation, UUID> {

    public Optional<Conversation> findByLead(UUID leadId) {
        return find("lead.id", leadId).firstResultOptional();
    }

    public List<Conversation> findByCouple(UUID coupleId) {
        return find("couple.id = ?1", Sort.by("lastMessageAt").descending(), coupleId).list();
    }

    public List<Conversation> findByVendor(UUID vendorId) {
        return find("vendor.id = ?1", Sort.by("lastMessageAt").descending(), vendorId).list();
    }
}
