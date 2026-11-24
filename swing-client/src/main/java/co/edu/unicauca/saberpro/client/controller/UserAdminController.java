package co.edu.unicauca.saberpro.client.controller;

import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Subject;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** RF-03 (A09) y RNF-09 (A11) para el Administrador. Notifica USERS_CHANGED. */
public class UserAdminController extends Subject {

    public static final String USERS_CHANGED = "USERS_CHANGED";

    private final ApiClient api;

    public UserAdminController(ApiClient api) {
        this.api = api;
    }

    public Dtos.Page<Dtos.User> list(String role, String status, String q, int page) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("role", role);
        query.put("status", status);
        query.put("q", q);
        query.put("page", page);
        query.put("size", 20);
        return api.get("/api/users", query, new TypeReference<Dtos.Page<Dtos.User>>() { });
    }

    public Dtos.User changeRole(UUID id, String role) {
        Dtos.User u = api.patch("/api/users/" + id + "/role", new Dtos.ChangeRole(role), Dtos.User.class);
        notifyObservers(USERS_CHANGED, u);
        return u;
    }

    public Dtos.User changeStatus(UUID id, String status) {
        Dtos.User u = api.patch("/api/users/" + id + "/status", new Dtos.ChangeStatus(status), Dtos.User.class);
        notifyObservers(USERS_CHANGED, u);
        return u;
    }

    public Dtos.Page<Dtos.LoginAudit> loginAudit(String login, Boolean success, int page) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("login", login);
        query.put("success", success);
        query.put("page", page);
        query.put("size", 50);
        return api.get("/api/users/audit/logins", query, new TypeReference<Dtos.Page<Dtos.LoginAudit>>() { });
    }
}
