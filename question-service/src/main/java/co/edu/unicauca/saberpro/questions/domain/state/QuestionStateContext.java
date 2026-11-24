package co.edu.unicauca.saberpro.questions.domain.state;

/** Contexto del patrón State: la pregunta delega en su estado y este le indica el siguiente. */
public interface QuestionStateContext {

    QuestionStatus getStatus();

    void transitionTo(QuestionStatus next);
}
