package ro.eternelle.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {

    @NotBlank
    public String token;
}
