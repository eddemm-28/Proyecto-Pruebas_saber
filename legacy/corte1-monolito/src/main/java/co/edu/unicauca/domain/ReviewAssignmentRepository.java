package co.edu.unicauca.domain;

import java.util.List;

/**
 * Abstracción de persistencia para las asignaciones de revisores.
 */
public interface ReviewAssignmentRepository {
    boolean save(ReviewAssignment assignment);
    boolean exists(String questionId, String reviewerLogin);
    List<ReviewAssignment> findByQuestion(String questionId);
    List<ReviewAssignment> findByReviewer(String reviewerLogin);
}
