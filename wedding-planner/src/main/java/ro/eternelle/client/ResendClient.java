package ro.eternelle.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import ro.eternelle.dto.email.SendEmailRequest;
import ro.eternelle.dto.email.SendEmailResponse;

@RegisterRestClient(configKey = "ro.eternelle.client.ResendClient")
@Path("/emails")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ResendClient {

    @POST
    SendEmailResponse send(
            @HeaderParam("Authorization") String authorization,
            SendEmailRequest request);
}
