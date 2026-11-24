package co.edu.unicauca.saberpro.simulations.application.dto;

/** Cuántas preguntas disponibles hay por competencia/tema/dificultad (para configurar el simulacro). */
public record CatalogEntry(String competence, String topic, String difficulty, Long available) {
}
