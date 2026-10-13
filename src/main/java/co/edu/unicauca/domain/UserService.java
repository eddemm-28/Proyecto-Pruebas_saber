package co.edu.unicauca.domain;

import co.edu.unicauca.access.IUserRepository;
import java.util.List;

/**
 * Servicio de dominio para gestionar las operaciones de usuario (SRP).
 * Depende de abstracciones (DIP).
 */
public class UserService {

    private final IUserRepository userRepository;
    private final IPasswordHasher passwordHasher;
    private final IPasswordValidator passwordValidator;

    public UserService(IUserRepository userRepository, IPasswordHasher passwordHasher, IPasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.passwordValidator = passwordValidator;
    }

    /**
     * Registra un nuevo usuario en el sistema.
     * @param user Objeto User con los datos sin la contraseña encriptada (se usará passwordRaw)
     * @param rawPassword Contraseña en texto plano a validar y encriptar
     * @return true si el registro fue exitoso
     * @throws IllegalArgumentException si hay errores de validación
     */
    public boolean register(User user, String rawPassword) {
        // 1. Validar que no exista el login
        if (userRepository.findByLogin(user.getLogin()) != null) {
            throw new IllegalArgumentException("El nombre de usuario (Login) ya existe.");
        }

        // 1.1 Validar el correo (es opcional a nivel de servicio, pero si viene debe ser válido)
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()
                && !user.getEmail().trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }

        // 2. Validar la contraseña
        List<String> passwordErrors = passwordValidator.getValidationErrors(rawPassword);
        if (!passwordErrors.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", passwordErrors));
        }

        // 3. Generar el hash de la contraseña
        String hash = passwordHasher.hash(rawPassword);
        user.setPasswordHash(hash);

        // 4. Guardar en el repositorio
        return userRepository.save(user);
    }

    /**
     * Autentica un usuario en el sistema.
     * @param login Nombre de usuario
     * @param rawPassword Contraseña en texto plano
     * @return El usuario autenticado, o null si las credenciales son inválidas
     */
    public User login(String login, String rawPassword) {
        User user = userRepository.findByLogin(login);
        if (user == null) {
            return null; // Usuario no encontrado
        }
        
        if (!user.isActive()) {
            throw new IllegalStateException("El usuario está inactivo.");
        }

        if (passwordHasher.verify(user.getPasswordHash(), rawPassword)) {
            return user;
        }

        return null; // Contraseña incorrecta
    }
}
