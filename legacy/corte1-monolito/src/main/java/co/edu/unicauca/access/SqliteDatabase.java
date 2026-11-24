package co.edu.unicauca.access;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Pequeño ayudante para abrir conexiones SQLite. Lo comparten los repositorios nuevos.
 * Con ":memory:" mantiene una sola conexión abierta (si no, la base se pierde al cerrarla).
 */
public class SqliteDatabase implements AutoCloseable {

    public static final String DEFAULT_URL = "jdbc:sqlite:users.db";

    private final String url;
    private Connection memoryConnection;

    public SqliteDatabase() {
        this(DEFAULT_URL);
    }

    public SqliteDatabase(String url) {
        this.url = url;
        if (url.contains(":memory:")) {
            try {
                memoryConnection = DriverManager.getConnection(url);
            } catch (SQLException e) {
                throw new IllegalStateException("No se pudo abrir la base en memoria: " + e.getMessage(), e);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (memoryConnection != null && !memoryConnection.isClosed()) {
            return memoryConnection;
        }
        return DriverManager.getConnection(url);
    }

    public void release(Connection conn) {
        if (conn != null && conn != memoryConnection) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
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
