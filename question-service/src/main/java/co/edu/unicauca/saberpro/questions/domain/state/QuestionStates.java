package co.edu.unicauca.saberpro.questions.domain.state;

import java.util.EnumMap;
import java.util.Map;

/** Fábrica de estados: los estados no tienen atributos, así que se comparten (Flyweight). */
public final class QuestionStates {

    private static final Map<QuestionStatus, QuestionState> STATES = new EnumMap<>(QuestionStatus.class);

    static {
        register(new BorradorState());
        register(new PendienteRevisionState());
        register(new EnRevisionState());
        register(new AprobadaState());
        register(new RechazadaState());
        register(new PublicadaState());
        register(new ArchivadaState());
    }

    private QuestionStates() {
    }

    private static void register(QuestionState state) {
        STATES.put(state.status(), state);
    }

    public static QuestionState of(QuestionStatus status) {
        QuestionState state = STATES.get(status);
        if (state == null) {
            throw new IllegalArgumentException("Estado sin implementación: " + status);
        }
        return state;
    }
}
