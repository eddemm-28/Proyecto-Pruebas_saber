package co.edu.unicauca.access;

import co.edu.unicauca.domain.Role;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteUserRepositoryTest {

    private SqliteUserRepository repository;

    @BeforeEach
    void setUp() {
        // Usa base de datos SQLite en memoria
        repository = new SqliteUserRepository("jdbc:sqlite::memory:");
    }

    @Test
    @DisplayName("Debe guardar y consultar un usuario por su login en SQLite")
    void testSaveAndFindByLogin() {
        User user = new User("teacher1", "Profesor Pruebas", Role.DOCENTE, UserStatus.ACTIVO, "hash12345");

        boolean saved = repository.save(user);
        assertTrue(saved);

        User found = repository.findByLogin("teacher1");
        assertNotNull(found);
        assertEquals("teacher1", found.getLogin());
        assertEquals("Profesor Pruebas", found.getFullName());
        assertEquals(Role.DOCENTE, found.getRole());
        assertEquals(UserStatus.ACTIVO, found.getStatus());
        assertEquals("hash12345", found.getPasswordHash());
    }

    @Test
    @DisplayName("Debe retornar null al buscar un usuario que no existe")
    void testFindByLoginNotFound() {
        User found = repository.findByLogin("inexistente");
        assertNull(found);
    }

    @Test
    @DisplayName("Debe listar todos los usuarios guardados en SQLite")
    void testFindAll() {
        User u1 = new User("user1", "User One", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash1");
        User u2 = new User("user2", "User Two", Role.ADMINISTRADOR, UserStatus.ACTIVO, "hash2");

        repository.save(u1);
        repository.save(u2);

        List<User> users = repository.findAll();
        assertEquals(2, users.size());
    }

    @Test
    @DisplayName("Guarda y recupera el correo del usuario")
    void guardaCorreo() {
        repository.save(new User("rev1", "Revisor Uno", Role.REVISOR, UserStatus.ACTIVO, "hash", "rev1@unicauca.edu.co"));
        assertEquals("rev1@unicauca.edu.co", repository.findByLogin("rev1").getEmail());
    }

    @Test
    @DisplayName("Un usuario sin correo se guarda con correo nulo")
    void usuarioSinCorreo() {
        repository.save(new User("u2", "Usuario Dos", Role.ESTUDIANTE, UserStatus.ACTIVO, "hash"));
        assertNull(repository.findByLogin("u2").getEmail());
    }
}
