package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumSet;
import java.util.Set;

/**
 * En revisión: se aprueba o se rechaza. En el Corte 3 (HU05) aquí se conectará la revisión
 * por pares completa (asignación, formato de evaluación, observaciones).
 */
final class EnRevisionState implements QuestionState {

    @Override
    public QuestionStatus status() { return QuestionStatus.EN_REVISION; }

    @Override
    public Set<QuestionAction> allowedActions() {
        return EnumSet.of(QuestionAction.APPROVE, QuestionAction.REJECT);
    }

    @Override
    public void approve(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.APROBADA); }

    @Override
    public void reject(QuestionStateContext ctx) { ctx.transitionTo(QuestionStatus.RECHAZADA); }
}
