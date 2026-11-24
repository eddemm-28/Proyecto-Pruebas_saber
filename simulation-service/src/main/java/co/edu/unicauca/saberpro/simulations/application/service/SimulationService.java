package co.edu.unicauca.saberpro.simulations.application.service;

import co.edu.unicauca.saberpro.simulations.application.dto.AnswerOutcome;
import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationResultView;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationView;
import co.edu.unicauca.saberpro.simulations.application.dto.StudentRef;
import co.edu.unicauca.saberpro.simulations.application.port.in.SimulationUseCases;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationEventPublisher;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationRepository;
import co.edu.unicauca.saberpro.simulations.domain.exception.ActiveSimulationExistsException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationAccessDeniedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationClosedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationNotFoundException;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationItem;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationStatus;
import co.edu.unicauca.saberpro.simulations.domain.scoring.ScoringPolicy;
import co.edu.unicauca.saberpro.simulations.domain.strategy.QuestionSelectionStrategy;
import co.edu.unicauca.saberpro.simulations.domain.strategy.SelectionStrategyResolver;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Casos de uso del simulador (B06–B09). Clase de aplicación pura: no conoce Spring, JPA ni
 * RabbitMQ; todo entra y sale por puertos. Las transacciones las pone el adaptador de
 * infraestructura envolviendo esta clase.
 */
public class SimulationService implements SimulationUseCases {

    private final SimulationRepository simulations;
    private final QuestionReplicaRepository replica;
    private final SimulationEventPublisher events;
    private final SelectionStrategyResolver strategies;
    private final ScoringPolicy scoring;
    private final Clock clock;
    private final Duration grace;
    private final RandomGenerator random;

    public SimulationService(SimulationRepository simulations, QuestionReplicaRepository replica,
                             SimulationEventPublisher events, SelectionStrategyResolver strategies,
                             ScoringPolicy scoring, Clock clock, Duration grace, RandomGenerator random) {
        this.simulations = simulations;
        this.replica = replica;
        this.events = events;
        this.strategies = strategies;
        this.scoring = scoring;
        this.clock = clock;
        this.grace = grace;
        this.random = random;
    }

    // ------------------------------------------------------------------ comandos

    @Override
    public SimulationView start(StudentRef student, SimulationConfig config) {
        config.validate();
        Instant now = clock.instant();

        Optional<Simulation> active = simulations.findActiveByStudent(student.id());
        if (active.isPresent()) {
            Simulation current = active.get();
            if (current.isOverdue(now, grace)) {
                closeAndPublish(current, SimulationStatus.EXPIRED, now, null);
            } else {
                throw new ActiveSimulationExistsException(current.getId());
            }
        }

        QuestionSelectionStrategy strategy = strategies.resolve(config.strategy());
        List<QuestionSnapshot> candidates = replica.findAvailable(strategy.criteria(config));
        List<QuestionSnapshot> selected = strategy.select(candidates, config, random);

        Simulation simulation = simulations.save(Simulation.start(student.id(), student.login(), config, selected, now));
        return toView(simulation, now, index(selected));
    }

    @Override
    public AnswerOutcome answer(UUID simulationId, UUID questionId, String option, UUID studentId, String correlationId) {
        Simulation simulation = owned(simulationId, studentId);
        Instant now = clock.instant();
        if (simulation.getStatus().isClosed()) {
            return new AnswerOutcome(false, simulation.getStatus().name(), 0, "El simulacro ya terminó.");
        }
        if (simulation.isOverdue(now, grace)) {
            closeAndPublish(simulation, SimulationStatus.EXPIRED, now, correlationId);
            return new AnswerOutcome(false, SimulationStatus.EXPIRED.name(), 0,
                    "Se acabó el tiempo: la respuesta no se registró y el simulacro se cerró.");
        }
        simulation.answer(questionId, option, now);
        simulations.save(simulation);
        return new AnswerOutcome(true, simulation.getStatus().name(), simulation.remainingSeconds(now), "Respuesta registrada.");
    }

    @Override
    public SimulationResultView finish(UUID simulationId, UUID studentId, String correlationId) {
        Simulation simulation = owned(simulationId, studentId);
        if (simulation.getStatus().isClosed()) {
            throw new SimulationClosedException("El simulacro ya terminó (" + simulation.getStatus() + ").");
        }
        Instant now = clock.instant();
        SimulationStatus finalStatus = simulation.isOverdue(now, grace) ? SimulationStatus.EXPIRED : SimulationStatus.FINISHED;
        closeAndPublish(simulation, finalStatus, now, correlationId);
        return toResult(simulation);
    }

    @Override
    public int expireOverdue() {
        Instant now = clock.instant();
        List<Simulation> overdue = simulations.findOverdue(now.minus(grace), 100);
        overdue.forEach(s -> closeAndPublish(s, SimulationStatus.EXPIRED, now, null));
        return overdue.size();
    }

