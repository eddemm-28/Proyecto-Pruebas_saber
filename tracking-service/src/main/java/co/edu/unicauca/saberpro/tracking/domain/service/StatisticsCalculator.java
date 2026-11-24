package co.edu.unicauca.saberpro.tracking.domain.service;

import co.edu.unicauca.saberpro.tracking.domain.model.Classification;
import co.edu.unicauca.saberpro.tracking.domain.model.ClassificationPolicy;
import co.edu.unicauca.saberpro.tracking.domain.model.CompetenceStat;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics.Trend;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics.TrendPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Servicio de dominio puro: cálculo de estadísticas, fortalezas y debilidades (B12). */
public class StatisticsCalculator {

    static final int RECENT = 10;
    static final double TREND_DELTA = 5.0;

    private final ClassificationPolicy policy;

    public StatisticsCalculator(ClassificationPolicy policy) {
        this.policy = policy;
    }

    /** Aplica un resultado nuevo sobre las estadísticas existentes del estudiante. */
    public List<CompetenceStat> apply(SimulationResult result, List<CompetenceStat> current, Instant now) {
        Map<String, CompetenceStat> byCompetence = current.stream()
                .collect(Collectors.toMap(c -> c.competence().toLowerCase(), Function.identity(), (a, b) -> a));
        List<CompetenceStat> updated = new ArrayList<>();
        for (SimulationResult.CompetenceResult r : result.competenceScores()) {
            CompetenceStat base = byCompetence.getOrDefault(r.competence().toLowerCase(),
                    CompetenceStat.empty(result.studentId(), result.studentLogin(), r.competence()));
            updated.add(base.plus(r, result.studentLogin(), now, policy));
        }
        return updated;
    }

    /** Arma el panel del estudiante a partir de su historial (cualquier orden) y sus estadísticas. */
    public StudentStatistics summarize(UUID studentId, String login, List<SimulationResult> history,
                                       List<CompetenceStat> competences) {
        List<SimulationResult> ordered = history.stream()
                .sorted(Comparator.comparing(SimulationResult::finishedAt)).toList();
        int n = ordered.size();
        double avg = n == 0 ? 0 : round(ordered.stream().mapToDouble(SimulationResult::globalScore).average().orElse(0));
        double best = ordered.stream().mapToDouble(SimulationResult::globalScore).max().orElse(0);
        double last = n == 0 ? 0 : ordered.get(n - 1).globalScore();

        List<TrendPoint> recent = ordered.subList(Math.max(0, n - RECENT), n).stream()
                .map(r -> new TrendPoint(r.simulationId(), r.finishedAt(), r.globalScore())).toList();

        List<CompetenceStat> sorted = competences.stream()
                .sorted(Comparator.comparingDouble(CompetenceStat::averageScore).reversed()
                        .thenComparing(CompetenceStat::competence))
                .toList();
        List<String> strengths = sorted.stream().filter(c -> c.classification() == Classification.FORTALEZA)
                .map(CompetenceStat::competence).toList();
        List<String> weaknesses = sorted.stream().filter(c -> c.classification() == Classification.DEBILIDAD)
                .sorted(Comparator.comparingDouble(CompetenceStat::averageScore))
                .map(CompetenceStat::competence).toList();

        String resolvedLogin = login != null ? login : (n == 0 ? null : ordered.get(n - 1).studentLogin());
        return new StudentStatistics(studentId, resolvedLogin, n, avg, best, last, trend(ordered), recent, sorted,
                strengths, weaknesses);
    }

    /** Compara el promedio de los 3 últimos simulacros con el de los 3 anteriores. */
    static Trend trend(List<SimulationResult> ordered) {
        int n = ordered.size();
        if (n < 2) {
            return Trend.SIN_DATOS;
        }
        int window = Math.min(3, n / 2);
        double recent = ordered.subList(n - window, n).stream().mapToDouble(SimulationResult::globalScore).average().orElse(0);
        double previous = ordered.subList(n - 2 * window, n - window).stream().mapToDouble(SimulationResult::globalScore).average().orElse(0);
        if (recent - previous > TREND_DELTA) return Trend.MEJORANDO;
        if (previous - recent > TREND_DELTA) return Trend.BAJANDO;
        return Trend.ESTABLE;
    }

    static double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
