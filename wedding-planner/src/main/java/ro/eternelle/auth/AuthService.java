package ro.eternelle.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import ro.eternelle.dto.auth.AuthResponse;
import ro.eternelle.exception.BusinessException;
import ro.eternelle.subscription.Subscription;
import ro.eternelle.subscription.SubscriptionRepository;
import ro.eternelle.user.User;
import ro.eternelle.user.UserRepository;
import ro.eternelle.user.UserRole;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);

    @Inject
    UserRepository userRepository;

    @Inject
    PasswordService passwordService;

    @Inject
    JwtService jwtService;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    EmailService emailService;

    @ConfigProperty(name = "app.auth.require-email-verification", defaultValue = "false")
    boolean requireEmailVerification;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Register ────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse register(String email, String password, UserRole role) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already in use");
        }

        User user = new User();
        user.email = email.toLowerCase().trim();
        user.passwordHash = passwordService.hash(password);
        user.role = role;

        // Always generate a verification token so the email can be sent
        String token = generateSecureToken();
        user.emailVerificationToken = token;
        user.emailVerificationExpires = Instant.now().plus(24, ChronoUnit.HOURS);

        userRepository.persist(user);

        // Auto-provision a FREE subscription for every new account
        Subscription freeSub = new Subscription();
        freeSub.user = user;
        freeSub.planName = "FREE";
        freeSub.status = "active";
        subscriptionRepository.persist(freeSub);

        // Fire-and-forget: don't fail registration if email delivery fails
        try {
            emailService.sendVerificationEmail(user.email, token);
        } catch (Exception ex) {
            LOG.errorf(ex, "Verification email failed for %s", user.email);
        }

        String jwt = jwtService.generateToken(user);
        return toAuthResponse(user, jwt);
    }

    // ── Login ───────────────────────────────────────────────────────────────

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

    // ── Verify email ────────────────────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired verification link"));

        if (user.emailVerificationExpires == null || Instant.now().isAfter(user.emailVerificationExpires)) {
            throw new BusinessException("Verification link has expired. Please request a new one.");
        }

        user.emailVerified = true;
        user.emailVerificationToken = null;
        user.emailVerificationExpires = null;
    }

    // ── Resend verification ─────────────────────────────────────────────────

    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            if (!user.emailVerified) {
                String token = generateSecureToken();
                user.emailVerificationToken = token;
                user.emailVerificationExpires = Instant.now().plus(24, ChronoUnit.HOURS);
                try {
                    emailService.sendVerificationEmail(user.email, token);
                } catch (Exception e) {
                    LOG.errorf(e, "Resend verification email failed for %s", user.email);
                }
            }
        });
        // Always return 204 — don't reveal whether the email exists
    }

    // ── Forgot password ─────────────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.toLowerCase().trim()).ifPresent(user -> {
            String token = generateSecureToken();
            user.passwordResetToken = token;
            user.passwordResetExpires = Instant.now().plus(1, ChronoUnit.HOURS);
            try {
                emailService.sendPasswordResetEmail(user.email, token);
            } catch (Exception e) {
                LOG.errorf(e, "Password reset email failed for %s", user.email);
            }
        });
        // Always return 204 — don't reveal whether the email exists
    }

    // ── Reset password ──────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired reset link"));

        if (user.passwordResetExpires == null || Instant.now().isAfter(user.passwordResetExpires)) {
            throw new BusinessException("Reset link has expired. Please request a new one.");
        }

        user.passwordHash = passwordService.hash(newPassword);
        user.passwordResetToken = null;
        user.passwordResetExpires = null;
    }

    // ── Get current user ────────────────────────────────────────────────────

    public AuthResponse getByUserId(UUID userId) {
        User user = userRepository.findById(userId);
        if (user == null) throw new BusinessException("User not found");
        String token = jwtService.generateToken(user);
        return toAuthResponse(user, token);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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
