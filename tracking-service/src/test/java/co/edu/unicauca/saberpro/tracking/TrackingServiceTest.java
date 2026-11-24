package co.edu.unicauca.saberpro.tracking;

import co.edu.unicauca.saberpro.tracking.application.port.in.TrackingQueries;
import co.edu.unicauca.saberpro.tracking.application.port.out.CompetenceStatsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.tracking.application.port.out.ReportsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.SimulationResultRepository;
import co.edu.unicauca.saberpro.tracking.application.service.TrackingService;
import co.edu.unicauca.saberpro.tracking.domain.model.Classification;
import co.edu.unicauca.saberpro.tracking.domain.model.ClassificationPolicy;
import co.edu.unicauca.saberpro.tracking.domain.model.CompetenceStat;
import co.edu.unicauca.saberpro.tracking.domain.model.Reports;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult.CompetenceResult;
import co.edu.unicauca.saberpro.tracking.domain.model.StudentStatistics;
import co.edu.unicauca.saberpro.tracking.domain.service.StatisticsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B10–B13 con adaptadores en memoria. */
class TrackingServiceTest {

    // ------------------------------------------------------------ dobles de los puertos
    static final class Results implements SimulationResultRepository {
        final List<SimulationResult> store = new ArrayList<>();
        public boolean exists(UUID id) { return store.stream().anyMatch(r -> r.simulationId().equals(id)); }
        public void save(SimulationResult r) { store.add(r); }
        public List<SimulationResult> findByStudent(UUID s, int page, int size) {
            return store.stream().filter(r -> r.studentId().equals(s))
                    .sorted(Comparator.comparing(SimulationResult::finishedAt).reversed())
                    .skip((long) page * size).limit(size).toList();
        }
        public long countByStudent(UUID s) { return store.stream().filter(r -> r.studentId().equals(s)).count(); }
        public List<SimulationResult> findAllByStudent(UUID s) { return store.stream().filter(r -> r.studentId().equals(s)).toList(); }
    }

    static final class Stats implements CompetenceStatsRepository {
        final Map<String, CompetenceStat> store = new HashMap<>();
        public List<CompetenceStat> findByStudent(UUID s) {
            return store.values().stream().filter(c -> c.studentId().equals(s)).toList();
        }
        public void saveAll(List<CompetenceStat> list) {
            list.forEach(c -> store.put(c.studentId() + "|" + c.competence(), c));
        }
    }

    static final class Processed implements ProcessedEventStore {
        final Set<UUID> ids = new HashSet<>();
        public boolean markProcessed(UUID id, String type) { return ids.add(id); }
    }

    static final class NoReports implements ReportsRepository {
        public List<Reports.CompetenceRow> byCompetence(Reports.Period p) { return List.of(); }
        public List<Reports.StudentRow> byStudent(Reports.Period p) {
            return List.of(new Reports.StudentRow(UUID.randomUUID(), "b", 2, 40, 50, Instant.EPOCH),
                    new Reports.StudentRow(UUID.randomUUID(), "a", 1, 90, 90, Instant.EPOCH));
        }
        public List<Reports.TopicRow> byTopic(Reports.Period p) {
            return List.of(new Reports.TopicRow("C", "Fácil", 10, 9, 90), new Reports.TopicRow("C", "Difícil", 10, 2, 20));
        }
    }

    private Results results;
    private Stats stats;
    private TrackingService service;
    private final UUID student = UUID.randomUUID();
    private int day = 1;

    @BeforeEach
    void setUp() {
        results = new Results();
        stats = new Stats();
        service = new TrackingService(results, stats, new NoReports(), new Processed(),
                new StatisticsCalculator(ClassificationPolicy.DEFAULT), Clock.fixed(Instant.parse("2026-11-30T00:00:00Z"), ZoneOffset.UTC));
    }

    private SimulationResult result(double global, CompetenceResult... comps) {
        int total = 0, correct = 0;
        for (CompetenceResult c : comps) { total += c.totalQuestions(); correct += c.correctAnswers(); }
        Instant end = Instant.parse("2026-11-01T10:00:00Z").plusSeconds(86400L * day++);
        return new SimulationResult(UUID.randomUUID(), student, "est1", "FINISHED", "MIXED", end.minusSeconds(1800), end,
                total, correct, global, List.of(comps), List.of());
    }

