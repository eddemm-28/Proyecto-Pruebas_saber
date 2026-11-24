package co.edu.unicauca.saberpro.tracking.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Filas de los reportes agregados para docentes y coordinadores (RF-28). */
public final class Reports {

    private Reports() {
    }

    public record CompetenceRow(String competence, long students, long simulations, long totalQuestions,
                                long correctAnswers, double averageScore) {
    }

    public record StudentRow(UUID studentId, String studentLogin, long simulations, double averageScore,
                             double bestScore, Instant lastFinishedAt) {
    }

    public record TopicRow(String competence, String topic, long totalAnswers, long correctAnswers, double accuracy) {
    }

    public record Period(Instant from, Instant to) {
        public Period {
            if (from != null && to != null && from.isAfter(to)) {
                throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final");
            }
        }
    }
}
