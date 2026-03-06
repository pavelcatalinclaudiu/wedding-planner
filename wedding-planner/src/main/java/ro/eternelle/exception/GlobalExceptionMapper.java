package ro.eternelle.exception;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Inject
    @ConfigProperty(name = "quarkus.http.cors.origins", defaultValue = "http://localhost:3000")
    String corsOrigins;

    private Response.ResponseBuilder cors(Response.ResponseBuilder b) {
        // Use the first configured origin for error responses (mirrors Quarkus CORS filter)
        String origin = corsOrigins.split(",")[0].trim();
        return b.header("Access-Control-Allow-Origin",      origin)
                .header("Access-Control-Allow-Credentials", "true");
    }

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof BusinessException) {
            return cors(Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage())))
                    .build();
        }
        if (exception instanceof UnauthorizedException) {
            return cors(Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage())))
                    .build();
        }
        // JAX-RS exceptions (NotFoundException, NotAllowedException, etc.) already
        // carry the correct HTTP status — return it directly without logging as an error.
        if (exception instanceof WebApplicationException wae) {
            Response original = wae.getResponse();
            return cors(Response.status(original.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", wae.getMessage() != null ? wae.getMessage() : "Not found")))
                    .build();
        }
        LOG.error("Unhandled exception", exception);
        return cors(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "An unexpected error occurred")))
                .build();
    }
}
