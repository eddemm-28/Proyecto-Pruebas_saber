package co.edu.unicauca.saberpro.simulations.infrastructure.web;

import co.edu.unicauca.saberpro.simulations.application.dto.AnswerOutcome;
import co.edu.unicauca.saberpro.simulations.application.dto.CatalogEntry;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationResultView;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationView;
import co.edu.unicauca.saberpro.simulations.application.dto.StudentRef;
import co.edu.unicauca.saberpro.simulations.application.port.in.SimulationUseCases;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Adaptador de entrada REST (B06–B09). Solo traduce HTTP ↔ puertos de entrada. */
@RestController
@RequestMapping("/api/simulations")
public class SimulationController {

    private static final String CID = "X-Correlation-Id";

    private final SimulationUseCases useCases;

    public SimulationController(SimulationUseCases useCases) {
        this.useCases = useCases;
    }

    public record StartRequest(String strategy, String competence, String topic, String difficulty,
                               Integer questionCount, Integer timeLimitMinutes) {
    }

    public record AnswerRequest(String selectedOption) {
    }

    @GetMapping("/catalog")
    public List<CatalogEntry> catalog() {
        return useCases.catalog();
    }

    @PostMapping
    public ResponseEntity<SimulationView> start(@RequestBody StartRequest req, Authentication auth) {
        CurrentUser user = CurrentUser.from(auth);
        StrategyType strategy;
        try {
            strategy = req.strategy() == null ? null : StrategyType.valueOf(req.strategy().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            strategy = null;
        }
        SimulationConfig config = new SimulationConfig(strategy, req.competence(), req.topic(), req.difficulty(),
                req.questionCount() == null ? 0 : req.questionCount(),
                req.timeLimitMinutes() == null ? 0 : req.timeLimitMinutes());
        SimulationView view = useCases.start(new StudentRef(user.id(), user.login()), config);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/mine/active")
    public ResponseEntity<SimulationView> active(@RequestHeader(value = CID, required = false) String cid,
                                                 Authentication auth) {
        return useCases.active(CurrentUser.from(auth).id(), cid)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public SimulationView get(@PathVariable UUID id, @RequestHeader(value = CID, required = false) String cid,
                              Authentication auth) {
        return useCases.get(id, CurrentUser.from(auth).id(), cid);
    }

    /** 200 si se registró; 409 si se acabó el tiempo (el simulacro queda cerrado como EXPIRED). */
    @PutMapping("/{id}/answers/{questionId}")
    public ResponseEntity<AnswerOutcome> answer(@PathVariable UUID id, @PathVariable UUID questionId,
                                                @RequestBody AnswerRequest req,
                                                @RequestHeader(value = CID, required = false) String cid,
                                                Authentication auth) {
        AnswerOutcome outcome = useCases.answer(id, questionId, req.selectedOption(), CurrentUser.from(auth).id(), cid);
        return outcome.accepted() ? ResponseEntity.ok(outcome) : ResponseEntity.status(HttpStatus.CONFLICT).body(outcome);
    }

    @PostMapping("/{id}/finish")
    public SimulationResultView finish(@PathVariable UUID id, @RequestHeader(value = CID, required = false) String cid,
                                       Authentication auth) {
        return useCases.finish(id, CurrentUser.from(auth).id(), cid);
    }

    @GetMapping("/{id}/result")
    public SimulationResultView result(@PathVariable UUID id, Authentication auth) {
        return useCases.result(id, CurrentUser.from(auth).id());
    }
}
