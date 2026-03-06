package ro.eternelle.network;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class VendorPartnerRepository implements PanacheRepositoryBase<VendorPartner, UUID> {

    public List<VendorPartner> findByVendor(UUID vendorId) {
        return find("vendor.id", vendorId).list();
    }
}
