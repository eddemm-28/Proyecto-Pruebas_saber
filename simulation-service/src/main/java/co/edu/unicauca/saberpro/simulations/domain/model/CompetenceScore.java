package co.edu.unicauca.saberpro.simulations.domain.model;

/** Puntaje de una competencia en un simulacro (RF-24). {@code score} en escala 0–100. */
public record CompetenceScore(String competence, int totalQuestions, int correctAnswers, double score) {
}
