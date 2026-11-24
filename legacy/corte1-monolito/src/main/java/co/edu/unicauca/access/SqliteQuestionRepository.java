package co.edu.unicauca.access;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import co.edu.unicauca.domain.QuestionRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositorio de preguntas con persistencia en SQLite.
 */
public class SqliteQuestionRepository implements QuestionRepository {

    private static final String COLUMNS =
            "id, nombre, contexto, pregunta, option_a, option_b, option_c, option_d, " +
            "respuesta_correcta, justificacion, bibliografia, competencia, tema, subtema, " +
            "dificultad, estado, autor";

    private final SqliteDatabase db;

    public SqliteQuestionRepository(SqliteDatabase db) {
        this.db = db;
        initTable();
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS questions (" +
                "id TEXT PRIMARY KEY, " +
                "nombre TEXT NOT NULL, " +
                "contexto TEXT, " +
                "pregunta TEXT NOT NULL, " +
                "option_a TEXT, option_b TEXT, option_c TEXT, option_d TEXT, " +
                "respuesta_correcta TEXT, " +
                "justificacion TEXT, " +
                "bibliografia TEXT, " +
                "competencia TEXT, " +
                "tema TEXT, " +
                "subtema TEXT, " +
                "dificultad TEXT, " +
                "estado TEXT NOT NULL, " +
                "autor TEXT" +
                ");";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("Error creando la tabla de preguntas: " + e.getMessage());
        } finally {
            db.release(conn);
        }
    }

    @Override
    public List<Question> findAll() {
        return query("SELECT " + COLUMNS + " FROM questions ORDER BY rowid", null);
    }

    @Override
    public Question findById(String id) {
        List<Question> result = query("SELECT " + COLUMNS + " FROM questions WHERE id = ?", id);
        return result.isEmpty() ? null : result.get(0);
    }

    @Override
    public List<Question> findByAuthor(String authorLogin) {
        return query("SELECT " + COLUMNS + " FROM questions WHERE autor = ? ORDER BY rowid", authorLogin);
    }

    @Override
    public boolean updateState(String id, String newState) {
        String sql = "UPDATE questions SET estado = ? WHERE id = ?";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newState);
                ps.setString(2, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error actualizando el estado de la pregunta: " + e.getMessage());
            return false;
        } finally {
            db.release(conn);
        }
    }

    @Override
    public boolean save(Question q) {
        if (q == null || q.getId() == null) {
            return false;
        }
        String sql = "INSERT INTO questions(" + COLUMNS + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, q.getId());
                fillCommonFields(ps, q, 2);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error guardando la pregunta: " + e.getMessage());
            return false;
        } finally {
            db.release(conn);
        }
    }

    @Override
    public boolean update(Question q) {
        if (q == null || q.getId() == null) {
            return false;
        }
        String sql = "UPDATE questions SET nombre=?, contexto=?, pregunta=?, option_a=?, option_b=?, " +
                "option_c=?, option_d=?, respuesta_correcta=?, justificacion=?, bibliografia=?, " +
                "competencia=?, tema=?, subtema=?, dificultad=?, estado=?, autor=? WHERE id=?";
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                fillCommonFields(ps, q, 1);
                ps.setString(17, q.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error actualizando la pregunta: " + e.getMessage());
            return false;
        } finally {
            db.release(conn);
        }
    }

    /** Llena los 16 campos que no son el id, empezando en el índice dado. */
    private void fillCommonFields(PreparedStatement ps, Question q, int start) throws SQLException {
        QuestionDistractors d = q.getDistractors() != null ? q.getDistractors() : new QuestionDistractors();
        int i = start;
        ps.setString(i++, q.getNombre());
        ps.setString(i++, q.getContexto());
        ps.setString(i++, q.getPregunta());
        ps.setString(i++, d.getOptionA());
        ps.setString(i++, d.getOptionB());
        ps.setString(i++, d.getOptionC());
        ps.setString(i++, d.getOptionD());
        ps.setString(i++, q.getRespuestaCorrecta());
        ps.setString(i++, q.getJustificacion());
        ps.setString(i++, q.getBibliografia());
        ps.setString(i++, q.getCompetencia());
        ps.setString(i++, q.getTema());
        ps.setString(i++, q.getSubtema());
        ps.setString(i++, q.getDificultad());
        ps.setString(i++, q.getEstado());
        ps.setString(i, q.getAutor());
    }

    private List<Question> query(String sql, String param) {
        List<Question> result = new ArrayList<>();
        Connection conn = null;
        try {
            conn = db.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (param != null) {
                    ps.setString(1, param);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapRow(rs));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando preguntas: " + e.getMessage());
        } finally {
            db.release(conn);
        }
        return result;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        QuestionDistractors d = new QuestionDistractors(
                rs.getString("option_a"), rs.getString("option_b"),
                rs.getString("option_c"), rs.getString("option_d"));
        return new Question(
                rs.getString("id"),
                rs.getString("nombre"),
                rs.getString("contexto"),
                rs.getString("pregunta"),
                d,
                rs.getString("respuesta_correcta"),
                rs.getString("justificacion"),
                rs.getString("bibliografia"),
                rs.getString("competencia"),
                rs.getString("tema"),
                rs.getString("subtema"),
                rs.getString("dificultad"),
                rs.getString("estado"),
                rs.getString("autor"));
    }
}