    // ------------------------------------------------------------------ consultas

    @Override
    public SimulationView get(UUID simulationId, UUID studentId, String correlationId) {
        Simulation simulation = owned(simulationId, studentId);
        Instant now = clock.instant();
        expireIfOverdue(simulation, now, correlationId);
        return toView(simulation, now, index(replica.findAllById(questionIds(simulation))));
    }

    @Override
    public Optional<SimulationView> active(UUID studentId, String correlationId) {
        Instant now = clock.instant();
        return simulations.findActiveByStudent(studentId)
                .filter(s -> !expireIfOverdue(s, now, correlationId))
                .map(s -> toView(s, now, index(replica.findAllById(questionIds(s)))));
    }

    @Override
    public SimulationResultView result(UUID simulationId, UUID studentId) {
        Simulation simulation = owned(simulationId, studentId);
        if (!simulation.getStatus().isClosed()) {
            throw new SimulationClosedException("El simulacro sigue en curso; el resultado estará disponible al finalizar.");
        }
        return toResult(simulation);
    }

    @Override
    public List<CatalogEntry> catalog() {
        return replica.catalog();
    }

    // ------------------------------------------------------------------ apoyo

    private void closeAndPublish(Simulation simulation, SimulationStatus finalStatus, Instant now, String correlationId) {
        simulation.close(now, finalStatus, scoring);
        simulations.save(simulation);
        events.publishFinished(simulation, correlationId);
    }

    /** @return {@code true} si estaba vencido y se cerró */
    private boolean expireIfOverdue(Simulation simulation, Instant now, String correlationId) {
        if (!simulation.getStatus().isClosed() && simulation.isOverdue(now, grace)) {
            closeAndPublish(simulation, SimulationStatus.EXPIRED, now, correlationId);
            return true;
        }
        return false;
    }

    private Simulation owned(UUID simulationId, UUID studentId) {
        Simulation s = simulations.findById(simulationId)
                .orElseThrow(() -> new SimulationNotFoundException("Simulacro no encontrado: " + simulationId));
        if (!s.belongsTo(studentId)) {
            throw new SimulationAccessDeniedException("El simulacro pertenece a otro estudiante.");
        }
        return s;
    }

    private static List<UUID> questionIds(Simulation s) {
        return s.getItems().stream().map(SimulationItem::getQuestionId).toList();
    }

    private static Map<UUID, QuestionSnapshot> index(List<QuestionSnapshot> snapshots) {
        return snapshots.stream().collect(Collectors.toMap(QuestionSnapshot::questionId, Function.identity(), (a, b) -> a));
    }

    private static List<SimulationView.Option> options(QuestionSnapshot q) {
        return q == null ? List.of() : q.options().stream().map(o -> new SimulationView.Option(o.label(), o.text())).toList();
    }

    private SimulationView toView(Simulation s, Instant now, Map<UUID, QuestionSnapshot> questions) {
        List<SimulationView.Item> items = s.getItems().stream().map(i -> {
            QuestionSnapshot q = questions.get(i.getQuestionId());
            return new SimulationView.Item(i.getPosition(), i.getQuestionId(), q == null ? null : q.code(),
                    i.getCompetence(), q == null ? null : q.context(), q == null ? null : q.statement(), options(q),
                    i.getSelectedOption());
        }).toList();
        return new SimulationView(s.getId(), s.getStatus().name(), s.getConfig().strategy().name(), s.getStartedAt(),
                s.getExpiresAt(), now, s.remainingSeconds(now), s.getItems().size(), s.answeredCount(), items);
    }

    private SimulationResultView toResult(Simulation s) {
        Map<UUID, QuestionSnapshot> questions = index(replica.findAllById(questionIds(s)));
        List<SimulationResultView.Item> items = s.getItems().stream().map(i -> {
            QuestionSnapshot q = questions.get(i.getQuestionId());
            return new SimulationResultView.Item(i.getPosition(), i.getQuestionId(), i.getCompetence(), i.getTopic(),
                    q == null ? null : q.statement(), options(q), i.getSelectedOption(), i.getCorrectOption(),
                    Boolean.TRUE.equals(i.getCorrect()), q == null ? null : q.justification());
        }).toList();
        return new SimulationResultView(s.getId(), s.getStatus().name(), s.getConfig().strategy().name(),
                s.getStartedAt(), s.getFinishedAt(), s.getItems().size(), s.answeredCount(), s.getCorrectCount(),
                s.getGlobalScore(),
                s.getCompetenceScores().stream().map(c -> new SimulationResultView.CompetenceScoreView(
                        c.competence(), c.totalQuestions(), c.correctAnswers(), c.score())).toList(),
                items);
    }
}
