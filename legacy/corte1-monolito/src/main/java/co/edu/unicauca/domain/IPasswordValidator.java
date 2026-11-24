package co.edu.unicauca.domain;

import java.util.List;

/**
 * Interfaz para la validación de seguridad de contraseñas (Inversión de Dependencias - DIP).
 */
public interface IPasswordValidator {
    boolean isValid(String password);
    List<String> getValidationErrors(String password);
}

