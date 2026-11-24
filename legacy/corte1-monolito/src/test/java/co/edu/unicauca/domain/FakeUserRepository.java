package co.edu.unicauca.domain;

import co.edu.unicauca.access.IUserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Repositorio de usuarios en memoria solo para pruebas. */
class FakeUserRepository implements IUserRepository {
    private final Map<String, User> users = new LinkedHashMap<>();

    @Override
    public boolean save(User user) {
        users.put(user.getLogin(), user);
        return true;
    }

    @Override
    public User findByLogin(String login) {
        return users.get(login);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }
}
