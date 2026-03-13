package ro.eternelle.website;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WeddingWebsiteRepository implements PanacheRepositoryBase<WeddingWebsite, UUID> {

    public Optional<WeddingWebsite> findByCouple(UUID coupleId) {
        return find("couple.id", coupleId).firstResultOptional();
    }
}
