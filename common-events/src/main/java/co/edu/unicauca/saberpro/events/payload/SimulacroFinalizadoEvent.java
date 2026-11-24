package co.edu.unicauca.saberpro.events.payload;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Payload de {@code SimulacroFinalizado} (v1): resultado completo ya calificado. */
public record SimulacroFinalizadoEvent(
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
        List<CompetenceScorePayload> competenceScores,
        List<AnswerPayload> answers) {

    public record CompetenceScorePayload(String competence, int total, int correct, double score) {
    }

    public record AnswerPayload(UUID questionId, String competence, String topic, String difficulty,
                                String selectedOption, boolean correct) {
    }
}
