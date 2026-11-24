package co.edu.unicauca.saberpro.tracking.application.service;

import co.edu.unicauca.saberpro.tracking.application.port.in.RegisterSimulationResult;
import co.edu.unicauca.saberpro.tracking.application.port.in.TrackingQueries;
import co.edu.unicauca.saberpro.tracking.application.port.out.CompetenceStatsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.tracking.application.port.out.ReportsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.SimulationResultRepository;
import co.edu.unicauca.saberpro.tracking.domain.model.CompetenceStat;
import co.edu.unicauca.saberpro.tracking.domain.model.Reports;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics;
import co.edu.unicauca.saberpro.tracking.domain.service.StatisticsCalculator;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Casos de uso del seguimiento académico (B10–B13). Sin dependencias de framework. */
public class TrackingService implements RegisterSimulationResult, TrackingQueries {

    private final SimulationResultRepository results;
    private final CompetenceStatsRepository stats;
    private final ReportsRepository reports;
    private final ProcessedEventStore processed;
    private final StatisticsCalculator calculator;
    private final Clock clock;

    public TrackingService(SimulationResultRepository results, CompetenceStatsRepository stats,
                           ReportsRepository reports, ProcessedEventStore processed,
                           StatisticsCalculator calculator, Clock clock) {
        this.results = results;
        this.stats = stats;
        this.reports = reports;
        this.processed = processed;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Override
    public boolean register(UUID eventId, SimulationResult result) {
        if (!processed.markProcessed(eventId, "SimulacroFinalizado")) {
            return false;
        }
        if (results.exists(result.simulationId())) {
            return false; // mismo simulacro reenviado con otro eventId
        }
        results.save(result);
        List<CompetenceStat> updated = calculator.apply(result, stats.findByStudent(result.studentId()), clock.instant());
        stats.saveAll(updated);
        return true;
    }

    @Override
    public HistoryPage history(UUID studentId, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.min(Math.max(size, 1), 100);
        long total = results.countByStudent(studentId);
        return new HistoryPage(results.findByStudent(studentId, p, s), p, s, total, (int) Math.ceil(total / (double) s));
    }

    @Override
    public StudentStatistics statistics(UUID studentId) {
        List<SimulationResult> history = results.findAllByStudent(studentId);
        return calculator.summarize(studentId, null, history, stats.findByStudent(studentId));
    }

    @Override
    public List<Reports.CompetenceRow> competenceReport(Reports.Period period) {
        return reports.byCompetence(period).stream()
                .sorted(Comparator.comparingDouble(Reports.CompetenceRow::averageScore)).toList();
    }

    @Override
    public List<Reports.StudentRow> studentReport(Reports.Period period) {
        return reports.byStudent(period).stream()
                .sorted(Comparator.comparingDouble(Reports.StudentRow::averageScore).reversed()
                        .thenComparing(Reports.StudentRow::studentLogin, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public List<Reports.TopicRow> topicReport(Reports.Period period, int limit) {
        return reports.byTopic(period).stream()
                .sorted(Comparator.comparingDouble(Reports.TopicRow::accuracy).thenComparing(Reports.TopicRow::topic))
                .limit(Math.max(1, Math.min(limit, 200)))
                .toList();
    }
}
