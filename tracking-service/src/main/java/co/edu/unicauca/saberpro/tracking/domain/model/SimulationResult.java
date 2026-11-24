package co.edu.unicauca.saberpro.tracking.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Resultado de un simulacro recibido por evento (RF-25). Inmutable. */
public record SimulationResult(
        UUID simulationId,
        UUID studentId,
        String studentLogin,
        String finalStatus,
        String strategy,
        Instant startedAt,
        Instant finishedAt,
        int totalQuestions,
        int correctAnswers,
        double globalScore,
        List<CompetenceResult> competenceScores,
        List<AnswerResult> answers) {

    public record CompetenceResult(String competence, int totalQuestions, int correctAnswers, double score) {
    }

    public record AnswerResult(UUID questionId, String competence, String topic, String difficulty,
                               boolean answered, boolean correct) {
    }

    public SimulationResult {
        Objects.requireNonNull(simulationId, "simulationId");
        Objects.requireNonNull(studentId, "studentId");
        Objects.requireNonNull(finishedAt, "finishedAt");
        competenceScores = competenceScores == null ? List.of() : List.copyOf(competenceScores);
        answers = answers == null ? List.of() : List.copyOf(answers);
    }

    public long durationSeconds() {
        return startedAt == null ? 0 : Math.max(0, Duration.between(startedAt, finishedAt).getSeconds());
    }
}
