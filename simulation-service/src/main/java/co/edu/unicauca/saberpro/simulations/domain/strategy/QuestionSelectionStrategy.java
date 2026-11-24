package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Patrón Strategy (B06, RF-21, RF-22): cada estrategia decide (1) qué filtro pedirle al
 * repositorio de la réplica y (2) cómo escoger aleatoriamente las preguntas del simulacro.
 */
public interface QuestionSelectionStrategy {

    StrategyType type();

    /** Criterios para pedir candidatas a la réplica local (nulos = sin filtro). */
    CandidateCriteria criteria(SimulationConfig config);

    /**
     * Escoge {@code config.questionCount()} preguntas del conjunto de candidatas.
     *
     * @throws co.edu.unicauca.saberpro.simulations.domain.exception.InsufficientQuestionsException
     *         si no hay suficientes
     */
    List<QuestionSnapshot> select(List<QuestionSnapshot> candidates, SimulationConfig config, RandomGenerator random);

    record CandidateCriteria(String competence, String topic, String difficulty) {
    }
}
