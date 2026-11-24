package co.edu.unicauca.saberpro.users.service;

import co.edu.unicauca.saberpro.users.domain.LoginAudit;
import co.edu.unicauca.saberpro.users.domain.LoginAudit.FailureReason;
import co.edu.unicauca.saberpro.users.domain.PasswordPolicy;
import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.repository.UserRepository;
import co.edu.unicauca.saberpro.users.web.error.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** A08: registro (RF-01) y login (RF-02) con BCrypt (RNF-08) y auditoría (RNF-09). */
@Service
public class AuthService {

    private static final Pattern LOGIN = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAuditService audit;
    /** Hash real para gastar el mismo tiempo cuando el usuario no existe (evita enumeración por tiempo). */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, TokenService tokens, LoginAuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.audit = audit;
        this.dummyHash = encoder.encode("usuario-inexistente");
    }

    /**
     * Registro público. Corrección del Corte 1: no se puede elegir rol; toda cuenta nace
     * como ESTUDIANTE y los demás roles los asigna un administrador (RF-03).
     */
    @Transactional
    public UserAccount register(String login, String fullName, String email, String rawPassword) {
        List<String> errors = new ArrayList<>();
        if (login == null || !LOGIN.matcher(login.trim()).matches()) {
            errors.add("El login debe tener entre 3 y 50 caracteres (letras, números, punto, guion o guion bajo).");
        }
        if (fullName == null || fullName.isBlank() || fullName.trim().length() > 120) {
            errors.add("El nombre completo es obligatorio (máximo 120 caracteres).");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            errors.add("El correo electrónico no es válido.");
        }
        errors.addAll(PasswordPolicy.validate(rawPassword));
        if (!errors.isEmpty()) {
            throw ApiException.validation(errors);
        }
        if (users.existsByLoginIgnoreCase(login.trim())) {
            throw ApiException.conflict("El nombre de usuario (login) ya existe.");
        }
        if (users.existsByEmailIgnoreCase(email.trim())) {
            throw ApiException.conflict("El correo electrónico ya está registrado.");
        }
        UserAccount user = new UserAccount(login, fullName, email, encoder.encode(rawPassword), Role.ESTUDIANTE);
        return users.save(user);
    }

    /** Login: valida credenciales, audita el intento y emite el JWT. */
    @Transactional(readOnly = true)
    public LoginResult login(String login, String rawPassword, String ip, String clientAgent) {
        String attempted = login == null ? "" : login.trim();
        Optional<UserAccount> found = users.findByLoginIgnoreCase(attempted);

        if (found.isEmpty()) {
            encoder.matches(rawPassword == null ? "" : rawPassword, dummyHash);
            audit.record(LoginAudit.failure(null, attempted, FailureReason.USER_NOT_FOUND, ip, clientAgent));
            throw ApiException.unauthorized("Usuario o contraseña incorrectos.");
        }
        UserAccount user = found.get();
        if (rawPassword == null || !encoder.matches(rawPassword, user.getPasswordHash())) {
            audit.record(LoginAudit.failure(user.getId(), attempted, FailureReason.BAD_CREDENTIALS, ip, clientAgent));
            throw ApiException.unauthorized("Usuario o contraseña incorrectos.");
        }
        if (!user.isActive()) {
            audit.record(LoginAudit.failure(user.getId(), attempted, FailureReason.USER_INACTIVE, ip, clientAgent));
            throw ApiException.forbidden("El usuario está inactivo. Contacte al administrador.");
        }
        audit.record(LoginAudit.success(user.getId(), attempted, ip, clientAgent));
        return new LoginResult(user, tokens.issue(user));
    }

    public record LoginResult(UserAccount user, TokenService.IssuedToken token) {
    }
}
