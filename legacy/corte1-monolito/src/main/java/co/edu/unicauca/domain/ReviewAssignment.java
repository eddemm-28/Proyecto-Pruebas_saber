package co.edu.unicauca.domain;

import java.time.LocalDateTime;

/**
 * Asignación de un revisor a una pregunta (HU4).
 */
public class ReviewAssignment {
    private final String questionId;
    private final String reviewerLogin;
    private final String assignedBy;
    private final LocalDateTime assignedAt;

    public ReviewAssignment(String questionId, String reviewerLogin, String assignedBy, LocalDateTime assignedAt) {
        this.questionId = questionId;
        this.reviewerLogin = reviewerLogin;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public String getQuestionId() { return questionId; }
    public String getReviewerLogin() { return reviewerLogin; }
    public String getAssignedBy() { return assignedBy; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
