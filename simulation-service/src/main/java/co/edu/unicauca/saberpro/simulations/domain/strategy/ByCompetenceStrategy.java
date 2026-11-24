package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.List;
import java.util.random.RandomGenerator;

/** Preguntas aleatorias de una competencia (opcionalmente de una dificultad). */
public class ByCompetenceStrategy implements QuestionSelectionStrategy {

    @Override
    public StrategyType type() {
        return StrategyType.COMPETENCE;
    }

    @Override
    public CandidateCriteria criteria(SimulationConfig config) {
        return new CandidateCriteria(config.competence(), null, config.difficulty());
    }

    @Override
    public List<QuestionSnapshot> select(List<QuestionSnapshot> candidates, SimulationConfig config, RandomGenerator random) {
        List<QuestionSnapshot> filtered = candidates.stream()
                .filter(q -> q.competence() != null && q.competence().equalsIgnoreCase(config.competence()))
                .filter(q -> config.difficulty() == null || config.difficulty().equalsIgnoreCase(q.difficulty()))
                .toList();
        return RandomSampling.sample(filtered, config.questionCount(), random);
    }
}
