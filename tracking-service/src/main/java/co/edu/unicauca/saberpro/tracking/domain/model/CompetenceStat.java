package co.edu.unicauca.saberpro.tracking.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/**
 * Estadística acumulada de un estudiante en una competencia (RF-26). El promedio es ponderado
 * por número de preguntas (aciertos totales / preguntas totales × 100), no promedio de promedios.
 */
public record CompetenceStat(
        UUID studentId,
        String studentLogin,
        String competence,
        int simulationsCount,
        int totalQuestions,
        int correctAnswers,
        double averageScore,
        double lastScore,
        Classification classification,
        Instant updatedAt) {

    public static CompetenceStat empty(UUID studentId, String login, String competence) {
        return new CompetenceStat(studentId, login, competence, 0, 0, 0, 0.0, 0.0, Classification.EN_DESARROLLO, null);
    }

    /** Suma el resultado de un simulacro y reclasifica. */
    public CompetenceStat plus(SimulationResult.CompetenceResult r, String login, Instant at, ClassificationPolicy policy) {
        int questions = totalQuestions + r.totalQuestions();
        int correct = correctAnswers + r.correctAnswers();
        double average = percentage(correct, questions);
        return new CompetenceStat(studentId, login == null ? studentLogin : login, competence,
                simulationsCount + 1, questions, correct, average, r.score(), policy.classify(average, questions), at);
    }

    static double percentage(int correct, int total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
