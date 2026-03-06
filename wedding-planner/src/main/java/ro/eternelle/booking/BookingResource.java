package ro.eternelle.booking;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.vendor.VendorRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Path("/api/bookings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"COUPLE", "VENDOR"})
public class BookingResource {

    @Inject BookingService    bookingService;
    @Inject CoupleRepository  coupleRepository;
    @Inject VendorRepository  vendorRepository;
    @Inject JsonWebToken      jwt;

    @GET
    @Path("/lead/{leadId}")
    public Response getByLead(@PathParam("leadId") UUID leadId) {
        return Response.ok(bookingService.getByLead(leadId)).build();
    }

    @GET
    @Path("/my")
    public Response getMyBookings() {
        UUID   userId = UUID.fromString(jwt.getSubject());
        String role   = jwt.getClaim("role");
        if ("COUPLE".equals(role)) {
            return coupleRepository.findByUserId(userId)
                    .map(cp -> Response.ok(bookingService.getByCouple(cp.id)).build())
                    .orElse(Response.status(404).build());
        } else {
            return vendorRepository.findByUserId(userId)
                    .map(vp -> Response.ok(bookingService.getByVendor(vp.id)).build())
                    .orElse(Response.status(404).build());
        }
    }

    @POST
    @Path("/{id}/deposit")
    public Response recordDeposit(@PathParam("id") UUID id, java.util.Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        return Response.ok(bookingService.recordDeposit(id, amount)).build();
    }
}
