package ro.eternelle.vendor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class VendorRepository implements PanacheRepositoryBase<VendorProfile, UUID> {

    public Optional<VendorProfile> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }

    public List<VendorProfile> findByCategory(VendorCategory category) {
        return find("category = ?1 AND isActive = true", category).list();
    }

    public List<VendorProfile> findByCity(String city) {
        if (city == null || city.isBlank()) return List.of();
        return find("lower(city) = ?1 AND isActive = true", city.toLowerCase()).list();
    }

    public List<VendorProfile> searchByKeyword(String keyword) {
        String q = "%" + keyword.toLowerCase() + "%";
        return find("(lower(businessName) like ?1 OR lower(description) like ?1) AND isActive = true", q).list();
    }

    /**
     * Count how many active vendors in the same category + city
     * have a strictly higher avgRating than the given vendor.
     * Adding 1 gives a 1-based search rank.
     */
    /** Name-search used by the Partner Network add-partner UI */
    public java.util.List<VendorProfile> searchByName(String keyword, int limit) {
        String q = "%" + keyword.toLowerCase() + "%";
        return find("lower(businessName) like ?1 AND isActive = true", q)
                .page(0, limit).list();
    }

    public long countWithHigherRating(VendorCategory category, String city, BigDecimal rating) {
        if (category == null || city == null || city.isBlank()) return 0L;
        BigDecimal effectiveRating = rating != null ? rating : BigDecimal.ZERO;
        return count(
                "category = ?1 AND lower(city) = ?2 AND isActive = true AND avgRating > ?3",
                category, city.toLowerCase(), effectiveRating);
    }
}
