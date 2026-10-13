package co.edu.unicauca.access;

import co.edu.unicauca.domain.Role;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.domain.UserStatus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación concreta del repositorio de usuarios utilizando SQLite JDBC.
 * Soporta bases de datos físicas y en memoria.
 */
public class SqliteUserRepository implements IUserRepository, AutoCloseable {

    private final String dbUrl;
    private Connection memoryConnection;

    public SqliteUserRepository() {
        this("jdbc:sqlite:users.db");
    }

    public SqliteUserRepository(String dbUrl) {
        this.dbUrl = dbUrl;
        if (dbUrl.contains(":memory:")) {
            try {
                this.memoryConnection = DriverManager.getConnection(dbUrl);
            } catch (SQLException e) {
                System.err.println("Error creando conexión en memoria para SQLite: " + e.getMessage());
            }
        }
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        if (memoryConnection != null && !memoryConnection.isClosed()) {
            return memoryConnection;
        }
        return DriverManager.getConnection(dbUrl);
    }

    private void closeIfNew(Connection conn) {
        if (conn != memoryConnection && conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "login TEXT PRIMARY KEY, " +
                "full_name TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "email TEXT" +
                ");";

        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
            agregarColumnaEmailSiNoExiste(conn);
        } catch (SQLException e) {
            System.err.println("Error inicializando la base de datos SQLite: " + e.getMessage());
        } finally {
            closeIfNew(conn);
        }
    }

    /** Las bases creadas antes de este cambio no tienen la columna email. */
    private void agregarColumnaEmailSiNoExiste(Connection conn) throws SQLException {
        boolean existe = false;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                if ("email".equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                }
            }
        }
        if (!existe) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT");
            }
        }
    }

    @Override
    public boolean save(User user) {
        if (user == null || user.getLogin() == null) {
            return false;
        }

        String sql = "INSERT INTO users(login, full_name, role, status, password_hash, email) VALUES(?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user.getLogin().trim());
                pstmt.setString(2, user.getFullName() != null ? user.getFullName().trim() : "");
                pstmt.setString(3, user.getRole() != null ? user.getRole().name() : Role.ESTUDIANTE.name());
                pstmt.setString(4, user.getStatus() != null ? user.getStatus().name() : UserStatus.ACTIVO.name());
                pstmt.setString(5, user.getPasswordHash());
                pstmt.setString(6, user.getEmail() != null ? user.getEmail().trim() : null);

                int affectedRows = pstmt.executeUpdate();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error al guardar usuario en SQLite: " + e.getMessage());
            return false;
        } finally {
            closeIfNew(conn);
        }
    }

    @Override
    public User findByLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT login, full_name, role, status, password_hash, email FROM users WHERE login = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, login.trim());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar usuario por login: " + e.getMessage());
        } finally {
            closeIfNew(conn);
        }

        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT login, full_name, role, status, password_hash, email FROM users";

        Connection conn = null;
        try {
            conn = getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al listar todos los usuarios: " + e.getMessage());
        } finally {
            closeIfNew(conn);
        }

        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String login = rs.getString("login");
        String fullName = rs.getString("full_name");
        String roleStr = rs.getString("role");
        String statusStr = rs.getString("status");
        String passwordHash = rs.getString("password_hash");
        String email = rs.getString("email");

        Role role = Role.fromDisplayName(roleStr);
        UserStatus status = UserStatus.fromDisplayName(statusStr);

        return new User(login, fullName, role, status, passwordHash, email);
    }

    @Override
    public void close() {
        if (memoryConnection != null) {
            try {
                memoryConnection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
