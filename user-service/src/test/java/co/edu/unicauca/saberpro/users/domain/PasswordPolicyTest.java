package co.edu.unicauca.saberpro.users.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @Test
    void contrasenaValida() {
        assertTrue(PasswordPolicy.validate("Segura1*").isEmpty());
    }

    @Test
    void contrasenaVaciaDevuelveUnSoloError() {
        assertEquals(1, PasswordPolicy.validate("").size());
    }

    @Test
    void detectaCadaReglaIncumplida() {
        // corta, sin mayúscula, sin dígito, sin especial
        assertEquals(4, PasswordPolicy.validate("abc").size());
    }

    @Test
    void rechazaMasDe72Bytes() {
        String larga = "A1*" + "x".repeat(80);
        assertTrue(PasswordPolicy.validate(larga).stream().anyMatch(e -> e.contains("72")));
    }
}
