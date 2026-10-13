package co.edu.unicauca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new PasswordHasher();
    }

    @Test
    @DisplayName("Debe generar un hash Argon2 no nulo y verificar correctamente la contraseña")
    void testHashAndVerifySuccess() {
        String plainPassword = "SecretPassword123!";
        String hash = hasher.hash(plainPassword);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertTrue(hasher.verify(hash, plainPassword));
    }

    @Test
    @DisplayName("Debe fallar al verificar una contraseña incorrecta")
    void testVerifyWrongPassword() {
        String plainPassword = "SecretPassword123!";
        String hash = hasher.hash(plainPassword);

        assertFalse(hasher.verify(hash, "WrongPassword123!"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta hashear nulo")
    void testHashNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }
}

