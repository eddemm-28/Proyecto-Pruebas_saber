package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Borrador: editable; se envía a revisión o se archiva. */
final class BorradorState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.BORRADOR; }

    @Override
    public boolean isEditable() { return true; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.SUBMIT, QuestionAction.ARCHIVE);
    }

    @Override
    public void submit(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.PENDIENTE_REVISION); }

    @Override
    public void archive(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.ARCHIVADA); }
}
