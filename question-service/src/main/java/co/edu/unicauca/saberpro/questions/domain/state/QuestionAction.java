package co.edu.unicauca.saberpro.questions.domain.state;

/** Acciones que disparan transiciones de estado. */
public enum QuestionAction {
    SUBMIT,
    START_REVIEW,
    APPROVE,
    REJECT,
    RETURN_TO_DRAFT,
    PUBLISH,
    ARCHIVE
}
