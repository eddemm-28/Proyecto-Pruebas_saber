package co.edu.unicauca.saberpro.questions.web.dto;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.persistence.Question;
import co.edu.unicauca.saberpro.questions.persistence.QuestionChangeHistory;
import co.edu.unicauca.saberpro.questions.persistence.QuestionStateAudit;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;
import co.edu.unicauca.saberpro.questions.plugin.api.QuestionPlugin;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** DTOs REST del question-service. */
public final class QuestionDtos {

    private QuestionDtos() {
    }

    /** Alta/edición (RF-05). {@code version} es opcional y activa el control de concurrencia en PUT. */
    public record QuestionRequest(String title, String type, String context, String statement, List<String> options,
                                  String correctOption, String justification, String bibliography, String competence,
                                  String topic, String subtopic, String difficulty, String mediaUrl, Long version) {
        public QuestionContent toContent() {
            return new QuestionContent(title, QuestionType.parse(type), context, statement, options, correctOption,
                    justification, bibliography, competence, topic, subtopic, difficulty, mediaUrl);
        }
    }

    public record OptionDto(String label, String text) {
    }

    public record QuestionResponse(UUID id, String code, String title, String type, String context, String statement,
                                   List<OptionDto> options, String correctOption, String justification,
                                   String bibliography, String competence, String topic, String subtopic,
                                   String difficulty, String difficultyName, String state, String stateName,
                                   boolean editable, Set<QuestionAction> allowedActions, UUID authorId,
                                   String authorLogin, String pluginId, String mediaUrl, Instant createdAt,
                                   Instant updatedAt, Instant publishedAt, Instant archivedAt, long version) {
        public static QuestionResponse from(Question q) {
            return new QuestionResponse(q.getId(), q.getCode(), q.getTitle(), q.getType().name(), q.getContext(),
                    q.getStatement(), q.getOptions().stream().map(o -> new OptionDto(o.getLabel(), o.getText())).toList(),
                    q.getCorrectOption(), q.getJustification(), q.getBibliography(), q.getCompetence(), q.getTopic(),
                    q.getSubtopic(), q.getDifficulty().name(), q.getDifficulty().displayName(), q.getStatus().name(),
                    q.getStatus().displayName(), q.state().isEditable(), q.allowedActions(), q.getAuthorId(),
                    q.getAuthorLogin(), q.getPluginId(), q.getMediaUrl(), q.getCreatedAt(), q.getUpdatedAt(),
                    q.getPublishedAt(), q.getArchivedAt(), q.getVersion());
        }
    }

    public record QuestionSummary(UUID id, String code, String title, String type, String competence, String topic,
                                  String subtopic, String difficulty, String state, String stateName,
                                  String authorLogin, Instant updatedAt) {
        public static QuestionSummary from(Question q) {
            return new QuestionSummary(q.getId(), q.getCode(), q.getTitle(), q.getType().name(), q.getCompetence(),
                    q.getTopic(), q.getSubtopic(), q.getDifficulty().name(), q.getStatus().name(),
                    q.getStatus().displayName(), q.getAuthorLogin(), q.getUpdatedAt());
        }
    }

    public record TransitionRequest(String comment) {
    }

    public record ChangeHistoryResponse(Long id, String field, String oldValue, String newValue, String changedBy,
                                        Instant changedAt) {
        public static ChangeHistoryResponse from(QuestionChangeHistory h) {
            return new ChangeHistoryResponse(h.getId(), h.getFieldName(), h.getOldValue(), h.getNewValue(),
                    h.getChangedByLogin(), h.getChangedAt());
        }
    }

    public record StateAuditResponse(Long id, String fromState, String toState, String changedBy, String comment,
                                     Instant changedAt) {
        public static StateAuditResponse from(QuestionStateAudit a) {
            return new StateAuditResponse(a.getId(), a.getFromState() == null ? null : a.getFromState().name(),
                    a.getToState().name(), a.getChangedByLogin(), a.getComment(), a.getChangedAt());
        }
    }

    public record PluginInfo(String id, String name, String type) {
        public static PluginInfo from(QuestionPlugin p) {
            return new PluginInfo(p.id(), p.displayName(), p.type().name());
        }
    }

    public record PluginGenerateRequest(String title, String context, String statement, List<String> options,
                                        String correctAnswer, String justification, String bibliography,
                                        String competence, String topic, String subtopic, String difficulty,
                                        String mediaUrl) {
        public PluginRequest toPluginRequest() {
            return new PluginRequest(title, context, statement, options, correctAnswer, justification, bibliography,
                    competence, topic, subtopic, difficulty, mediaUrl);
        }
    }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
            return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                    page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
