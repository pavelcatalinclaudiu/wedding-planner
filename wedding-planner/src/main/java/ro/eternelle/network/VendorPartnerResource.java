package ro.eternelle.network;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.vendor.VendorProfile;
import ro.eternelle.vendor.VendorRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/api/network")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorPartnerResource {

    @Inject VendorPartnerRepository partnerRepository;
    @Inject VendorRepository vendorRepository;
    @Inject JsonWebToken jwt;

    // ── Authenticated: own partner list ──────────────────────────────────────

    @GET
    @RolesAllowed("VENDOR")
    public Response getPartners() {
        VendorProfile vendor = getVendor();
        List<VendorPartnerDTO> result = partnerRepository.findByVendor(vendor.id)
                .stream().map(VendorPartnerDTO::from).collect(Collectors.toList());
        return Response.ok(result).build();
    }

    // ── Public: partners shown on vendor detail page ──────────────────────────

    @GET
    @Path("/vendor/{vendorId}")
    @PermitAll
    public Response getPublicPartners(@PathParam("vendorId") UUID vendorId) {
        List<VendorPartnerDTO> result = partnerRepository.findByVendor(vendorId)
                .stream().map(VendorPartnerDTO::from).collect(Collectors.toList());
        return Response.ok(result).build();
    }

    // ── Authenticated: search platform vendors to add ─────────────────────────

    @GET
    @Path("/search")
    @RolesAllowed("VENDOR")
    public Response searchVendors(@QueryParam("q") String q) {
        if (q == null || q.trim().length() < 2) {
            return Response.ok(List.of()).build();
        }
        VendorProfile self = getVendor();
        List<VendorProfile> hits = vendorRepository.searchByName(q.trim(), 10);
        List<VendorPartnerDTO> dtos = hits.stream()
                .filter(v -> !v.id.equals(self.id))
                .map(v -> {
                    VendorPartnerDTO dto = new VendorPartnerDTO();
                    dto.partnerId       = v.id.toString();
                    dto.partnerName     = v.businessName;
                    dto.partnerCategory = v.category != null ? v.category.name() : null;
                    dto.partnerCity        = v.city;
                    dto.partnerCoverPhoto  = v.coverPhoto;
                    dto.onPlatform = true;
                    return dto;
                })
                .collect(Collectors.toList());
        return Response.ok(dtos).build();
    }

    // ── Authenticated: add a partner ─────────────────────────────────────────

    @POST
    @RolesAllowed("VENDOR")
    @Transactional
    public Response addPartner(AddPartnerRequest req) {
        VendorProfile vendor = getVendor();
        VendorPartner vp = new VendorPartner();
        vp.vendor = vendor;

        if (req.partnerId != null) {
            VendorProfile partner = vendorRepository.findById(req.partnerId);
            if (partner == null) throw new BusinessException("Vendor not found");
            if (partner.id.equals(vendor.id)) throw new BusinessException("You cannot add yourself as a partner");
            vp.partner = partner;
        } else if (req.partnerName != null && !req.partnerName.isBlank()) {
            vp.partnerName = req.partnerName.trim();
        } else {
            throw new BusinessException("Either partnerId or partnerName is required");
        }

        partnerRepository.persist(vp);
        return Response.status(Response.Status.CREATED)
                .entity(VendorPartnerDTO.from(vp)).build();
    }

    // ── Authenticated: remove a partner ──────────────────────────────────────

    @DELETE
    @Path("/{id}")
    @RolesAllowed("VENDOR")
    @Transactional
    public Response removePartner(@PathParam("id") UUID id) {
        VendorPartner vp = partnerRepository.findById(id);
        if (vp == null) return Response.status(404).build();
        VendorProfile vendor = getVendor();
        if (!vp.vendor.id.equals(vendor.id)) return Response.status(403).build();
        partnerRepository.deleteById(id);
        return Response.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private VendorProfile getVendor() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Vendor profile not found"));
    }

    public static class AddPartnerRequest {
        public UUID partnerId;
        public String partnerName;
    }
}
