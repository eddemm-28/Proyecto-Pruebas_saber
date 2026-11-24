package co.edu.unicauca.access;

import co.edu.unicauca.domain.ReviewAssignment;
import co.edu.unicauca.domain.ReviewAssignmentRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistencia en SQLite de las asignaciones pregunta-revisor.
 */
public class SqliteReviewAssignmentRepository implements ReviewAssignmentRepository {

    private final SqliteDatabase db;

    public SqliteReviewAssignmentRepository(SqliteDatabase db) {
        this.db = db;
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS review_assignments (" +
                "question_id TEXT NOT NULL, " +
                "reviewer_login TEXT NOT NULL, " +
                "assigned_by TEXT NOT NULL, " +
                "assigned_at TEXT NOT NULL, " +
                "PRIMARY KEY (question_id, reviewer_login)" +
                ");";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("Error creando la tabla de asignaciones: " + e.getMessage());
        } finally {
            db.release(conn);
        }
    }

    @Override
    public boolean save(ReviewAssignment a) {
        String sql = "INSERT INTO review_assignments(question_id, reviewer_login, assigned_by, assigned_at) " +
                "VALUES(?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.getQuestionId());
                ps.setString(2, a.getReviewerLogin());
                ps.setString(3, a.getAssignedBy());
                ps.setString(4, a.getAssignedAt().toString());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error guardando la asignación: " + e.getMessage());
            return false;
        } finally {
            db.release(conn);
        }
    }

    @Override
    public boolean exists(String questionId, String reviewerLogin) {
        String sql = "SELECT 1 FROM review_assignments WHERE question_id = ? AND reviewer_login = ?";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, questionId);
                ps.setString(2, reviewerLogin);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando la asignación: " + e.getMessage());
            return false;
        } finally {
            db.release(conn);
        }
    }

    @Override
    public List<ReviewAssignment> findByQuestion(String questionId) {
        return query("SELECT question_id, reviewer_login, assigned_by, assigned_at " +
                "FROM review_assignments WHERE question_id = ? ORDER BY rowid", questionId);
    }

    @Override
    public List<ReviewAssignment> findByReviewer(String reviewerLogin) {
        return query("SELECT question_id, reviewer_login, assigned_by, assigned_at " +
                "FROM review_assignments WHERE reviewer_login = ? ORDER BY rowid", reviewerLogin);
    }

    private List<ReviewAssignment> query(String sql, String param) {
        List<ReviewAssignment> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, param);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(new ReviewAssignment(
                                rs.getString("question_id"),
                                rs.getString("reviewer_login"),
                                rs.getString("assigned_by"),
                                LocalDateTime.parse(rs.getString("assigned_at"))));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando asignaciones: " + e.getMessage());
        } finally {
            db.release(conn);
        }
        return result;
    }
}
