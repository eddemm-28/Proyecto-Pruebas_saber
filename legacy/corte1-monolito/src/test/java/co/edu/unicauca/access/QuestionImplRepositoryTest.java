package co.edu.unicauca.access;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import co.edu.unicauca.domain.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionImplRepositoryTest {

    private QuestionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new QuestionImplRepository();
    }

    @Test
    @DisplayName("Debe inicializarse con preguntas por defecto")
    void testInitialData() {
        List<Question> questions = repository.findAll();
        assertNotNull(questions);
        assertEquals(5, questions.size());
    }

    @Test
    @DisplayName("Debe permitir guardar una nueva pregunta")
    void testSaveQuestion() {
        Question newQuestion = new Question(
                "P-010",
                "Nueva Pregunta",
                "¿Enunciado nuevo?",
                new QuestionDistractors("A", "B", "C", "D"),
                "A",
                Question.STATE_BORRADOR
        );

        boolean saved = repository.save(newQuestion);
        assertTrue(saved);

        Question retrieved = repository.findById("P-010");
        assertNotNull(retrieved);
        assertEquals("Nueva Pregunta", retrieved.getNombre());
    }

    @Test
    @DisplayName("Debe actualizar el estado de una pregunta")
    void testUpdateState() {
        boolean updated = repository.updateState("P-001", Question.STATE_ELIMINADA);
        assertTrue(updated);

        Question q = repository.findById("P-001");
        assertEquals(Question.STATE_ELIMINADA, q.getEstado());
    }
}
