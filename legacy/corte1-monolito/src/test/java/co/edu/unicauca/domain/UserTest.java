package co.edu.unicauca.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Debe crear correctamente un usuario y retornar sus atributos")
    void testUserCreation() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, "hash123");

        assertEquals("jdoe", user.getLogin());
        assertEquals("John Doe", user.getFullName());
        assertEquals(Role.DOCENTE, user.getRole());
        assertEquals(UserStatus.ACTIVO, user.getStatus());
        assertEquals("hash123", user.getPasswordHash());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Debe validar el estado inactivo del usuario")
    void testUserInactiveStatus() {
        User user = new User("jdoe", "John Doe", Role.ESTUDIANTE, UserStatus.INACTIVO, "hash123");

        assertEquals(UserStatus.INACTIVO, user.getStatus());
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("Debe parsear correctamente roles por su nombre legible o enum name")
    void testRoleFromDisplayName() {
        assertEquals(Role.ADMINISTRADOR, Role.fromDisplayName("Administrador"));
        assertEquals(Role.AUTOR_DE_PREGUNTAS, Role.fromDisplayName("Autor de preguntas"));
        assertEquals(Role.REVISOR, Role.fromDisplayName("REVISOR"));
        assertEquals(Role.DOCENTE, Role.fromDisplayName("Docente"));
        assertEquals(Role.ESTUDIANTE, Role.fromDisplayName("Estudiante"));
    }

    @Test
    @DisplayName("Debe parsear correctamente estados de usuario")
    void testUserStatusFromDisplayName() {
        assertEquals(UserStatus.ACTIVO, UserStatus.fromDisplayName("Activo"));
        assertEquals(UserStatus.INACTIVO, UserStatus.fromDisplayName("INACTIVO"));
    }
}

