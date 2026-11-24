package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Aprobada: lista para publicarse en el banco de simulacros. */
final class AprobadaState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.APROBADA; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.PUBLISH, QuestionAction.ARCHIVE);
    }

    @Override
    public void publish(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.PUBLICADA); }

    @Override
    public void archive(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.ARCHIVADA); }
}
