package co.edu.unicauca.saberpro.simulations.domain.model;

import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationClosedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationNotFoundException;
import co.edu.unicauca.saberpro.simulations.domain.scoring.ScoringPolicy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado Simulacro (dominio puro, sin JPA ni Spring).
 * El control de tiempo (RF-23) se hace siempre con el reloj del servidor.
 */
public final class Simulation {

    private final UUID id;
    private final UUID studentId;
    private final String studentLogin;
    private final SimulationConfig config;
    private final Instant startedAt;
    private final Instant expiresAt;
    private final List<SimulationItem> items;
    private SimulationStatus status;
    private Instant finishedAt;
    private int correctCount;
    private double globalScore;
    private List<CompetenceScore> competenceScores;
    private final long version;

    public Simulation(UUID id, UUID studentId, String studentLogin, SimulationConfig config, Instant startedAt,
                      Instant expiresAt, List<SimulationItem> items, SimulationStatus status, Instant finishedAt,
                      int correctCount, double globalScore, List<CompetenceScore> competenceScores, long version) {
        this.id = Objects.requireNonNull(id);
        this.studentId = Objects.requireNonNull(studentId);
        this.studentLogin = studentLogin;
        this.config = Objects.requireNonNull(config);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.items = new ArrayList<>(items);
        this.status = Objects.requireNonNull(status);
        this.finishedAt = finishedAt;
        this.correctCount = correctCount;
        this.globalScore = globalScore;
        this.competenceScores = competenceScores == null ? new ArrayList<>() : new ArrayList<>(competenceScores);
        this.version = version;
    }

    /** Crea un simulacro nuevo con las preguntas ya seleccionadas por la estrategia. */
    public static Simulation start(UUID studentId, String studentLogin, SimulationConfig config,
                                   List<QuestionSnapshot> selected, Instant now) {
        List<SimulationItem> items = new ArrayList<>();
        for (int i = 0; i < selected.size(); i++) {
            items.add(SimulationItem.from(selected.get(i), i + 1));
        }
        Instant expires = now.plus(Duration.ofMinutes(config.timeLimitMinutes()));
        return new Simulation(UUID.randomUUID(), studentId, studentLogin, config, now, expires, items,
                SimulationStatus.IN_PROGRESS, null, 0, 0.0, List.of(), 0);
    }

    // ------------------------------------------------------------------ tiempo

    /** ¿Se acabó el tiempo? Se admite un pequeño margen por latencia de red. */
    public boolean isOverdue(Instant now, Duration grace) {
        return now.isAfter(expiresAt.plus(grace));
    }

    public long remainingSeconds(Instant now) {
        if (status.isClosed()) {
            return 0;
        }
        return Math.max(0, Duration.between(now, expiresAt).getSeconds());
    }

    // ------------------------------------------------------------------ comportamiento

    /**
     * Registra (o cambia) la respuesta de una pregunta. El llamador debe comprobar antes
     * {@link #isOverdue}; aquí solo se exige que el simulacro siga abierto.
     */
    public void answer(UUID questionId, String option, Instant now) {
        ensureOpen();
        String normalized = option == null ? null : option.trim().toUpperCase();
        if (normalized != null && !List.of("A", "B", "C", "D").contains(normalized)) {
            throw new IllegalArgumentException("La opción debe ser A, B, C o D.");
        }
        SimulationItem item = items.stream().filter(i -> i.getQuestionId().equals(questionId)).findFirst()
                .orElseThrow(() -> new SimulationNotFoundException("La pregunta no pertenece a este simulacro."));
        item.answer(normalized, now);
    }

    /** RF-24: cierra y califica. Las preguntas sin responder cuentan como incorrectas. */
    public void close(Instant now, SimulationStatus finalStatus, ScoringPolicy scoring) {
        ensureOpen();
        if (finalStatus == SimulationStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Estado final inválido");
        }
        items.forEach(SimulationItem::grade);
        ScoringPolicy.Result result = scoring.score(items);
        this.correctCount = result.correctCount();
        this.globalScore = result.globalScore();
        this.competenceScores = new ArrayList<>(result.byCompetence());
        this.status = finalStatus;
        // Si expiró, la hora de cierre "oficial" es la de vencimiento
        this.finishedAt = finalStatus == SimulationStatus.EXPIRED && now.isAfter(expiresAt) ? expiresAt : now;
    }

    private void ensureOpen() {
        if (status.isClosed()) {
            throw new SimulationClosedException("El simulacro ya terminó (" + status + ").");
        }
    }

    public boolean belongsTo(UUID student) {
        return studentId.equals(student);
    }

    public int answeredCount() {
        return (int) items.stream().filter(SimulationItem::isAnswered).count();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getStudentLogin() { return studentLogin; }
    public SimulationConfig getConfig() { return config; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public List<SimulationItem> getItems() { return Collections.unmodifiableList(items); }
    public SimulationStatus getStatus() { return status; }
    public Instant getFinishedAt() { return finishedAt; }
    public int getCorrectCount() { return correctCount; }
    public double getGlobalScore() { return globalScore; }
    public List<CompetenceScore> getCompetenceScores() { return Collections.unmodifiableList(competenceScores); }
    public long getVersion() { return version; }
}
