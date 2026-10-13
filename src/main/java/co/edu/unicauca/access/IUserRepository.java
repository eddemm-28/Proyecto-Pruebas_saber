package co.edu.unicauca.access;

import co.edu.unicauca.domain.User;
import java.util.List;

/**
 * Interfaz para el repositorio de usuarios (Inversión de Dependencias - DIP).
 */
public interface IUserRepository {
    boolean save(User user);
    User findByLogin(String login);
    List<User> findAll();
}