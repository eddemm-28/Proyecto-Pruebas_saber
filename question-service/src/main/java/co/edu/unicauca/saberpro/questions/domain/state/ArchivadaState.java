package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/** Archivada: estado final. Sustituye al borrado físico (RNF-16). */
final class ArchivadaState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.ARCHIVADA; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.noneOf(QuestionAction.class);
    }
}
