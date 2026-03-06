package ro.eternelle.auth;

import jakarta.enterprise.context.ApplicationScoped;
import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped
public class PasswordService {

    private static final int WORKFACTOR = 12;

    public String hash(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(WORKFACTOR));
    }

    public boolean verify(String plaintext, String hashed) {
        return BCrypt.checkpw(plaintext, hashed);
    }
}
