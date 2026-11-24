package co.edu.unicauca.saberpro.questions.domain.validation;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador compuesto (A15): ejecuta todas las reglas y acumula los errores para mostrarlos
 * juntos en el cliente. Se pueden agregar reglas nuevas sin tocar las existentes (OCP).
 */
public class QuestionValidator {

    private final List<QuestionRule> rules;

    public QuestionValidator() {
        this(List.of(
                Rules.REQUIRED_FIELDS,
                Rules.CONTEXT_REQUIRED,
                Rules.SINGLE_QUESTION,
                Rules.FOUR_OPTIONS,
                Rules.SINGLE_CORRECT_ANSWER,
                Rules.NO_ALL_OR_NONE,
                Rules.BASIC_GRAMMAR,
                Rules.CASE_PLUGIN,
                Rules.MULTIMEDIA_PLUGIN));
    }

    public QuestionValidator(List<QuestionRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<String> validate(QuestionContent q) {
        List<String> errors = new ArrayList<>();
        if (q == null) {
            errors.add("La pregunta no puede ser nula.");
            return errors;
        }
        for (QuestionRule rule : rules) {
            rule.check(q, errors);
        }
        return errors;
    }

    public void validateOrThrow(QuestionContent q) {
        List<String> errors = validate(q);
        if (!errors.isEmpty()) {
            throw new QuestionValidationException(errors);
        }
    }
}
