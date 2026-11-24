package co.edu.unicauca.saberpro.questions.service;

import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.questions.domain.model.ContentDiff;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.policy.ForbiddenOperationException;
import co.edu.unicauca.saberpro.questions.domain.policy.QuestionPermissions;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidator;
import co.edu.unicauca.saberpro.questions.outbox.OutboxWriter;
import co.edu.unicauca.saberpro.questions.persistence.Question;
import co.edu.unicauca.saberpro.questions.persistence.QuestionChangeHistory;
import co.edu.unicauca.saberpro.questions.persistence.QuestionChangeHistoryRepository;
import co.edu.unicauca.saberpro.questions.persistence.QuestionRepository;
import co.edu.unicauca.saberpro.questions.persistence.QuestionStateAudit;
import co.edu.unicauca.saberpro.questions.persistence.QuestionStateAuditRepository;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;
import co.edu.unicauca.saberpro.questions.plugin.api.QuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.registry.QuestionPluginRegistry;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Casos de uso del banco de preguntas (A12–A19). */
@Service
public class QuestionService {

    private final QuestionRepository questions;
    private final QuestionChangeHistoryRepository history;
    private final QuestionStateAuditRepository stateAudit;
    private final QuestionValidator validator;
    private final QuestionPluginRegistry plugins;
    private final OutboxWriter outbox;

    public QuestionService(QuestionRepository questions, QuestionChangeHistoryRepository history,
                           QuestionStateAuditRepository stateAudit, QuestionValidator validator,
                           QuestionPluginRegistry plugins, OutboxWriter outbox) {
        this.questions = questions;
        this.history = history;
        this.stateAudit = stateAudit;
        this.validator = validator;
        this.plugins = plugins;
        this.outbox = outbox;
    }

    // ---------------------------------------------------------------- creación

    /** RF-05 + RF-08..RF-13: crea la pregunta en Borrador. */
    @Transactional
    public Question create(QuestionContent content, CurrentUser user) {
        return create(content, user, "MANUAL");
    }

    /** A14: las preguntas generadas por plugins pasan por los mismos validadores y se persisten. */
    @Transactional
    public Question generateWithPlugin(String pluginId, PluginRequest request, CurrentUser user) {
        QuestionPlugin plugin = plugins.find(pluginId)
                .orElseThrow(() -> new NotFoundException("No existe el plugin '" + pluginId + "'"));
        return create(plugin.generate(request), user, plugin.id());
    }

    private Question create(QuestionContent content, CurrentUser user, String pluginId) {
        if (!QuestionPermissions.canCreate(user.role())) {
            throw new ForbiddenOperationException("Solo autores y administradores pueden crear preguntas.");
        }
        validator.validateOrThrow(content);
        String code = "Q-" + Year.now().getValue() + "-" + String.format("%06d", questions.nextCodeNumber());
        Question q = questions.save(Question.create(content, code, user.id(), user.login(), pluginId));
        stateAudit.save(new QuestionStateAudit(q.getId(), null, QuestionStatus.BORRADOR, user.id(), user.login(),
                "Creada" + ("MANUAL".equals(pluginId) ? "" : " con el plugin " + pluginId)));
        return q;
    }

    // ---------------------------------------------------------------- edición

    /** RF-06: edición solo en Borrador y solo por el autor; RF-29: historial por campo. */
    @Transactional
    public Question update(UUID id, QuestionContent content, Long expectedVersion, CurrentUser user) {
        Question q = find(id);
        QuestionPermissions.checkCanEdit(user.role(), q.isOwnedBy(user.id()));
        if (expectedVersion != null && expectedVersion != q.getVersion()) {
            throw new OptimisticLockingFailureException("Versión desactualizada");
        }
        validator.validateOrThrow(content);
        List<ContentDiff.FieldChange> changes = q.updateContent(content);
        for (ContentDiff.FieldChange c : changes) {
            history.save(new QuestionChangeHistory(q.getId(), user.id(), user.login(), c.field(), c.oldValue(), c.newValue()));
        }
        return questions.save(q);
    }

    // ---------------------------------------------------------------- estados

    /**
     * A13 + A19: aplica una transición. El State decide si es válida; {@link QuestionPermissions}
     * decide si el usuario puede. Publicar y archivar una publicada emiten eventos (A18).
     */
    @Transactional
    public Question transition(UUID id, QuestionAction action, String comment, CurrentUser user, String correlationId) {
        Question q = find(id);
        QuestionPermissions.checkCanApply(action, q.getStatus(), user.role(), q.isOwnedBy(user.id()), comment);
        QuestionStatus from = q.apply(action);
        stateAudit.save(new QuestionStateAudit(q.getId(), from, q.getStatus(), user.id(), user.login(), comment));

        Question saved = questions.saveAndFlush(q);
        if (action == QuestionAction.PUBLISH) {
            outbox.enqueue(saved.getId(), EventTopology.RK_QUESTION_PUBLISHED, QuestionEvents.published(saved, correlationId));
        } else if (action == QuestionAction.ARCHIVE && from == QuestionStatus.PUBLICADA) {
            outbox.enqueue(saved.getId(), EventTopology.RK_QUESTION_ARCHIVED,
                    QuestionEvents.archived(saved, user.login(), correlationId));
        }
        return saved;
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public Question get(UUID id, CurrentUser user) {
        Question q = find(id);
        if (!QuestionPermissions.canRead(user.role(), q.isOwnedBy(user.id()))) {
            throw new ForbiddenOperationException("No puede consultar preguntas de otros autores.");
        }
        return q;
    }

    /** RF-07. Un autor solo ve las suyas salvo que tenga un rol con lectura global. */
    @Transactional(readOnly = true)
    public Page<Question> search(QuestionFilter filter, boolean onlyMine, CurrentUser user, Pageable pageable) {
        QuestionFilter effective = (onlyMine || !QuestionPermissions.canReadAll(user.role()))
                ? filter.withAuthor(user.id()) : filter;
        return questions.findAll(effective.toSpecification(), pageable);
    }

    @Transactional(readOnly = true)
    public List<QuestionChangeHistory> history(UUID id, CurrentUser user) {
        checkOwnerOrAdmin(find(id), user);
        return history.findByQuestionIdOrderByChangedAtDescIdDesc(id);
    }

    @Transactional(readOnly = true)
    public List<QuestionStateAudit> stateAudit(UUID id, CurrentUser user) {
        checkOwnerOrAdmin(find(id), user);
        return stateAudit.findByQuestionIdOrderByChangedAtAscIdAsc(id);
    }

    @Transactional(readOnly = true)
    public Map<QuestionStatus, Long> countByState() {
        Map<QuestionStatus, Long> result = new EnumMap<>(QuestionStatus.class);
        for (QuestionStatus s : QuestionStatus.values()) {
            result.put(s, 0L);
        }
        questions.countByStatus().forEach(c -> result.put(c.getStatus(), c.getTotal()));
        return result;
    }

    public List<QuestionPlugin> plugins() {
        return List.copyOf(plugins.all());
    }

    private Question find(UUID id) {
        return questions.findById(id).orElseThrow(() -> new NotFoundException("Pregunta no encontrada: " + id));
    }

    private void checkOwnerOrAdmin(Question q, CurrentUser user) {
        if (!q.isOwnedBy(user.id()) && !QuestionPermissions.ADMIN.equals(user.role())) {
            throw new ForbiddenOperationException("Solo el autor o un administrador pueden ver el historial.");
        }
    }
}
