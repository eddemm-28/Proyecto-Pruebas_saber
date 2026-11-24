package co.edu.unicauca.saberpro.client.model;

import co.edu.unicauca.saberpro.client.observer.Subject;

/** Sesión en memoria (el JWT nunca se guarda en disco). Notifica LOGIN / LOGOUT. */
public class Session extends Subject {

    public static final String LOGIN = "LOGIN";
    public static final String LOGOUT = "LOGOUT";

    private volatile String token;
    private volatile Dtos.User user;

    public void start(Dtos.LoginResponse response) {
        this.token = response.accessToken();
        this.user = response.user();
        notifyObservers(LOGIN, user);
    }

    public void end() {
        this.token = null;
        this.user = null;
        notifyObservers(LOGOUT, null);
    }

    public String token() {
        return token;
    }

    public Dtos.User user() {
        return user;
    }

    public boolean hasRole(String... roles) {
        if (user == null) return false;
        for (String r : roles) {
            if (r.equals(user.role())) return true;
        }
        return false;
    }
}
