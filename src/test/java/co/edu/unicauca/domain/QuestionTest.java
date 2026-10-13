package co.edu.unicauca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private Question question;
    private QuestionDistractors distractors;

    @BeforeEach
    void setUp() {
        distractors = new QuestionDistractors("Op A", "Op B", "Op C", "Op D");
        question = new Question(
                "P-100",
                "Pregunta Test",
                "¿Texto de prueba?",
                distractors,
                "A",
                Question.STATE_BORRADOR
        );
    }

    @Test
    @DisplayName("Debe crear una pregunta con sus valores correctos")
    void testQuestionCreation() {
        assertEquals("P-100", question.getId());
        assertEquals("Pregunta Test", question.getNombre());
        assertEquals("¿Texto de prueba?", question.getPregunta());
        assertEquals("A", question.getRespuestaCorrecta());
        assertEquals(Question.STATE_BORRADOR, question.getEstado());
        assertNotNull(question.getDistractors());
    }

    @Test
    @DisplayName("Debe formatear correctamente el texto de los distractores")
    void testDistractorsFormatting() {
        String formatted = distractors.getFormattedText();
        assertTrue(formatted.contains("A. Op A"));
        assertTrue(formatted.contains("B. Op B"));
        assertTrue(formatted.contains("C. Op C"));
        assertTrue(formatted.contains("D. Op D"));
    }

    @Test
    @DisplayName("Debe permitir cambiar el estado de la pregunta")
    void testSetEstado() {
        question.setEstado(Question.STATE_PENDIENTE_REVISION);
        assertEquals(Question.STATE_PENDIENTE_REVISION, question.getEstado());

        question.setEstado(Question.STATE_ELIMINADA);
        assertEquals(Question.STATE_ELIMINADA, question.getEstado());
    }

    @Test
    @DisplayName("Debe retornar representación textual legible en toString")
    void testToString() {
        assertEquals("P-100 - Pregunta Test", question.toString());
    }
}
