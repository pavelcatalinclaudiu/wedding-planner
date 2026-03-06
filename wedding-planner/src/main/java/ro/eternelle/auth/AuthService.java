package ro.eternelle.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import ro.eternelle.dto.auth.AuthResponse;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.user.UserRole;

import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    JwtService jwtService;

    @ConfigProperty(name = "app.auth.require-email-verification", defaultValue = "false")
    boolean requireEmailVerification;

    @Transactional
    public AuthResponse register(String email, String password, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already in use");
        }

        User user = new User();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordService.hash(password);
        user.role = role;
        userRepository.persist(user);

        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    @Transactional
    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordService.verify(password, user.passwordHash)) {
            throw new BusinessException("Invalid credentials");
        }

        if (requireEmailVerification && !user.emailVerified) {
            throw new BusinessException("Please verify your email address before logging in");
        }

        user.lastLogin = Instant.now();
        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    public AuthResponse getByUserId(java.util.UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) throw new BusinessException("User not found");
        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return new AuthResponse(
            token,
            new AuthResponse.UserDto(
                user.id.toString(),
                user.email,
                user.role.name(),
                user.emailVerified,
                user.createdAt
            )
        );
    }
}
