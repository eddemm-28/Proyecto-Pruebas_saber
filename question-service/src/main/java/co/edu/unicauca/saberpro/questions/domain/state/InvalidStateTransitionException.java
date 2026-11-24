package co.edu.unicauca.saberpro.questions.domain.state;

/** Transición no permitida desde el estado actual → HTTP 409. */
public class InvalidStateTransitionException extends RuntimeException {

    private final QuestionStatus from;
    private final QuestionAction action;

    public InvalidStateTransitionException(QuestionStatus from, QuestionAction action) {
        super("No se puede ejecutar " + action + " sobre una pregunta en estado \"" + from.displayName() + "\"");
        this.from = from;
        this.action = action;
    }

    public InvalidStateTransitionException(QuestionStatus from, String message) {
        super(message);
        this.from = from;
        this.action = null;
    }

    public QuestionStatus getFrom() { return from; }
    public QuestionAction getAction() { return action; }
}
