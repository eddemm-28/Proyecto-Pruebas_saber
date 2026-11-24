package co.edu.unicauca.saberpro.simulations.infrastructure.persistence;

import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationRepository;
import co.edu.unicauca.saberpro.simulations.domain.model.CompetenceScore;
import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationItem;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationStatus;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.SimulationEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.SimulationItemEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.SimulationJpaRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Adaptador de salida: traduce el agregado de dominio {@link Simulation} a JPA y viceversa. */
@Component
public class SimulationPersistenceAdapter implements SimulationRepository {

    private final SimulationJpaRepository jpa;

    public SimulationPersistenceAdapter(SimulationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Simulation> findById(UUID id) {
        return jpa.findById(id).map(SimulationPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Simulation> findActiveByStudent(UUID studentId) {
        return jpa.findFirstByStudentIdAndStatus(studentId, SimulationStatus.IN_PROGRESS.name())
                .map(SimulationPersistenceAdapter::toDomain);
    }

    @Override
    public List<Simulation> findOverdue(Instant cutoff, int limit) {
        return jpa.findByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(SimulationStatus.IN_PROGRESS.name(), cutoff,
                        PageRequest.of(0, limit))
                .stream().map(SimulationPersistenceAdapter::toDomain).toList();
    }

    /**
     * Se usa saveAndFlush para que el cambio de estado se escriba antes de insertar otro
     * simulacro del mismo estudiante (índice único parcial "un solo IN_PROGRESS").
     */
    @Override
    public Simulation save(Simulation s) {
        Optional<SimulationEntity> existing = jpa.findById(s.getId());
        SimulationEntity e;
        if (existing.isPresent()) {
            e = existing.get();
            if (e.version != s.getVersion()) {
                throw new OptimisticLockingFailureException("El simulacro fue modificado concurrentemente");
            }
        } else {
            e = new SimulationEntity();
            e.id = s.getId();
            e.studentId = s.getStudentId();
            e.studentLogin = s.getStudentLogin();
            SimulationConfig c = s.getConfig();
            e.strategy = c.strategy().name();
            e.filterCompetence = c.competence();
            e.filterTopic = c.topic();
            e.filterDifficulty = c.difficulty();
            e.questionCount = c.questionCount();
            e.timeLimitMinutes = c.timeLimitMinutes();
            e.startedAt = s.getStartedAt();
            e.expiresAt = s.getExpiresAt();
        }
        e.status = s.getStatus().name();
        e.finishedAt = s.getFinishedAt();
        e.correctCount = s.getCorrectCount();
        e.globalScore = BigDecimal.valueOf(s.getGlobalScore()).setScale(2, RoundingMode.HALF_UP);
        syncItems(e, s.getItems());
        e.competenceScores.clear();
        s.getCompetenceScores().forEach(cs -> e.competenceScores.add(new SimulationEntity.CompetenceScoreEmbeddable(
                cs.competence(), cs.totalQuestions(), cs.correctAnswers(),
                BigDecimal.valueOf(cs.score()).setScale(2, RoundingMode.HALF_UP))));
        return toDomain(jpa.saveAndFlush(e));
    }

    private static void syncItems(SimulationEntity e, List<SimulationItem> items) {
        Map<UUID, SimulationItemEntity> current = e.items.stream()
                .collect(Collectors.toMap(i -> i.id, Function.identity()));
        for (SimulationItem item : items) {
            SimulationItemEntity ie = current.get(item.getId());
            if (ie == null) {
                ie = new SimulationItemEntity();
                ie.id = item.getId();
                ie.simulation = e;
                ie.questionId = item.getQuestionId();
                ie.position = item.getPosition();
                ie.competence = item.getCompetence();
                ie.topic = item.getTopic();
                ie.difficulty = item.getDifficulty();
                ie.correctOption = item.getCorrectOption();
                e.items.add(ie);
            }
            ie.selectedOption = item.getSelectedOption();
            ie.correct = item.getCorrect();
            ie.answeredAt = item.getAnsweredAt();
        }
    }

    static Simulation toDomain(SimulationEntity e) {
        SimulationConfig config = new SimulationConfig(StrategyType.valueOf(e.strategy), e.filterCompetence,
                e.filterTopic, e.filterDifficulty, e.questionCount, e.timeLimitMinutes);
        List<SimulationItem> items = e.items.stream()
                .map(i -> new SimulationItem(i.id, i.questionId, i.position, i.competence, i.topic, i.difficulty,
                        i.correctOption, i.selectedOption, i.correct, i.answeredAt))
                .toList();
        List<CompetenceScore> scores = e.competenceScores.stream()
                .map(c -> new CompetenceScore(c.competence, c.totalQuestions, c.correctAnswers, c.score.doubleValue()))
                .toList();
        return new Simulation(e.id, e.studentId, e.studentLogin, config, e.startedAt, e.expiresAt, items,
                SimulationStatus.valueOf(e.status), e.finishedAt, e.correctCount, e.globalScore.doubleValue(),
                scores, e.version);
    }
}