    @Test
    void registraEsIdempotentePorEventoYPorSimulacro() {
        SimulationResult r = result(50, new CompetenceResult("Lectura crítica", 4, 2, 50));
        UUID event = UUID.randomUUID();
        assertTrue(service.register(event, r));
        assertFalse(service.register(event, r), "mismo eventId");
        assertFalse(service.register(UUID.randomUUID(), r), "mismo simulationId con otro eventId");
        assertEquals(1, results.store.size());
    }

    @Test
    void promedioPonderadoYClasificacion() {
        service.register(UUID.randomUUID(), result(80,
                new CompetenceResult("Lectura crítica", 5, 5, 100), new CompetenceResult("Razonamiento cuantitativo", 5, 1, 20)));
        service.register(UUID.randomUUID(), result(50,
                new CompetenceResult("Lectura crítica", 5, 3, 60), new CompetenceResult("Razonamiento cuantitativo", 5, 2, 40)));
        service.register(UUID.randomUUID(), result(100, new CompetenceResult("Inglés", 2, 2, 100)));

        StudentStatistics s = service.statistics(student);
        assertEquals(3, s.simulationsCount());
        CompetenceStat lectura = s.competences().stream().filter(c -> c.competence().equals("Lectura crítica")).findFirst().orElseThrow();
        assertEquals(80.0, lectura.averageScore(), 0.001);  // 8/10
        assertEquals(60.0, lectura.lastScore(), 0.001);
        assertEquals(2, lectura.simulationsCount());
        assertEquals(Classification.FORTALEZA, lectura.classification());

        assertEquals(List.of("Lectura crítica"), s.strengths());
        assertEquals(List.of("Razonamiento cuantitativo"), s.weaknesses()); // 3/10 = 30 %
        CompetenceStat ingles = s.competences().stream().filter(c -> c.competence().equals("Inglés")).findFirst().orElseThrow();
        assertEquals(Classification.EN_DESARROLLO, ingles.classification(), "con 2 preguntas no se clasifica");
        assertEquals(76.67, s.averageGlobalScore(), 0.001);
        assertEquals(100.0, s.bestGlobalScore(), 0.001);
    }

    @Test
    void tendencia() {
        for (double g : new double[]{30, 35, 40, 70, 75, 80}) {
            service.register(UUID.randomUUID(), result(g, new CompetenceResult("X", 1, 1, g)));
        }
        assertEquals(StudentStatistics.Trend.MEJORANDO, service.statistics(student).trend());
        assertEquals(6, service.statistics(student).recentScores().size());
    }

    @Test
    void estudianteSinSimulacros() {
        StudentStatistics s = service.statistics(UUID.randomUUID());
        assertEquals(0, s.simulationsCount());
        assertEquals(StudentStatistics.Trend.SIN_DATOS, s.trend());
        assertTrue(s.strengths().isEmpty());
    }

    @Test
    void historialPaginadoRecientePrimero() {
        for (int i = 0; i < 5; i++) service.register(UUID.randomUUID(), result(i * 10, new CompetenceResult("X", 1, 0, 0)));
        TrackingQueries.HistoryPage page = service.history(student, 0, 2);
        assertEquals(5, page.totalElements());
        assertEquals(3, page.totalPages());
        assertEquals(40.0, page.content().get(0).globalScore(), 0.001);
    }

    @Test
    void reportesOrdenados() {
        assertEquals("a", service.studentReport(new Reports.Period(null, null)).get(0).studentLogin());
        assertEquals("Difícil", service.topicReport(new Reports.Period(null, null), 10).get(0).topic());
        assertThrows(IllegalArgumentException.class,
                () -> new Reports.Period(Instant.parse("2026-12-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z")));
    }

    @Test
    void politicaDeClasificacion() {
        ClassificationPolicy p = ClassificationPolicy.DEFAULT;
        assertEquals(Classification.FORTALEZA, p.classify(70, 5));
        assertEquals(Classification.EN_DESARROLLO, p.classify(69.99, 5));
        assertEquals(Classification.EN_DESARROLLO, p.classify(50, 5));
        assertEquals(Classification.DEBILIDAD, p.classify(49.99, 5));
        assertEquals(Classification.EN_DESARROLLO, p.classify(0, 4));
    }
}
