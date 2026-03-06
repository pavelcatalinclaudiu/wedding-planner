package ro.eternelle.budget;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.couple.CoupleProfile;
import ro.eternelle.couple.CoupleRepository;
import ro.eternelle.exception.BusinessException;

import java.util.List;
import java.util.UUID;

@Path("/api/budget")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("COUPLE")
public class BudgetResource {

    @Inject BudgetRepository budgetRepository;
    @Inject CoupleRepository coupleRepository;
    @Inject JsonWebToken jwt;

    @GET
    public Response getItems() {
        CoupleProfile couple = getCouple();
        return Response.ok(budgetRepository.findByCouple(couple.id)).build();
    }

    @POST
    @Transactional
    public Response addItem(BudgetItem item) {
        CoupleProfile couple = getCouple();
        item.couple = couple;
        budgetRepository.persist(item);
        return Response.status(Response.Status.CREATED).entity(item).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response updateItem(@PathParam("id") UUID id, BudgetItem updates) {
        CoupleProfile couple = getCouple();
        BudgetItem item = budgetRepository.findById(id);
        if (item == null || !item.couple.id.equals(couple.id)) throw new BusinessException("Budget item not found");
        if (updates.name != null)          item.name          = updates.name;
        if (updates.category != null)      item.category      = updates.category;
        if (updates.estimatedCost != null) item.estimatedCost = updates.estimatedCost;
        if (updates.actualCost != null)    item.actualCost    = updates.actualCost;
        item.isPaid = updates.isPaid;
        return Response.ok(item).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteItem(@PathParam("id") UUID id) {
        CoupleProfile couple = getCouple();
        BudgetItem item = budgetRepository.findById(id);
        if (item == null || !item.couple.id.equals(couple.id)) throw new BusinessException("Budget item not found");
        budgetRepository.delete(item);
        return Response.noContent().build();
    }

    private CoupleProfile getCouple() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return coupleRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Couple profile not found"));
    }
}
