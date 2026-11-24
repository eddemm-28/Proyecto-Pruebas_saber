package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Rechazada: el autor la devuelve a Borrador para corregirla, o se archiva. */
final class RechazadaState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.RECHAZADA; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.RETURN_TO_DRAFT, QuestionAction.ARCHIVE);
    }

    @Override
    public void returnToDraft(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.BORRADOR); }

    @Override
    public void archive(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.ARCHIVADA); }
}
