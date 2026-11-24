package co.edu.unicauca.saberpro.users.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla de complejidad de contraseñas heredada del Corte 1: mínimo 6 caracteres,
 * una mayúscula, un dígito y un carácter especial. Máximo 72 bytes (límite de BCrypt).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 6;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("La contraseña no puede estar vacía.");
            return errors;
        }
        if (password.length() < MIN_LENGTH) {
            errors.add("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_LENGTH) {
            errors.add("La contraseña no puede superar " + MAX_LENGTH + " bytes.");
        }
        if (!password.matches(".*[A-Z].*")) {
            errors.add("La contraseña debe incluir al menos una letra mayúscula.");
        }
        if (!password.matches(".*[0-9].*")) {
            errors.add("La contraseña debe incluir al menos un dígito.");
        }
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            errors.add("La contraseña debe incluir al menos un carácter especial (ej. @, #, $, !, %, *, _).");
        }
        return errors;
    }
}
