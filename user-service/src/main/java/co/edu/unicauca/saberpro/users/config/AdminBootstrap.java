package co.edu.unicauca.saberpro.users.config;

import co.edu.unicauca.saberpro.users.domain.Role;
import co.edu.unicauca.saberpro.users.domain.UserAccount;
import co.edu.unicauca.saberpro.users.domain.UserStatus;
import co.edu.unicauca.saberpro.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el administrador inicial si no existe ningún administrador activo. Las credenciales
 * vienen de variables de entorno (ADMIN_LOGIN / ADMIN_PASSWORD), nunca del código ni de SQL.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String login;
    private final String password;
    private final String email;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder,
                          @Value("${security.bootstrap-admin.login}") String login,
                          @Value("${security.bootstrap-admin.password}") String password,
                          @Value("${security.bootstrap-admin.email}") String email) {
        this.users = users;
        this.encoder = encoder;
        this.login = login;
        this.password = password;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.countByRoleAndStatus(Role.ADMINISTRADOR, UserStatus.ACTIVO) > 0 || users.existsByLoginIgnoreCase(login)) {
            return;
        }
        users.save(new UserAccount(login, "Administrador del sistema", email, encoder.encode(password), Role.ADMINISTRADOR));
        log.warn("Administrador inicial '{}' creado. Cambie la contraseña por defecto en producción.", login);
    }
}
