package co.edu.unicauca.domain;

import co.edu.unicauca.access.QuestionImplRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QuestionServiceTest {

    private QuestionRepository repository;
    private QuestionService service;

    @BeforeEach
    void setUp() {
        repository = new QuestionImplRepository();
        service = new QuestionService(repository);
    }

    @Test
    @DisplayName("Debe obtener todas las preguntas del repositorio")
    void testGetAllQuestions() {
        List<Question> questions = service.getAllQuestions();
        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        assertTrue(questions.size() >= 5);
    }

    @Test
    @DisplayName("Debe buscar una pregunta por su ID")
    void testGetQuestionById() {
        Question q = service.getQuestionById("P-001");
        assertNotNull(q);
        assertEquals("P-001", q.getId());
        assertEquals("Pregunta sobre DDD", q.getNombre());
    }

    @Test
    @DisplayName("Debe actualizar el estado de una pregunta existente")
    void testUpdateQuestionState() {
        boolean result = service.updateQuestionState("P-001", Question.STATE_ELIMINADA);
        assertTrue(result);

        Question updated = service.getQuestionById("P-001");
        assertEquals(Question.STATE_ELIMINADA, updated.getEstado());
    }

    @Test
    @DisplayName("Debe retornar false al intentar actualizar una pregunta inexistente")
    void testUpdateNonExistingQuestionState() {
        boolean result = service.updateQuestionState("P-999", Question.STATE_ELIMINADA);
        assertFalse(result);
    }

    @Test
    @DisplayName("Debe calcular correctamente el conteo de preguntas por estado")
    void testGetQuestionCountByState() {
        Map<String, Integer> counts = service.getQuestionCountByState();
        assertNotNull(counts);
        assertTrue(counts.containsKey(Question.STATE_BORRADOR));
        assertTrue(counts.containsKey(Question.STATE_PENDIENTE_REVISION));
        assertTrue(counts.containsKey(Question.STATE_ELIMINADA));

        int totalCount = counts.get(Question.STATE_BORRADOR)
                + counts.get(Question.STATE_PENDIENTE_REVISION)
                + counts.get(Question.STATE_ELIMINADA);
        assertEquals(service.getTotalQuestionsCount(), totalCount);
    }

    @Test
    @DisplayName("Debe calcular los porcentajes de preguntas por estado")
    void testGetQuestionPercentageByState() {
        Map<String, Double> percentages = service.getQuestionPercentageByState();
        assertNotNull(percentages);
        assertTrue(percentages.containsKey(Question.STATE_BORRADOR));
        assertTrue(percentages.containsKey(Question.STATE_PENDIENTE_REVISION));
        assertTrue(percentages.containsKey(Question.STATE_ELIMINADA));

        double sum = percentages.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, sum, 1.0);
    }
}
