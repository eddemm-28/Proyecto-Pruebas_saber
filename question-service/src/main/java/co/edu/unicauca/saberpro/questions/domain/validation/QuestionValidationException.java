package co.edu.unicauca.saberpro.questions.domain.validation;

import java.util.List;

/** Errores de validación estructural → HTTP 422 con la lista completa. */
public class QuestionValidationException extends RuntimeException {

    private final List<String> errors;

    public QuestionValidationException(List<String> errors) {
        super("La pregunta no cumple las reglas estructurales: " + errors.size() + " error(es)");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
