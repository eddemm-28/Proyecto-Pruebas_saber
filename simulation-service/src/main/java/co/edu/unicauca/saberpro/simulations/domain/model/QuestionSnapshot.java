package co.edu.unicauca.saberpro.simulations.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Copia local (réplica) de una pregunta publicada, alimentada por eventos. Es todo lo que el
 * simulador necesita para presentar y calificar sin consultar al question-service.
 */
public record QuestionSnapshot(
        UUID questionId,
        String code,
        String context,
        String statement,
        List<Option> options,
        String correctOption,
        String justification,
        String competence,
        String topic,
        String subtopic,
        String difficulty,
        long sourceVersion,
        boolean available,
        Instant publishedAt) {

    public record Option(String label, String text) {
    }

    public QuestionSnapshot {
        Objects.requireNonNull(questionId, "questionId");
        options = options == null ? List.of() : List.copyOf(options);
    }

    public QuestionSnapshot archived(long version) {
        return new QuestionSnapshot(questionId, code, context, statement, options, correctOption, justification,
                competence, topic, subtopic, difficulty, Math.max(version, sourceVersion), false, publishedAt);
    }
}
