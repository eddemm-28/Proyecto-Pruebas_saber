package co.edu.unicauca.domain;

import java.util.Objects;

/**
 * Entidad de dominio que representa a un usuario del sistema.
 */
public class User {
    private String login;
    private String fullName;
    private Role role;
    private UserStatus status;
    private String passwordHash;
    private String email;

    public User() {
    }

    public User(String login, String fullName, Role role, UserStatus status, String passwordHash) {
        this.login = login;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.passwordHash = passwordHash;
    }

    public User(String login, String fullName, Role role, UserStatus status, String passwordHash, String email) {
        this(login, fullName, role, status, passwordHash);
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }

    @Override
    public String toString() {
        return "User{" +
                "login='" + login + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status=" + status +
                '}';
    }
}
