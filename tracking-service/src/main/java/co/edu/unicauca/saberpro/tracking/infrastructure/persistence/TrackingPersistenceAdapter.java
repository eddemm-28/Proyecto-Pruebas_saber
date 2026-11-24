package co.edu.unicauca.saberpro.tracking.infrastructure.persistence;

import co.edu.unicauca.saberpro.tracking.application.port.out.CompetenceStatsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.tracking.application.port.out.ReportsRepository;
import co.edu.unicauca.saberpro.tracking.application.port.out.SimulationResultRepository;
import co.edu.unicauca.saberpro.tracking.domain.model.Classification;
import co.edu.unicauca.saberpro.tracking.domain.model.CompetenceStat;
import co.edu.unicauca.saberpro.tracking.domain.model.Reports;
import co.edu.unicauca.saberpro.tracking.domain.model.SimulationResult;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.ProcessedEventEntity;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.SimulationResultEntity;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.StudentCompetenceStatEntity;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository.SimulationResultJpaRepository;
import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository.StudentCompetenceStatJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Adaptador de salida PostgreSQL de todos los puertos del seguimiento. */
@Component
public class TrackingPersistenceAdapter
        implements SimulationResultRepository, CompetenceStatsRepository, ReportsRepository, ProcessedEventStore {

    private static final Instant MIN = Instant.parse("2000-01-01T00:00:00Z");

    private final SimulationResultJpaRepository results;
    private final StudentCompetenceStatJpaRepository stats;
    private final ProcessedEventJpaRepository processed;

    public TrackingPersistenceAdapter(SimulationResultJpaRepository results, StudentCompetenceStatJpaRepository stats,
                                      ProcessedEventJpaRepository processed) {
        this.results = results;
        this.stats = stats;
        this.processed = processed;
    }

    // ------------------------------------------------------------ ProcessedEventStore

    @Override
    public boolean markProcessed(UUID eventId, String eventType) {
        if (processed.existsById(eventId)) {
            return false;
        }
        processed.save(new ProcessedEventEntity(eventId, eventType));
        return true;
    }

    // ------------------------------------------------------------ SimulationResultRepository

    @Override
    public boolean exists(UUID simulationId) {
        return results.existsById(simulationId);
    }

    @Override
    public void save(SimulationResult r) {
        SimulationResultEntity e = new SimulationResultEntity();
        e.simulationId = r.simulationId();
        e.studentId = r.studentId();
        e.studentLogin = r.studentLogin();
        e.finalStatus = r.finalStatus();
        e.strategy = r.strategy();
        e.totalQuestions = r.totalQuestions();
        e.correctAnswers = r.correctAnswers();
        e.globalScore = decimal(r.globalScore());
        e.durationSeconds = (int) Math.min(Integer.MAX_VALUE, r.durationSeconds());
        e.startedAt = r.startedAt();
        e.finishedAt = r.finishedAt();
        e.receivedAt = Instant.now();
        r.competenceScores().forEach(c -> e.competenceScores.add(new SimulationResultEntity.CompetenceScore(
                c.competence(), c.totalQuestions(), c.correctAnswers(), decimal(c.score()))));
        r.answers().forEach(a -> e.answers.add(new SimulationResultEntity.Answer(a.questionId(), a.competence(),
                a.topic(), a.difficulty(), a.answered(), a.correct())));
        results.save(e);
    }

    @Override
    public List<SimulationResult> findByStudent(UUID studentId, int page, int size) {
        return results.findByStudentIdOrderByFinishedAtDesc(studentId, PageRequest.of(page, size))
                .map(e -> toDomain(e, false)).getContent();
    }

    @Override
    public long countByStudent(UUID studentId) {
        return results.countByStudentId(studentId);
    }

    @Override
    public List<SimulationResult> findAllByStudent(UUID studentId) {
        return results.findByStudentIdOrderByFinishedAtAsc(studentId).stream().map(e -> toDomain(e, false)).toList();
    }

    // ------------------------------------------------------------ CompetenceStatsRepository

    @Override
    public List<CompetenceStat> findByStudent(UUID studentId) {
        return stats.findByStudentId(studentId).stream().map(TrackingPersistenceAdapter::toDomain).toList();
    }

    @Override
    public void saveAll(List<CompetenceStat> list) {
        for (CompetenceStat c : list) {
            StudentCompetenceStatEntity e = stats.findById(new StudentCompetenceStatEntity.Key(c.studentId(), c.competence()))
                    .orElseGet(StudentCompetenceStatEntity::new);
            e.studentId = c.studentId();
            e.competence = c.competence();
            e.studentLogin = c.studentLogin();
            e.simulationsCount = c.simulationsCount();
            e.totalQuestions = c.totalQuestions();
            e.correctAnswers = c.correctAnswers();
            e.averageScore = decimal(c.averageScore());
            e.lastScore = decimal(c.lastScore());
            e.classification = c.classification().name();
            e.updatedAt = c.updatedAt();
            stats.save(e);
        }
    }

    // ------------------------------------------------------------ ReportsRepository

    @Override
    public List<Reports.CompetenceRow> byCompetence(Reports.Period p) {
        return results.competenceAggregates(from(p), to(p)).stream().map(r -> {
            long total = num(r[3]).longValue();
            long correct = num(r[4]).longValue();
            return new Reports.CompetenceRow((String) r[0], num(r[1]).longValue(), num(r[2]).longValue(), total,
                    correct, pct(correct, total));
        }).toList();
    }

    @Override
    public List<Reports.StudentRow> byStudent(Reports.Period p) {
        return results.studentAggregates(from(p), to(p)).stream().map(r -> new Reports.StudentRow(
                (UUID) r[0], (String) r[1], num(r[2]).longValue(), round(num(r[3]).doubleValue()),
                round(num(r[4]).doubleValue()), (Instant) r[5])).toList();
    }

    @Override
    public List<Reports.TopicRow> byTopic(Reports.Period p) {
        return results.topicAggregates(from(p), to(p)).stream().map(r -> {
            long total = num(r[2]).longValue();
            long correct = num(r[3]).longValue();
            return new Reports.TopicRow((String) r[0], (String) r[1], total, correct, pct(correct, total));
        }).toList();
    }

    // ------------------------------------------------------------ mapeos

    private static SimulationResult toDomain(SimulationResultEntity e, boolean withAnswers) {
        return new SimulationResult(e.simulationId, e.studentId, e.studentLogin, e.finalStatus, e.strategy,
                e.startedAt, e.finishedAt, e.totalQuestions, e.correctAnswers, e.globalScore.doubleValue(),
                e.competenceScores.stream().map(c -> new SimulationResult.CompetenceResult(c.competence,
                        c.totalQuestions, c.correctAnswers, c.score.doubleValue())).toList(),
                withAnswers ? e.answers.stream().map(a -> new SimulationResult.AnswerResult(a.questionId,
                        a.competence, a.topic, a.difficulty, a.answered, a.correct)).toList() : List.of());
    }

    private static CompetenceStat toDomain(StudentCompetenceStatEntity e) {
        return new CompetenceStat(e.studentId, e.studentLogin, e.competence, e.simulationsCount, e.totalQuestions,
                e.correctAnswers, e.averageScore.doubleValue(), e.lastScore.doubleValue(),
                Classification.valueOf(e.classification), e.updatedAt);
    }

    private static Instant from(Reports.Period p) {
        return p == null || p.from() == null ? MIN : p.from();
    }

    private static Instant to(Reports.Period p) {
        return p == null || p.to() == null ? Instant.now().plus(1, ChronoUnit.DAYS) : p.to();
    }

    private static Number num(Object o) {
        return o == null ? 0 : (Number) o;
    }

    private static BigDecimal decimal(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static double round(double v) {
        return decimal(v).doubleValue();
    }

    private static double pct(long correct, long total) {
        return total == 0 ? 0.0 : round(correct * 100.0 / total);
    }
}
