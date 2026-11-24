package co.edu.unicauca.saberpro.simulations.domain.model;

/** Estrategias de generación de simulacros (RF-22, patrón Strategy). */
public enum StrategyType {
    /** Todas las preguntas de una competencia. */
    COMPETENCE,
    /** Todas las preguntas de un tema. */
    TOPIC,
    /** Todas las preguntas de una dificultad. */
    DIFFICULTY,
    /** Mezcla balanceada entre competencias (tipo prueba real); admite filtro opcional de dificultad. */
    MIXED
}
