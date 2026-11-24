package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Pendiente de revisión: espera que un revisor/administrador la tome (A19 mínimo). */
final class PendienteRevisionState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.PENDIENTE_REVISION; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.START_REVIEW, QuestionAction.ARCHIVE);
    }

    @Override
    public void startReview(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.EN_REVISION); }

    @Override
    public void archive(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.ARCHIVADA); }
}
