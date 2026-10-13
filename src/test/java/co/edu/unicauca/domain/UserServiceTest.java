package co.edu.unicauca.domain;

import co.edu.unicauca.access.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {

    private UserService userService;
    private MockUserRepository mockRepository;

    @BeforeEach
    void setUp() {
        mockRepository = new MockUserRepository();
        IPasswordHasher hasher = new PasswordHasher();
        IPasswordValidator validator = new PasswordValidator();
        userService = new UserService(mockRepository, hasher, validator);
    }

    @Test
    void testRegisterSuccess() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, null);
        boolean result = userService.register(user, "StrongPass123!");
        
        assertTrue(result);
        assertNotNull(mockRepository.findByLogin("jdoe"));
        assertNotNull(mockRepository.findByLogin("jdoe").getPasswordHash());
    }

    @Test
    void testRegisterWithValidEmail() {
        User user = new User("rev1", "Revisor Uno", Role.REVISOR, UserStatus.ACTIVO, null, "rev1@unicauca.edu.co");
        assertTrue(userService.register(user, "StrongPass123!"));
        assertEquals("rev1@unicauca.edu.co", mockRepository.findByLogin("rev1").getEmail());
    }

    @Test
    void testRegisterWithInvalidEmail() {
        User user = new User("rev2", "Revisor Dos", Role.REVISOR, UserStatus.ACTIVO, null, "correo-sin-arroba");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(user, "StrongPass123!"));
        assertTrue(ex.getMessage().contains("correo"));
        assertNull(mockRepository.findByLogin("rev2"));
    }

    @Test
    void testRegisterDuplicateLogin() {
        User user1 = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, null);
        userService.register(user1, "StrongPass123!");

        User user2 = new User("jdoe", "Jane Doe", Role.ESTUDIANTE, UserStatus.ACTIVO, null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(user2, "AnotherPass123!");
        });
        
        assertEquals("El nombre de usuario (Login) ya existe.", exception.getMessage());
    }

    @Test
    void testRegisterWeakPassword() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, null);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.register(user, "weak");
        });
        
        assertTrue(exception.getMessage().contains("La contraseña debe tener al menos 6 caracteres."));
    }

    @Test
    void testLoginSuccess() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, null);
        userService.register(user, "StrongPass123!");

        User loggedInUser = userService.login("jdoe", "StrongPass123!");
        assertNotNull(loggedInUser);
        assertEquals("jdoe", loggedInUser.getLogin());
    }

    @Test
    void testLoginWrongPassword() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.ACTIVO, null);
        userService.register(user, "StrongPass123!");

        User loggedInUser = userService.login("jdoe", "WrongPass123!");
        assertNull(loggedInUser);
    }

    @Test
    void testLoginInactiveUser() {
        User user = new User("jdoe", "John Doe", Role.DOCENTE, UserStatus.INACTIVO, null);
        userService.register(user, "StrongPass123!");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.login("jdoe", "StrongPass123!");
        });
        
        assertEquals("El usuario está inactivo.", exception.getMessage());
    }

    // Mock para simular la persistencia sin usar SQLite en pruebas unitarias
    private static class MockUserRepository implements IUserRepository {
        private final List<User> users = new ArrayList<>();

        @Override
        public boolean save(User user) {
            users.add(user);
            return true;
        }

        @Override
        public User findByLogin(String login) {
            for (User u : users) {
                if (u.getLogin().equals(login)) {
                    return u;
                }
            }
            return null;
        }

        @Override
        public List<User> findAll() {
            return users;
        }
    }
}
