package ro.eternelle.couple;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CoupleRepository implements PanacheRepositoryBase<CoupleProfile, UUID> {

    public Optional<CoupleProfile> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }

    public Optional<CoupleProfile> findBySubdomain(String subdomain) {
        return find("websiteSubdomain", subdomain).firstResultOptional();
    }
}
