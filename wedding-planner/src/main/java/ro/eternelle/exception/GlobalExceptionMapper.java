package ro.eternelle.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.Map;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<RuntimeException> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof BusinessException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();
        }
        if (exception instanceof UnauthorizedException) {
            return Response.status(Response.Status.FORBIDDEN)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", exception.getMessage()))
                    .build();
        }
        // JAX-RS exceptions (NotFoundException, NotAllowedException, etc.) already
        // carry the correct HTTP status — return it directly without logging as an error.
        if (exception instanceof WebApplicationException wae) {
            Response original = wae.getResponse();
            return Response.status(original.getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(Map.of("error", wae.getMessage() != null ? wae.getMessage() : "Not found"))
                    .build();
        }
        LOG.error("Unhandled exception", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Map.of("error", "An unexpected error occurred"))
                .build();
    }
}
