package co.edu.unicauca.access;

import co.edu.unicauca.domain.ReviewAssignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteReviewAssignmentRepositoryTest {

    private SqliteDatabase db;
    private SqliteReviewAssignmentRepository repository;

    @BeforeEach
    void setUp() {
        db = new SqliteDatabase("jdbc:sqlite::memory:");
        repository = new SqliteReviewAssignmentRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private ReviewAssignment asignacion(String pregunta, String revisor) {
        return new ReviewAssignment(pregunta, revisor, "admin", LocalDateTime.of(2026, 10, 11, 18, 30));
    }

    @Test
    @DisplayName("Guarda una asignación y la encuentra por pregunta")
    void guardarYBuscarPorPregunta() {
        assertTrue(repository.save(asignacion("P-001", "rev1")));

        List<ReviewAssignment> lista = repository.findByQuestion("P-001");
        assertEquals(1, lista.size());
        assertEquals("rev1", lista.get(0).getReviewerLogin());
        assertEquals("admin", lista.get(0).getAssignedBy());
        assertEquals(LocalDateTime.of(2026, 10, 11, 18, 30), lista.get(0).getAssignedAt());
    }

    @Test
    @DisplayName("Una pregunta puede tener varios revisores")
    void variosRevisores() {
        repository.save(asignacion("P-001", "rev1"));
        repository.save(asignacion("P-001", "rev2"));
        assertEquals(2, repository.findByQuestion("P-001").size());
    }

    @Test
    @DisplayName("No permite asignar dos veces el mismo revisor a la misma pregunta")
    void sinDuplicados() {
        assertTrue(repository.save(asignacion("P-001", "rev1")));
        assertFalse(repository.save(asignacion("P-001", "rev1")));
        assertEquals(1, repository.findByQuestion("P-001").size());
    }

    @Test
    @DisplayName("exists indica si la asignación ya existe")
    void existe() {
        repository.save(asignacion("P-001", "rev1"));
        assertTrue(repository.exists("P-001", "rev1"));
        assertFalse(repository.exists("P-001", "rev2"));
        assertFalse(repository.exists("P-002", "rev1"));
    }

    @Test
    @DisplayName("Busca las asignaciones de un revisor")
    void buscarPorRevisor() {
        repository.save(asignacion("P-001", "rev1"));
        repository.save(asignacion("P-002", "rev1"));
        repository.save(asignacion("P-002", "rev2"));

        assertEquals(2, repository.findByReviewer("rev1").size());
        assertEquals(1, repository.findByReviewer("rev2").size());
        assertTrue(repository.findByReviewer("nadie").isEmpty());
    }
}
