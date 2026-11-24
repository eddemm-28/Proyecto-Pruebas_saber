package co.edu.unicauca.saberpro.tracking.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Panel de estadísticas de un estudiante (RF-26, RF-27). */
public record StudentStatistics(
        UUID studentId,
        String studentLogin,
        int simulationsCount,
        double averageGlobalScore,
        double bestGlobalScore,
        double lastGlobalScore,
        Trend trend,
        List<TrendPoint> recentScores,
        List<CompetenceStat> competences,
        List<String> strengths,
        List<String> weaknesses) {

    public enum Trend { MEJORANDO, ESTABLE, BAJANDO, SIN_DATOS }

    public record TrendPoint(UUID simulationId, Instant finishedAt, double globalScore) {
    }
}
