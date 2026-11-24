package co.edu.unicauca.domain;

import java.util.ArrayList;
import java.util.List;

/** Repositorio de asignaciones en memoria solo para pruebas. */
class FakeAssignmentRepository implements ReviewAssignmentRepository {
    private final List<ReviewAssignment> items = new ArrayList<>();

    @Override
    public boolean save(ReviewAssignment assignment) {
        items.add(assignment);
        return true;
    }

    @Override
    public boolean exists(String questionId, String reviewerLogin) {
        for (ReviewAssignment a : items) {
            if (a.getQuestionId().equals(questionId) && a.getReviewerLogin().equals(reviewerLogin)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ReviewAssignment> findByQuestion(String questionId) {
        List<ReviewAssignment> result = new ArrayList<>();
        for (ReviewAssignment a : items) {
            if (a.getQuestionId().equals(questionId)) {
                result.add(a);
            }
        }
        return result;
    }

    @Override
    public List<ReviewAssignment> findByReviewer(String reviewerLogin) {
        List<ReviewAssignment> result = new ArrayList<>();
        for (ReviewAssignment a : items) {
            if (a.getReviewerLogin().equals(reviewerLogin)) {
                result.add(a);
            }
        }
        return result;
    }
}
