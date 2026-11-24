package co.edu.unicauca.saberpro.simulations.domain.strategy;

import co.edu.unicauca.saberpro.simulations.domain.exception.InsufficientQuestionsException;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/** Muestreo aleatorio sin reemplazo (Fisher–Yates parcial). */
final class RandomSampling {

    private RandomSampling() {
    }

    static List<QuestionSnapshot> sample(List<QuestionSnapshot> pool, int n, RandomGenerator random) {
        List<QuestionSnapshot> available = pool.stream().filter(QuestionSnapshot::available).toList();
        if (available.size() < n) {
            throw new InsufficientQuestionsException(available.size(), n);
        }
        List<QuestionSnapshot> copy = new ArrayList<>(available);
        for (int i = 0; i < n; i++) {
            int j = i + random.nextInt(copy.size() - i);
            QuestionSnapshot tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return List.copyOf(copy.subList(0, n));
    }
}
