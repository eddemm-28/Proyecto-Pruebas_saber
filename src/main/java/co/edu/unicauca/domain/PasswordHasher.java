package co.edu.unicauca.domain;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Implementación de hashing seguro de contraseñas utilizando Argon2id.
 */
public class PasswordHasher implements IPasswordHasher {

    private final Argon2 argon2;

    public PasswordHasher() {
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }

    @Override
    public String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("La contraseña a cifrar no puede ser nula.");
        }
        char[] passwordChars = password.toCharArray();
        try {
            return argon2.hash(2, 15360, 1, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }

    @Override
    public boolean verify(String hash, String password) {
        if (hash == null || password == null) {
            return false;
        }
        char[] passwordChars = password.toCharArray();
        try {
            return argon2.verify(hash, passwordChars);
        } finally {
            argon2.wipeArray(passwordChars);
        }
    }
}
