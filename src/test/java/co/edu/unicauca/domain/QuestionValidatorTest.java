package co.edu.unicauca.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionValidatorTest {

    private QuestionValidator validator;
    private Question q;

    @BeforeEach
    void setUp() {
        validator = new QuestionValidator();
        q = TestData.preguntaValida("autor1");
    }

    @Test
    @DisplayName("Una pregunta completa no tiene errores")
    void preguntaValida() {
        assertTrue(validator.validate(q).isEmpty());
    }

    @Test
    @DisplayName("Una pregunta nula es inválida")
    void preguntaNula() {
        assertFalse(validator.validate(null).isEmpty());
    }

    @Test
    @DisplayName("El título y el contexto son obligatorios")
    void tituloYContextoObligatorios() {
        q.setNombre("  ");
        q.setContexto(null);
        List<String> errores = validator.validate(q);
        assertEquals(2, errores.size());
    }

    @Test
    @DisplayName("El título no puede superar 100 caracteres")
    void tituloMuyLargo() {
        q.setNombre("x".repeat(101));
        assertEquals(1, validator.validate(q).size());
    }

    @Test
    @DisplayName("Las cuatro opciones son obligatorias")
    void opcionesObligatorias() {
        q.setDistractors(new QuestionDistractors("Uno", "", "Tres", null));
        assertEquals(2, validator.validate(q).size());
    }

    @Test
    @DisplayName("Las opciones no pueden repetirse")
    void opcionesRepetidas() {
        q.setDistractors(new QuestionDistractors("Igual", "igual", "Tres", "Cuatro"));
        List<String> errores = validator.validate(q);
        assertEquals(1, errores.size());
        assertTrue(errores.get(0).contains("repetida"));
    }

    @Test
    @DisplayName("La respuesta correcta debe ser A, B, C o D")
    void respuestaInvalida() {
        q.setRespuestaCorrecta("E");
        assertEquals(1, validator.validate(q).size());
        q.setRespuestaCorrecta("c");
        assertTrue(validator.validate(q).isEmpty());
    }

    @Test
    @DisplayName("Justificación, bibliografía, competencia, tema y subtema son obligatorios")
    void camposAcademicosObligatorios() {
        q.setJustificacion("");
        q.setBibliografia("");
        q.setCompetencia("");
        q.setTema("");
        q.setSubtema("");
        assertEquals(5, validator.validate(q).size());
    }

    @Test
    @DisplayName("La dificultad solo puede ser Fácil, Media o Difícil")
    void dificultadInvalida() {
        q.setDificultad("Imposible");
        assertEquals(1, validator.validate(q).size());
    }

    @Test
    @DisplayName("validateOrThrow lanza excepción con todos los errores")
    void validateOrThrow() {
        q.setTema("");
        q.setSubtema("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> validator.validateOrThrow(q));
        assertTrue(ex.getMessage().contains("tema"));
        assertTrue(ex.getMessage().contains("subtema"));
    }
}
