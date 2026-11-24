package co.edu.unicauca.saberpro.questions.web;

import co.edu.unicauca.saberpro.questions.domain.model.Difficulty;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import co.edu.unicauca.saberpro.questions.service.CurrentUser;
import co.edu.unicauca.saberpro.questions.service.QuestionFilter;
import co.edu.unicauca.saberpro.questions.service.QuestionService;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.ChangeHistoryResponse;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.PageResponse;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.QuestionRequest;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.QuestionResponse;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.QuestionSummary;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.StateAuditResponse;
import co.edu.unicauca.saberpro.questions.web.dto.QuestionDtos.TransitionRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Endpoints del banco de preguntas (A12–A19). */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private static final String CORRELATION = "X-Correlation-Id";

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@RequestBody QuestionRequest req, Authentication auth) {
        return QuestionResponse.from(service.create(req.toContent(), CurrentUser.from(auth)));
    }

    @GetMapping
    public PageResponse<QuestionSummary> search(@RequestParam(required = false) QuestionStatus state,
                                                @RequestParam(required = false) String competence,
                                                @RequestParam(required = false) String topic,
                                                @RequestParam(required = false) String subtopic,
                                                @RequestParam(required = false) String difficulty,
                                                @RequestParam(required = false) QuestionType type,
                                                @RequestParam(required = false) UUID authorId,
                                                @RequestParam(required = false, name = "q") String text,
                                                @RequestParam(defaultValue = "all") String scope,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                Authentication auth) {
        QuestionFilter filter = new QuestionFilter(state, competence, topic, subtopic,
                Difficulty.tryParse(difficulty).orElse(null), type, authorId, text);
        return PageResponse.of(service.search(filter, "mine".equalsIgnoreCase(scope), CurrentUser.from(auth),
                pageRequest(page, size)), QuestionSummary::from);
    }

    @GetMapping("/mine")
    public PageResponse<QuestionSummary> mine(@RequestParam(required = false) QuestionStatus state,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              Authentication auth) {
        QuestionFilter filter = new QuestionFilter(state, null, null, null, null, null, null, null);
        return PageResponse.of(service.search(filter, true, CurrentUser.from(auth), pageRequest(page, size)),
                QuestionSummary::from);
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable UUID id, Authentication auth) {
        return QuestionResponse.from(service.get(id, CurrentUser.from(auth)));
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable UUID id, @RequestBody QuestionRequest req, Authentication auth) {
        return QuestionResponse.from(service.update(id, req.toContent(), req.version(), CurrentUser.from(auth)));
    }

    // -------- Transiciones de estado (A13) y aprobación mínima (A19) --------

    @PostMapping("/{id}/submit")
    public QuestionResponse submit(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                   @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.SUBMIT, req, cid, auth);
    }

    @PostMapping("/{id}/start-review")
    public QuestionResponse startReview(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                        @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.START_REVIEW, req, cid, auth);
    }

    @PostMapping("/{id}/approve")
    public QuestionResponse approve(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                    @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.APPROVE, req, cid, auth);
    }

    @PostMapping("/{id}/reject")
    public QuestionResponse reject(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                   @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.REJECT, req, cid, auth);
    }

    @PostMapping("/{id}/return-to-draft")
    public QuestionResponse returnToDraft(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                          @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.RETURN_TO_DRAFT, req, cid, auth);
    }

    @PostMapping("/{id}/publish")
    public QuestionResponse publish(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                    @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.PUBLISH, req, cid, auth);
    }

    /** RNF-16: reemplaza al DELETE físico. */
    @PostMapping("/{id}/archive")
    public QuestionResponse archive(@PathVariable UUID id, @RequestBody(required = false) TransitionRequest req,
                                    @RequestHeader(value = CORRELATION, required = false) String cid, Authentication auth) {
        return transition(id, QuestionAction.ARCHIVE, req, cid, auth);
    }

    // -------- Historial y auditoría (A17) --------

    @GetMapping("/{id}/history")
    public List<ChangeHistoryResponse> history(@PathVariable UUID id, Authentication auth) {
        return service.history(id, CurrentUser.from(auth)).stream().map(ChangeHistoryResponse::from).toList();
    }

    @GetMapping("/{id}/state-audit")
    public List<StateAuditResponse> stateAudit(@PathVariable UUID id, Authentication auth) {
        return service.stateAudit(id, CurrentUser.from(auth)).stream().map(StateAuditResponse::from).toList();
    }

    @GetMapping("/stats/by-state")
    public Map<QuestionStatus, Long> statsByState() {
        return service.countByState();
    }

    private QuestionResponse transition(UUID id, QuestionAction action, TransitionRequest req, String cid,
                                        Authentication auth) {
        String comment = req == null ? null : req.comment();
        return QuestionResponse.from(service.transition(id, action, comment, CurrentUser.from(auth), cid));
    }

    private static PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
