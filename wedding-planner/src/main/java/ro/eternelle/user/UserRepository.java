package ro.eternelle.user;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRepository implements PanacheRepositoryBase<User, UUID> {

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }

    public Optional<User> findByEmailVerificationToken(String token) {
        return find("emailVerificationToken", token).firstResultOptional();
    }

    public Optional<User> findByPasswordResetToken(String token) {
        return find("passwordResetToken", token).firstResultOptional();
    }
}
