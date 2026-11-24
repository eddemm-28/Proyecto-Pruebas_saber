package co.edu.unicauca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Test
    void testValidPassword() {
        assertTrue(validator.isValid("Aa1234!"));
        assertTrue(validator.isValid("StrongPass_2026"));
    }

    @Test
    void testTooShortPassword() {
        assertFalse(validator.isValid("Aa1!"));
        List<String> errors = validator.getValidationErrors("Aa1!");
        assertTrue(errors.contains("La contraseña debe tener al menos 6 caracteres."));
    }

    @Test
    void testNoUpperCase() {
        assertFalse(validator.isValid("aa1234!"));
        List<String> errors = validator.getValidationErrors("aa1234!");
        assertTrue(errors.contains("La contraseña debe incluir al menos una letra mayúscula."));
    }

    @Test
    void testNoDigit() {
        assertFalse(validator.isValid("Aaaaaa!"));
        List<String> errors = validator.getValidationErrors("Aaaaaa!");
        assertTrue(errors.contains("La contraseña debe incluir al menos un dígito."));
    }

    @Test
    void testNoSpecialCharacter() {
        assertFalse(validator.isValid("Aa123456"));
        List<String> errors = validator.getValidationErrors("Aa123456");
        assertTrue(errors.contains("La contraseña debe incluir al menos un carácter especial (ej. @, #, $, !, %, *, _)."));
    }
}
