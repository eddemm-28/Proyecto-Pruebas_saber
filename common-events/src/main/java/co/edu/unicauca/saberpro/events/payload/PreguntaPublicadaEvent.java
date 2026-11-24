package co.edu.unicauca.saberpro.events.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Payload de {@code PreguntaPublicada} (v1). Trae todo lo que el simulador necesita para
 * evaluar sin volver a consultar al question-service.
 */
public record PreguntaPublicadaEvent(
        UUID questionId,
        String code,
        long version,
        String type,
        String context,
        String statement,
        List<OptionPayload> options,
        String correctOption,
        String justification,
        String competence,
        String topic,
        String subtopic,
        String difficulty,
        Instant publishedAt) {

    public record OptionPayload(String label, String text) {
    }
}
