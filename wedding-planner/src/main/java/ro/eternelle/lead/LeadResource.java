package ro.eternelle.lead;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Path("/api/leads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LeadResource {

    @Inject LeadService leadService;
    @Inject JsonWebToken jwt;

    public record CreateLeadRequest(
            UUID vendorId,
            LocalDate eventDate,
            @DecimalMin(value = "0", message = "Budget must be non-negative")
            @DecimalMax(value = "9999999999.99", message = "Budget exceeds maximum allowed value")
            @Digits(integer = 10, fraction = 2, message = "Budget must have at most 10 integer digits and 2 decimal places")
            BigDecimal budget,
            String message
    ) {}

    // Couple submits a lead to a vendor
    @POST
    @RolesAllowed("COUPLE")
    public Response create(@Valid CreateLeadRequest req) {
        UUID coupleUserId = UUID.fromString(jwt.getSubject());
        LeadDTO dto = leadService.createLead(coupleUserId, req.vendorId(),
                req.eventDate(), req.budget(), req.message());
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    // Vendor fetches their inbox; optional status filter
    @GET
    @RolesAllowed("VENDOR")
    public Response listForVendor(@QueryParam("status") LeadStatus status) {
        UUID vendorUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.getLeadsForVendor(vendorUserId, status)).build();
    }

    // Couple fetches their own leads
    @GET
    @Path("/my")
    @RolesAllowed("COUPLE")
    public Response listForCouple() {
        UUID coupleUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.getLeadsForCouple(coupleUserId)).build();
    }

    // Get single lead (both roles)
    @GET
    @Path("/{id}")
    @RolesAllowed({"COUPLE", "VENDOR"})
    public Response get(@PathParam("id") UUID id) {
        UUID actorUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.getLead(id, actorUserId)).build();
    }

    // Vendor marks a lead as VIEWED
    @PATCH
    @Path("/{id}/view")
    @RolesAllowed("VENDOR")
    public Response markViewed(@PathParam("id") UUID id) {
        UUID vendorUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.markViewed(id, vendorUserId)).build();
    }

    // Vendor accepts a lead → IN_DISCUSSION + creates Conversation
    @PATCH
    @Path("/{id}/accept")
    @RolesAllowed("VENDOR")
    public Response accept(@PathParam("id") UUID id) {
        UUID vendorUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.acceptLead(id, vendorUserId)).build();
    }

    // Vendor declines a lead
    @PATCH
    @Path("/{id}/decline")
    @RolesAllowed("VENDOR")
    public Response decline(@PathParam("id") UUID id) {
        UUID vendorUserId = UUID.fromString(jwt.getSubject());
        return Response.ok(leadService.declineLead(id, vendorUserId)).build();
    }
}
