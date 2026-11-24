package co.edu.unicauca.saberpro.questions.persistence;

import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** RF-30 / RNF-09: auditoría de cada transición de estado. */
@Entity
@Table(name = "question_state_audit")
public class QuestionStateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_state", length = 20)
    private QuestionStatus fromState;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_state", nullable = false, length = 20)
    private QuestionStatus toState;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_by_login", nullable = false, length = 50)
    private String changedByLogin;

    @Column(length = 1000)
    private String comment;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected QuestionStateAudit() {
    }

    public QuestionStateAudit(UUID questionId, QuestionStatus from, QuestionStatus to, UUID changedBy,
                              String changedByLogin, String comment) {
        this.questionId = questionId;
        this.fromState = from;
        this.toState = to;
        this.changedBy = changedBy;
        this.changedByLogin = changedByLogin;
        this.comment = comment == null || comment.isBlank() ? null
                : (comment.length() > 1000 ? comment.substring(0, 1000) : comment.trim());
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public QuestionStatus getFromState() { return fromState; }
    public QuestionStatus getToState() { return toState; }
    public UUID getChangedBy() { return changedBy; }
    public String getChangedByLogin() { return changedByLogin; }
    public String getComment() { return comment; }
    public Instant getChangedAt() { return changedAt; }
}
