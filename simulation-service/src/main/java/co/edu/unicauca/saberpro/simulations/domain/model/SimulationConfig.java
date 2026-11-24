package co.edu.unicauca.saberpro.simulations.domain.model;

import co.edu.unicauca.saberpro.simulations.domain.exception.InvalidSimulationConfigException;

import java.util.ArrayList;
import java.util.List;

/** Configuración elegida por el estudiante (RF-21, RF-22, RF-23). */
public record SimulationConfig(
        StrategyType strategy,
        String competence,
        String topic,
        String difficulty,
        int questionCount,
        int timeLimitMinutes) {

    public static final int MIN_QUESTIONS = 1;
    public static final int MAX_QUESTIONS = 100;
    public static final int MIN_MINUTES = 1;
    public static final int MAX_MINUTES = 300;

    public SimulationConfig {
        competence = blankToNull(competence);
        topic = blankToNull(topic);
        difficulty = blankToNull(difficulty) == null ? null : difficulty.trim().toUpperCase();
    }

    public void validate() {
        List<String> errors = new ArrayList<>();
        if (strategy == null) {
            errors.add("Debe elegir una estrategia: COMPETENCE, TOPIC, DIFFICULTY o MIXED.");
        } else {
            switch (strategy) {
                case COMPETENCE -> { if (competence == null) errors.add("La estrategia por competencia requiere la competencia."); }
                case TOPIC -> { if (topic == null) errors.add("La estrategia por tema requiere el tema."); }
                case DIFFICULTY -> { if (difficulty == null) errors.add("La estrategia por dificultad requiere la dificultad."); }
                case MIXED -> { }
            }
        }
        if (difficulty != null && !List.of("FACIL", "MEDIA", "DIFICIL").contains(difficulty)) {
            errors.add("La dificultad debe ser FACIL, MEDIA o DIFICIL.");
        }
        if (questionCount < MIN_QUESTIONS || questionCount > MAX_QUESTIONS) {
            errors.add("El número de preguntas debe estar entre " + MIN_QUESTIONS + " y " + MAX_QUESTIONS + ".");
        }
        if (timeLimitMinutes < MIN_MINUTES || timeLimitMinutes > MAX_MINUTES) {
            errors.add("El tiempo máximo debe estar entre " + MIN_MINUTES + " y " + MAX_MINUTES + " minutos.");
        }
        if (!errors.isEmpty()) {
            throw new InvalidSimulationConfigException(errors);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
