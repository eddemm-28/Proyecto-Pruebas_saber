package co.edu.unicauca.saberpro.questions.persistence;

import co.edu.unicauca.saberpro.questions.domain.model.ContentDiff;
import co.edu.unicauca.saberpro.questions.domain.model.Difficulty;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.domain.state.InvalidStateTransitionException;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionState;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStateContext;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStates;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado Pregunta (A12). Es el <b>contexto</b> del patrón State (A13): no decide las
 * transiciones, se las delega a {@link QuestionState} según su estado actual.
 */
@Entity
@Table(name = "questions")
public class Question implements QuestionStateContext {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false, columnDefinition = "text")
    private String context;

    @Column(nullable = false, columnDefinition = "text")
    private String statement;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("label ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption;

    @Column(nullable = false, columnDefinition = "text")
    private String justification;

    @Column(nullable = false, columnDefinition = "text")
    private String bibliography;

    @Column(nullable = false, length = 100)
    private String competence;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(nullable = false, length = 100)
    private String subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private QuestionStatus status;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "author_login", nullable = false, length = 50)
    private String authorLogin;

    @Column(name = "plugin_id", nullable = false, length = 40)
    private String pluginId;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Version
    private long version;

    @Transient
    private QuestionStatus previousStatus;

    protected Question() {
    }

    public static Question create(QuestionContent content, String code, UUID authorId, String authorLogin, String pluginId) {
        Question q = new Question();
        q.id = UUID.randomUUID();
        q.code = code;
        q.authorId = authorId;
        q.authorLogin = authorLogin;
        q.pluginId = pluginId == null ? "MANUAL" : pluginId;
        q.status = QuestionStatus.BORRADOR;
        q.createdAt = Instant.now();
        q.applyContent(content);
        return q;
    }

    // ----------------------- Patrón State -----------------------

    public QuestionState state() {
        return QuestionStates.of(status);
    }

    @Override
    public QuestionStatus getStatus() {
        return status;
    }

    @Override
    public void transitionTo(QuestionStatus next) {
        this.previousStatus = this.status;
        this.status = next;
        Instant now = Instant.now();
        this.updatedAt = now;
        if (next == QuestionStatus.PUBLICADA) {
            this.publishedAt = now;
        } else if (next == QuestionStatus.ARCHIVADA) {
            this.archivedAt = now;
        }
    }

    /** Ejecuta la acción delegando en el estado actual. @return el estado anterior */
    public QuestionStatus apply(QuestionAction action) {
        QuestionStatus from = status;
        state().apply(action, this);
        return from;
    }

    public Set<QuestionAction> allowedActions() {
        return state().allowedActions();
    }

    // ----------------------- Contenido -----------------------

    /** RF-06: solo se edita en Borrador. @return cambios campo a campo para el historial (RF-29). */
    public List<ContentDiff.FieldChange> updateContent(QuestionContent newContent) {
        if (!state().isEditable()) {
            throw new InvalidStateTransitionException(status,
                    "Solo se pueden editar preguntas en Borrador (estado actual: " + status.displayName() + ")");
        }
        List<ContentDiff.FieldChange> changes = ContentDiff.between(content(), newContent);
        if (!changes.isEmpty()) {
            applyContent(newContent);
        }
        return changes;
    }

    private void applyContent(QuestionContent c) {
        this.title = c.title();
        this.type = c.type();
        this.context = c.context();
        this.statement = c.statement();
        this.options.clear();
        for (int i = 0; i < c.options().size(); i++) {
            this.options.add(new QuestionOption(QuestionContent.LABELS.get(i), c.options().get(i)));
        }
        this.correctOption = c.correctOption();
        this.justification = c.justification();
        this.bibliography = c.bibliography();
        this.competence = c.competence();
        this.topic = c.topic();
        this.subtopic = c.subtopic();
        this.difficulty = Difficulty.tryParse(c.difficulty()).orElseThrow();
        this.mediaUrl = c.mediaUrl();
        this.updatedAt = Instant.now();
    }

    public QuestionContent content() {
        return new QuestionContent(title, type, context, statement,
                options.stream().map(QuestionOption::getText).toList(), correctOption, justification,
                bibliography, competence, topic, subtopic, difficulty == null ? null : difficulty.name(), mediaUrl);
    }

    public boolean isOwnedBy(UUID userId) {
        return authorId.equals(userId);
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public QuestionType getType() { return type; }
    public String getContext() { return context; }
    public String getStatement() { return statement; }
    public List<QuestionOption> getOptions() { return List.copyOf(options); }
    public String getCorrectOption() { return correctOption; }
    public String getJustification() { return justification; }
    public String getBibliography() { return bibliography; }
    public String getCompetence() { return competence; }
    public String getTopic() { return topic; }
    public String getSubtopic() { return subtopic; }
    public Difficulty getDifficulty() { return difficulty; }
    public UUID getAuthorId() { return authorId; }
    public String getAuthorLogin() { return authorLogin; }
    public String getPluginId() { return pluginId; }
    public String getMediaUrl() { return mediaUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public long getVersion() { return version; }
    public QuestionStatus getPreviousStatus() { return previousStatus; }
}
