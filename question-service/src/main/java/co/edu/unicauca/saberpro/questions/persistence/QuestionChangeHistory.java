package co.edu.unicauca.saberpro.questions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** RF-29: un registro por campo modificado. */
@Entity
@Table(name = "question_change_history")
public class QuestionChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_by_login", nullable = false, length = 50)
    private String changedByLogin;

    @Column(name = "field_name", nullable = false, length = 40)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected QuestionChangeHistory() {
    }

    public QuestionChangeHistory(UUID questionId, UUID changedBy, String changedByLogin, String fieldName,
                                 String oldValue, String newValue) {
        this.questionId = questionId;
        this.changedBy = changedBy;
        this.changedByLogin = changedByLogin;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public UUID getChangedBy() { return changedBy; }
    public String getChangedByLogin() { return changedByLogin; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Instant getChangedAt() { return changedAt; }
}
