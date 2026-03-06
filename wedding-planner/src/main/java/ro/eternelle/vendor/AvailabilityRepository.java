package ro.eternelle.vendor;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AvailabilityRepository implements PanacheRepositoryBase<VendorAvailability, UUID> {

    public List<VendorAvailability> findByVendor(UUID vendorId) {
        return find("vendor.id = ?1 ORDER BY date ASC", vendorId).list();
    }

    public Optional<VendorAvailability> findByVendorAndDate(UUID vendorId, LocalDate date) {
        return find("vendor.id = ?1 AND date = ?2", vendorId, date).firstResultOptional();
    }

    /** Returns vendor IDs that have this date blocked. */
    public List<UUID> findBlockedVendorIds(LocalDate date) {
        return find("date = ?1", date).stream()
                .map(a -> a.vendor.id)
                .collect(java.util.stream.Collectors.toList());
    }
}
