package co.edu.unicauca.access;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteQuestionRepositoryTest {

    private SqliteDatabase db;
    private SqliteQuestionRepository repository;

    @BeforeEach
    void setUp() {
        db = new SqliteDatabase("jdbc:sqlite::memory:");
        repository = new SqliteQuestionRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Question pregunta(String id, String autor) {
        return new Question(id, "Título " + id, "Contexto de la pregunta " + id, "¿Cuál es la respuesta?",
                new QuestionDistractors("Opción 1", "Opción 2", "Opción 3", "Opción 4"), "B",
                "Justificación de la respuesta", "Bibliografía de ejemplo", "Competencia X",
                "Tema Y", "Subtema Z", "Media", Question.STATE_BORRADOR, autor);
    }

    @Test
    @DisplayName("Guarda una pregunta y la recupera con todos sus campos")
    void guardarYConsultar() {
        assertTrue(repository.save(pregunta("P-001", "autor1")));

        Question q = repository.findById("P-001");
        assertNotNull(q);
        assertEquals("Título P-001", q.getNombre());
        assertEquals("Contexto de la pregunta P-001", q.getContexto());
        assertEquals("Opción 3", q.getDistractors().getOptionC());
        assertEquals("B", q.getRespuestaCorrecta());
        assertEquals("Justificación de la respuesta", q.getJustificacion());
        assertEquals("Bibliografía de ejemplo", q.getBibliografia());
        assertEquals("Competencia X", q.getCompetencia());
        assertEquals("Tema Y", q.getTema());
        assertEquals("Subtema Z", q.getSubtema());
        assertEquals("Media", q.getDificultad());
        assertEquals(Question.STATE_BORRADOR, q.getEstado());
        assertEquals("autor1", q.getAutor());
    }

    @Test
    @DisplayName("Devuelve null si la pregunta no existe")
    void noExiste() {
        assertNull(repository.findById("P-999"));
    }

    @Test
    @DisplayName("No permite guardar dos preguntas con el mismo id")
    void idRepetido() {
        assertTrue(repository.save(pregunta("P-001", "autor1")));
        assertFalse(repository.save(pregunta("P-001", "autor1")));
    }

    @Test
    @DisplayName("Lista todas las preguntas en orden de inserción")
    void listarTodas() {
        repository.save(pregunta("P-001", "autor1"));
        repository.save(pregunta("P-002", "autor2"));
        List<Question> todas = repository.findAll();
        assertEquals(2, todas.size());
        assertEquals("P-001", todas.get(0).getId());
    }

    @Test
    @DisplayName("Lista solo las preguntas de un autor")
    void listarPorAutor() {
        repository.save(pregunta("P-001", "autor1"));
        repository.save(pregunta("P-002", "autor2"));
        repository.save(pregunta("P-003", "autor1"));

        assertEquals(2, repository.findByAuthor("autor1").size());
        assertEquals(0, repository.findByAuthor("nadie").size());
    }

    @Test
    @DisplayName("Actualiza el estado de una pregunta")
    void actualizarEstado() {
        repository.save(pregunta("P-001", "autor1"));
        assertTrue(repository.updateState("P-001", Question.STATE_PENDIENTE_REVISION));
        assertEquals(Question.STATE_PENDIENTE_REVISION, repository.findById("P-001").getEstado());
        assertFalse(repository.updateState("P-999", Question.STATE_ELIMINADA));
    }

    @Test
    @DisplayName("Actualiza los datos de una pregunta existente")
    void actualizarPregunta() {
        repository.save(pregunta("P-001", "autor1"));
        Question q = repository.findById("P-001");
        q.setNombre("Título editado");
        q.setTema("Otro tema");

        assertTrue(repository.update(q));
        assertEquals("Título editado", repository.findById("P-001").getNombre());
        assertEquals("Otro tema", repository.findById("P-001").getTema());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("Actualizar una pregunta que no existe devuelve false")
    void actualizarInexistente() {
        assertFalse(repository.update(pregunta("P-999", "autor1")));
    }
}
