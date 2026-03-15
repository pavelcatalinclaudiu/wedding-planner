package ro.eternelle.auth;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import ro.eternelle.dto.auth.AuthResponse;
import ro.eternelle.dto.auth.ForgotPasswordRequest;
import ro.eternelle.dto.auth.LoginRequest;
import ro.eternelle.dto.auth.RegisterRequest;
import ro.eternelle.dto.auth.ResetPasswordRequest;
import ro.eternelle.dto.auth.VerifyEmailRequest;

import java.util.UUID;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest req) {
        AuthResponse auth = authService.register(req.email, req.password, req.role);
        return Response.status(Response.Status.CREATED).entity(auth).build();
    }

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest req) {
        AuthResponse auth = authService.login(req.email, req.password);
        return Response.ok(auth).build();
    }

    @POST
    @Path("/logout")
    @PermitAll
    public Response logout() {
        return Response.noContent().build();
    }

    @GET
    @Path("/me")
    @RolesAllowed({"COUPLE", "VENDOR", "ADMIN"})
    public Response me() {
        UUID userId = UUID.fromString(jwt.getSubject());
        return Response.ok(authService.getByUserId(userId)).build();
    }

    @POST
    @Path("/verify-email")
    @PermitAll
    public Response verifyEmail(@Valid VerifyEmailRequest req) {
        authService.verifyEmail(req.token);
        return Response.noContent().build();
    }

    @POST
    @Path("/resend-verification")
    @PermitAll
    public Response resendVerification(ForgotPasswordRequest req) {
        authService.resendVerification(req.email);
        return Response.noContent().build();
    }

    @POST
    @Path("/forgot-password")
    @PermitAll
    public Response forgotPassword(@Valid ForgotPasswordRequest req) {
        authService.requestPasswordReset(req.email);
        return Response.noContent().build();
    }

    @POST
    @Path("/reset-password")
    @PermitAll
    public Response resetPassword(@Valid ResetPasswordRequest req) {
        authService.resetPassword(req.token, req.newPassword);
        return Response.noContent().build();
    }
}
