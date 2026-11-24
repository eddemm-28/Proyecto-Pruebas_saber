package co.edu.unicauca.saberpro.questions.plugin.api;

import java.util.List;

/**
 * Entrada de un plugin. {@code correctAnswer} acepta la letra (A–D) o el texto exacto de la
 * opción; el núcleo lo normaliza siempre a letra (estandarización del Corte 1, A14).
 */
public record PluginRequest(
        String title,
        String context,
        String statement,
        List<String> options,
        String correctAnswer,
        String justification,
        String bibliography,
        String competence,
        String topic,
        String subtopic,
        String difficulty,
        String mediaUrl) {
}
