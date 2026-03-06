package ro.eternelle.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank
    public String token;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    public String newPassword;
}
