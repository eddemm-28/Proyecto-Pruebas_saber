package co.edu.unicauca.saberpro.simulations.domain.scoring;

import co.edu.unicauca.saberpro.simulations.domain.model.CompetenceScore;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RF-24: calificación automática. Puntaje = aciertos / preguntas × 100 (2 decimales), global y
 * por competencia. Las competencias se listan en el orden en que aparecen en el simulacro.
 */
public class ScoringPolicy {

    public record Result(int correctCount, double globalScore, List<CompetenceScore> byCompetence) {
    }

    public Result score(List<SimulationItem> items) {
        int total = items.size();
        int correct = (int) items.stream().filter(i -> Boolean.TRUE.equals(i.getCorrect())).count();

        Map<String, int[]> byCompetence = new LinkedHashMap<>();
        for (SimulationItem item : items) {
            int[] acc = byCompetence.computeIfAbsent(item.getCompetence(), k -> new int[2]);
            acc[0]++;
            if (Boolean.TRUE.equals(item.getCorrect())) {
                acc[1]++;
            }
        }
        List<CompetenceScore> scores = new ArrayList<>();
        byCompetence.forEach((competence, acc) ->
                scores.add(new CompetenceScore(competence, acc[0], acc[1], percentage(acc[1], acc[0]))));
        return new Result(correct, percentage(correct, total), scores);
    }

    static double percentage(int correct, int total) {
        if (total == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
