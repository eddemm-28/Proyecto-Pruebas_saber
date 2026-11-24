package co.edu.unicauca.saberpro.simulations.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Simulacro en curso tal como lo ve el estudiante: <b>sin</b> respuestas correctas.
 * {@code serverTime} y {@code remainingSeconds} permiten sincronizar el cronómetro del cliente.
 */
public record SimulationView(
        UUID id,
        String status,
        String strategy,
        Instant startedAt,
        Instant expiresAt,
        Instant serverTime,
        long remainingSeconds,
        int questionCount,
        int answeredCount,
        List<Item> items) {

    public record Item(int position, UUID questionId, String code, String competence, String context,
                       String statement, List<Option> options, String selectedOption) {
    }

    public record Option(String label, String text) {
    }
}
