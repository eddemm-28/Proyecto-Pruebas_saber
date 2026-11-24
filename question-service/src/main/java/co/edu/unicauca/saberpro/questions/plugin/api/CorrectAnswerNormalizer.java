package co.edu.unicauca.saberpro.questions.plugin.api;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;

import java.util.List;

/**
 * A14: en el Corte 1 el formulario guardaba la letra (A–D) y el plugin de selección múltiple
 * guardaba el texto de la opción. Ahora todo se guarda como letra.
 */
public final class CorrectAnswerNormalizer {

    private CorrectAnswerNormalizer() {
    }

    /** @return la letra A–D o {@code null} si no corresponde a ninguna opción */
    public static String toLabel(String answer, List<String> options) {
        if (answer == null || answer.isBlank()) {
            return null;
        }
        String trimmed = answer.trim();
        String upper = trimmed.toUpperCase();
        if (upper.length() == 1 && QuestionContent.LABELS.contains(upper)) {
            return upper;
        }
        if (options != null) {
            for (int i = 0; i < Math.min(options.size(), 4); i++) {
                String opt = options.get(i);
                if (opt != null && opt.trim().equalsIgnoreCase(trimmed)) {
                    return QuestionContent.LABELS.get(i);
                }
            }
        }
        return null;
    }
}
