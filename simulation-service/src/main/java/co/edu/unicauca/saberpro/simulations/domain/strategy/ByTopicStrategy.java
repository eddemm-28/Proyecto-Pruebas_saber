package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.List;
import java.util.random.RandomGenerator;

/** Preguntas aleatorias de un tema (opcionalmente acotado a competencia y dificultad). */
public class ByTopicStrategy implements QuestionSelectionStrategy {

    @Override
    public StrategyType type() {
        return StrategyType.TOPIC;
    }

    @Override
    public CandidateCriteria criteria(SimulationConfig config) {
        return new CandidateCriteria(config.competence(), config.topic(), config.difficulty());
    }

    @Override
    public List<QuestionSnapshot> select(List<QuestionSnapshot> candidates, SimulationConfig config, RandomGenerator random) {
        List<QuestionSnapshot> filtered = candidates.stream()
                .filter(q -> q.topic() != null && q.topic().equalsIgnoreCase(config.topic()))
                .filter(q -> config.competence() == null || config.competence().equalsIgnoreCase(q.competence()))
                .filter(q -> config.difficulty() == null || config.difficulty().equalsIgnoreCase(q.difficulty()))
                .toList();
        return RandomSampling.sample(filtered, config.questionCount(), random);
    }
}
