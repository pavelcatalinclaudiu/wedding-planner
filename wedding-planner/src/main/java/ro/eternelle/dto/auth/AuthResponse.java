package ro.eternelle.dto.auth;

import java.time.Instant;

public class AuthResponse {

    public String accessToken;
    public String tokenType = "Bearer";
    public UserDto user;

    public AuthResponse(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public static class UserDto {
        public String id;
        public String email;
        public String role;
        public boolean emailVerified;
        public Instant createdAt;

        public UserDto(String id, String email, String role, boolean emailVerified, Instant createdAt) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.emailVerified = emailVerified;
            this.createdAt = createdAt;
        }
    }
}
