package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.Set;

/**
 * Patrón State (A13). Cada estado concreto decide qué acciones acepta y a qué estado lleva.
 * Por defecto toda acción es inválida; cada estado sobrescribe solo las que permite.
 */
public interface QuestionState {

    QuestionStatus status();

    /** RF-06: solo el Borrador es editable. */
    default boolean isEditable() {
        return false;
    }

    Set<QuestionAction> allowedActions();

    default void submit(QuestionStateContext ctx) { throw invalid(QuestionAction.SUBMIT); }

    default void startReview(QuestionStateContext ctx) { throw invalid(QuestionAction.START_REVIEW); }

    default void approve(QuestionStateContext ctx) { throw invalid(QuestionAction.APPROVE); }

    default void reject(QuestionStateContext ctx) { throw invalid(QuestionAction.REJECT); }

    default void returnToDraft(QuestionStateContext ctx) { throw invalid(QuestionAction.RETURN_TO_DRAFT); }

    default void publish(QuestionStateContext ctx) { throw invalid(QuestionAction.PUBLISH); }

    default void archive(QuestionStateContext ctx) { throw invalid(QuestionAction.ARCHIVE); }

    default InvalidStateTransitionException invalid(QuestionAction action) {
        return new InvalidStateTransitionException(status(), action);
    }

    /** Despacha una acción al método correspondiente del estado. */
    default void apply(QuestionAction action, QuestionStateContext ctx) {
        switch (action) {
            case SUBMIT -> submit(ctx);
            case START_REVIEW -> startReview(ctx);
            case APPROVE -> approve(ctx);
            case REJECT -> reject(ctx);
            case RETURN_TO_DRAFT -> returnToDraft(ctx);
            case PUBLISH -> publish(ctx);
            case ARCHIVE -> archive(ctx);
            default -> throw invalid(action);
        }
    }
}
