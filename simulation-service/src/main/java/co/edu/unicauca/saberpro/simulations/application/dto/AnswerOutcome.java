package co.edu.unicauca.saberpro.simulations.application.dto;

/**
 * Resultado de responder. Si el tiempo ya venció, {@code accepted = false}: el simulacro se
 * cerró como EXPIRED en la misma operación (no se lanza excepción para no deshacer el cierre).
 */
public record AnswerOutcome(boolean accepted, String status, long remainingSeconds, String message) {
}
