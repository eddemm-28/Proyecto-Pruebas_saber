package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Publicada: disponible para simulacros. Solo puede archivarse (emite PreguntaArchivada). */
final class PublicadaState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.PUBLICADA; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.ARCHIVE);
    }

    @Override
    public void archive(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.ARCHIVADA); }
}
