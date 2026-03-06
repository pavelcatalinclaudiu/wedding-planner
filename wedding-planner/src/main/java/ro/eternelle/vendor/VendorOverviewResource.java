package ro.eternelle.vendor;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.vendor.dto.VendorOverviewDTO;

import java.util.UUID;

@Path("/api/vendor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VendorOverviewResource {

    @Inject
    VendorAnalyticsService vendorAnalyticsService;

    @Inject
    VendorRepository vendorRepository;

    @Inject
    JsonWebToken jwt;

    /**
     * Returns all overview stats for the authenticated vendor in a single call.
     * Response is cached per vendor for 5 minutes; mutations (new deal, review,
     * booking) invalidate the cache automatically.
     */
    @GET
    @Path("/overview-stats")
    @RolesAllowed("VENDOR")
    public Response getOverviewStats(@Context SecurityContext sec) {
        UUID userId = UUID.fromString(jwt.getSubject());

        VendorProfile vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Vendor profile not found"));

        VendorOverviewDTO overview = vendorAnalyticsService.buildOverview(vendor.id, userId);
        return Response.ok(overview).build();
    }
}
