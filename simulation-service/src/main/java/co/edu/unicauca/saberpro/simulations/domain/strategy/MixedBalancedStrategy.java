package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.exception.InsufficientQuestionsException;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Simulacro "tipo prueba real": reparte las preguntas lo más parejo posible entre las
 * competencias disponibles (round-robin sobre listas barajadas). Admite filtro de dificultad.
 */
public class MixedBalancedStrategy implements QuestionSelectionStrategy {

    @Override
    public StrategyType type() {
        return StrategyType.MIXED;
    }

    @Override
    public CandidateCriteria criteria(SimulationConfig config) {
        return new CandidateCriteria(null, null, config.difficulty());
    }

    @Override
    public List<QuestionSnapshot> select(List<QuestionSnapshot> candidates, SimulationConfig config, RandomGenerator random) {
        List<QuestionSnapshot> pool = candidates.stream()
                .filter(QuestionSnapshot::available)
                .filter(q -> config.difficulty() == null || config.difficulty().equalsIgnoreCase(q.difficulty()))
                .toList();
        int n = config.questionCount();
        if (pool.size() < n) {
            throw new InsufficientQuestionsException(pool.size(), n);
        }

        Map<String, List<QuestionSnapshot>> byCompetence = pool.stream()
                .sorted(Comparator.comparing(q -> String.valueOf(q.competence())))
                .collect(Collectors.groupingBy(q -> String.valueOf(q.competence()), LinkedHashMap::new, Collectors.toList()));
        List<List<QuestionSnapshot>> buckets = new ArrayList<>();
        for (List<QuestionSnapshot> bucket : byCompetence.values()) {
            List<QuestionSnapshot> shuffled = new ArrayList<>(bucket);
            shuffle(shuffled, random);
            buckets.add(shuffled);
        }
        shuffle(buckets, random); // qué competencia empieza también es aleatorio

        List<QuestionSnapshot> result = new ArrayList<>(n);
        int[] cursor = new int[buckets.size()];
        while (result.size() < n) {
            for (int b = 0; b < buckets.size() && result.size() < n; b++) {
                if (cursor[b] < buckets.get(b).size()) {
                    result.add(buckets.get(b).get(cursor[b]++));
                }
            }
        }
        shuffle(result, random); // el orden final no agrupa por competencia
        return List.copyOf(result);
    }

    private static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }
}
