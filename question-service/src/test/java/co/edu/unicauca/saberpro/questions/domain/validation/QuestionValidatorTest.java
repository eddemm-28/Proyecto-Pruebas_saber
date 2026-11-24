package co.edu.unicauca.saberpro.questions.domain.validation;

import co.edu.unicauca.saberpro.questions.TestQuestions;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A15: reglas RF-08 a RF-13. */
class QuestionValidatorTest {

    private final QuestionValidator validator = new QuestionValidator();

    private boolean hasError(QuestionContent q, String fragment) {
        return validator.validate(q).stream().anyMatch(e -> e.contains(fragment));
    }

    @Test
    void preguntaValidaNoTieneErrores() {
        assertEquals(List.of(), validator.validate(TestQuestions.valid()));
    }

    @Test
    void rf08ContextoObligatorio() {
        assertTrue(hasError(TestQuestions.withContext(QuestionType.MULTIPLE_CHOICE, "", null), "RF-08"));
        assertTrue(hasError(TestQuestions.withContext(QuestionType.MULTIPLE_CHOICE, "Muy corto.", null), "RF-08"));
    }

    @Test
    void rf09UnaSolaPregunta() {
        assertTrue(hasError(TestQuestions.withStatement("¿Qué es State? ¿Y qué es Strategy?"), "RF-09"));
        assertFalse(hasError(TestQuestions.valid(), "RF-09"));
    }

    @Test
    void rf10CuatroOpcionesDistintas() {
        assertTrue(hasError(TestQuestions.withOptions(List.of("A", "B", "C"), "A"), "RF-10"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "dos", "Cuatro"), "A"), "repetida"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "", "Tres", "Cuatro"), "A"), "opción B es obligatoria"));
    }

    @Test
    void rf11RespuestaCorrectaUnicaComoLetra() {
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "Tres", "Cuatro"), "E"), "RF-11"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "Tres", "Cuatro"), "A,B"), "RF-11"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "Tres", "Cuatro"), null), "RF-11"));
    }

    @Test
    void rf12BloqueaTodasNingunaDeLasAnteriores() {
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "Tres", "Todas las anteriores"), "A"), "RF-12"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "Ninguna de las anteriores.", "Cuatro"), "A"), "RF-12"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "Dos", "NINGUNA DE LAS ANTERIORES", "Cuatro"), "A"), "RF-12"));
        assertTrue(hasError(TestQuestions.withOptions(List.of("Uno", "A y B son correctas", "Tres", "Cuatro"), "A"), "RF-12"));
        // Falso positivo que no se debe bloquear
        assertFalse(hasError(TestQuestions.withOptions(
                List.of("Revisar todas las opciones de configuración del servidor", "Dos", "Tres", "Cuatro"), "A"), "RF-12"));
    }

    @Test
    void rf13GramaticaBasica() {
        assertTrue(hasError(TestQuestions.withStatement("¿cuál patrón aplica en este caso?"), "mayúscula"));
        assertTrue(hasError(TestQuestions.withStatement("Cuál patrón aplica en este caso?"), "¿"));
        assertTrue(hasError(TestQuestions.withStatement("¿Cuál es es el patrón correcto?"), "repite"));
        assertTrue(hasError(TestQuestions.withStatement("¿Cuál  patrón aplica aquí?"), "espacios dobles"));
    }

    @Test
    void pluginCasoYMultimedia() {
        assertTrue(hasError(TestQuestions.withContext(QuestionType.CASE,
                "Un contexto mediano de unos cincuenta caracteres aprox.", null), "caso"));
        assertTrue(hasError(TestQuestions.withContext(QuestionType.MULTIMEDIA, TestQuestions.valid().context(), null), "URL"));
        assertTrue(hasError(TestQuestions.withContext(QuestionType.MULTIMEDIA, TestQuestions.valid().context(), "ftp://x"), "no es válida"));
        assertEquals(List.of(), validator.validate(TestQuestions.withContext(QuestionType.MULTIMEDIA,
                TestQuestions.valid().context(), "https://cdn.unicauca.edu.co/img/diagrama.png")));
    }

    @Test
    void validateOrThrowLanzaConTodosLosErrores() {
        QuestionValidationException ex = assertThrows(QuestionValidationException.class,
                () -> validator.validateOrThrow(TestQuestions.withOptions(List.of("x", "x"), "Z")));
        assertTrue(ex.getErrors().size() >= 2);
    }
}
