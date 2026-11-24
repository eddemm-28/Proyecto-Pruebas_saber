package co.edu.unicauca.saberpro.client.controller;

import co.edu.unicauca.saberpro.client.infra.ApiClient;
import co.edu.unicauca.saberpro.client.infra.ApiException;
import co.edu.unicauca.saberpro.client.model.Dtos;
import co.edu.unicauca.saberpro.client.observer.Subject;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * B14: estado del simulacro en curso del estudiante (modelo MVC + Subject).
 * Eventos: STARTED, ANSWERED, EXPIRED, FINISHED.
 */
public class SimulationController extends Subject {

    public static final String STARTED = "STARTED";
    public static final String ANSWERED = "ANSWERED";
    public static final String EXPIRED = "EXPIRED";
    public static final String FINISHED = "FINISHED";

    private final ApiClient api;
    private final SimulationTimer timer;
    private volatile Dtos.Simulation current;
    private final Map<UUID, String> answers = new HashMap<>();

    public SimulationController(ApiClient api) {
        this(api, Clock.systemUTC());
    }

    public SimulationController(ApiClient api, Clock clock) {
        this.api = api;
        this.timer = new SimulationTimer(clock);
    }

    public List<Dtos.CatalogEntry> catalog() {
        return api.get("/api/simulations/catalog", null, new TypeReference<List<Dtos.CatalogEntry>>() { });
    }

    public Dtos.Simulation start(Dtos.StartSimulation request) {
        Dtos.Simulation sim = api.post("/api/simulations", request, Dtos.Simulation.class);
        load(sim);
        notifyObservers(STARTED, sim);
        return sim;
    }

    /** Retoma un simulacro en curso (p. ej. si se cerró la aplicación). */
    public Optional<Dtos.Simulation> resumeActive() {
        Dtos.Simulation sim = api.get("/api/simulations/mine/active", null, Dtos.Simulation.class);
        if (sim == null) {
            return Optional.empty();
        }
        load(sim);
        notifyObservers(STARTED, sim);
        return Optional.of(sim);
    }

    /** Re-sincroniza el cronómetro con el servidor. */
    public Dtos.Simulation refresh() {
        if (current == null) return null;
        Dtos.Simulation sim = api.get("/api/simulations/" + current.id(), null, Dtos.Simulation.class);
        load(sim);
        if (!"IN_PROGRESS".equals(sim.status())) {
            notifyObservers(EXPIRED, sim);
        }
        return sim;
    }

    /** @return {@code true} si el servidor aceptó la respuesta; {@code false} si ya expiró. */
    public boolean answer(UUID questionId, String option) {
        if (current == null) {
            throw new IllegalStateException("No hay simulacro en curso");
        }
        ApiClient.RawResponse raw = api.putRaw("/api/simulations/" + current.id() + "/answers/" + questionId,
                new Dtos.Answer(option));
        if (raw.status() == 200) {
            Dtos.AnswerOutcome outcome = api.read(raw.body(), Dtos.AnswerOutcome.class);
            answers.put(questionId, option);
            notifyObservers(ANSWERED, outcome);
            return true;
        }
        if (raw.status() == 409) {
            Dtos.AnswerOutcome outcome = api.read(raw.body(), Dtos.AnswerOutcome.class);
            notifyObservers(EXPIRED, outcome);
            return false;
        }
        throw new ApiException(raw.status(), "No se pudo registrar la respuesta (HTTP " + raw.status() + ")", List.of(), raw.body());
    }

    public Dtos.SimulationResult finish() {
        if (current == null) {
            throw new IllegalStateException("No hay simulacro en curso");
        }
        Dtos.SimulationResult result;
        try {
            result = api.post("/api/simulations/" + current.id() + "/finish", null, Dtos.SimulationResult.class);
        } catch (ApiException e) {
            if (!e.isConflict()) throw e;
            // Ya lo cerró el servidor por tiempo: se consulta el resultado
            result = api.get("/api/simulations/" + current.id() + "/result", null, Dtos.SimulationResult.class);
        }
        current = null;
        notifyObservers(FINISHED, result);
        return result;
    }

    private void load(Dtos.Simulation sim) {
        this.current = sim;
        timer.sync(sim.serverTime(), sim.expiresAt());
        answers.clear();
        sim.items().forEach(i -> {
            if (i.selectedOption() != null) answers.put(i.questionId(), i.selectedOption());
        });
    }

    public Dtos.Simulation current() { return current; }
    public SimulationTimer timer() { return timer; }
    public String answerOf(UUID questionId) { return answers.get(questionId); }
    public int answeredCount() { return answers.size(); }
}
