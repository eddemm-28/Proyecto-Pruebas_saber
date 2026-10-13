package co.edu.unicauca.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de complejidad de contraseñas de acuerdo con la regla de negocio:
 * - Mínimo 6 caracteres
 * - Al menos un dígito
 * - Al menos un carácter especial
 * - Al menos una mayúscula
 */
public class PasswordValidator implements IPasswordValidator {

    @Override
    public boolean isValid(String password) {
        return getValidationErrors(password).isEmpty();
    }

    @Override
    public List<String> getValidationErrors(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("La contraseña no puede estar vacía.");
            return errors;
        }

        if (password.length() < 6) {
            errors.add("La contraseña debe tener al menos 6 caracteres.");
        }

        if (!password.matches(".*[A-Z].*")) {
            errors.add("La contraseña debe incluir al menos una letra mayúscula.");
        }

        if (!password.matches(".*[0-9].*")) {
            errors.add("La contraseña debe incluir al menos un dígito.");
        }

        // Carácter especial: cualquier carácter que no sea letra ni dígito
        if (!password.matches(".*[^a-zA-Z0-9].*")) {
            errors.add("La contraseña debe incluir al menos un carácter especial (ej. @, #, $, !, %, *, _).");
        }

        return errors;
    }
}
