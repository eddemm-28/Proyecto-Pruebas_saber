package co.edu.unicauca.saberpro.client.controller;

import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.model.Session;

/** Login (RF-02) y registro (RF-01) contra /api/auth. */
public class AuthController {

    private final ApiClient api;
    private final Session session;

    public AuthController(ApiClient api, Session session) {
        this.api = api;
        this.session = session;
    }

    public Dtos.User login(String login, String password) {
        Dtos.LoginResponse resp = api.post("/api/auth/login", new Dtos.LoginRequest(login, password), Dtos.LoginResponse.class);
        session.start(resp);
        return resp.user();
    }

    public Dtos.User register(String login, String fullName, String email, String password) {
        return api.post("/api/auth/register", new Dtos.RegisterRequest(login, fullName, email, password), Dtos.User.class);
    }

    public void logout() {
        session.end();
    }
}
