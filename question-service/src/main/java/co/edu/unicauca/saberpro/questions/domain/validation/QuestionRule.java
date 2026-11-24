package co.edu.unicauca.saberpro.questions.domain.validation;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;

import java.util.List;

/** Una regla de validación estructural (A15). Agrega mensajes a {@code errors}; nunca lanza. */
@FunctionalInterface
public interface QuestionRule {

    void check(QuestionContent q, List<String> errors);

    static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
