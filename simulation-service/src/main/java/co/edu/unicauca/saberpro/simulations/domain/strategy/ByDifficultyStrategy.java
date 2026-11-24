package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.List;
import java.util.random.RandomGenerator;

/** Preguntas aleatorias de una dificultad, de cualquier competencia. */
public class ByDifficultyStrategy implements QuestionSelectionStrategy {

    @Override
    public StrategyType type() {
        return StrategyType.DIFFICULTY;
    }

    @Override
    public CandidateCriteria criteria(SimulationConfig config) {
        return new CandidateCriteria(null, null, config.difficulty());
    }

    @Override
    public List<QuestionSnapshot> select(List<QuestionSnapshot> candidates, SimulationConfig config, RandomGenerator random) {
        List<QuestionSnapshot> filtered = candidates.stream()
                .filter(q -> config.difficulty().equalsIgnoreCase(q.difficulty()))
                .toList();
        return RandomSampling.sample(filtered, config.questionCount(), random);
    }
}
