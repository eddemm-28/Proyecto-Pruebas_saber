package co.edu.unicauca.saberpro.questions.domain.state;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A13: transiciones válidas e inválidas del patrón State. */
class QuestionStateTest {

    /** Contexto mínimo para probar los estados sin JPA. */
    static final class Ctx implements QuestionStateContext {
        QuestionStatus status;

        Ctx(QuestionStatus status) {
            this.status = status;
        }

        @Override
        public QuestionStatus getStatus() {
            return status;
        }

        @Override
        public void transitionTo(QuestionStatus next) {
            status = next;
        }

        void apply(QuestionAction action) {
            QuestionStates.of(status).apply(action, this);
        }
    }

    @Test
    void flujoFelizHastaPublicadaYArchivada() {
        Ctx q = new Ctx(QuestionStatus.BORRADOR);
        q.apply(QuestionAction.SUBMIT);
        assertEquals(QuestionStatus.PENDIENTE_REVISION, q.status);
        q.apply(QuestionAction.START_REVIEW);
        assertEquals(QuestionStatus.EN_REVISION, q.status);
        q.apply(QuestionAction.APPROVE);
        assertEquals(QuestionStatus.APROBADA, q.status);
        q.apply(QuestionAction.PUBLISH);
        assertEquals(QuestionStatus.PUBLICADA, q.status);
        q.apply(QuestionAction.ARCHIVE);
        assertEquals(QuestionStatus.ARCHIVADA, q.status);
    }

    @Test
    void rechazoYCorreccion() {
        Ctx q = new Ctx(QuestionStatus.EN_REVISION);
        q.apply(QuestionAction.REJECT);
        assertEquals(QuestionStatus.RECHAZADA, q.status);
        q.apply(QuestionAction.RETURN_TO_DRAFT);
        assertEquals(QuestionStatus.BORRADOR, q.status);
    }

    @Test
    void soloElBorradorEsEditable() {
        for (QuestionStatus s : QuestionStatus.values()) {
            assertEquals(s == QuestionStatus.BORRADOR, QuestionStates.of(s).isEditable(), s.name());
        }
    }

    @Test
    void archivadaEsEstadoFinal() {
        Ctx q = new Ctx(QuestionStatus.ARCHIVADA);
        for (QuestionAction a : QuestionAction.values()) {
            assertThrows(InvalidStateTransitionException.class, () -> q.apply(a));
        }
        assertEquals(QuestionStatus.ARCHIVADA, q.status);
    }

    @Test
    void noSePuedePublicarSinAprobar() {
        Ctx q = new Ctx(QuestionStatus.BORRADOR);
        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> q.apply(QuestionAction.PUBLISH));
        assertEquals(QuestionStatus.BORRADOR, ex.getFrom());
        assertEquals(QuestionStatus.BORRADOR, q.status, "una transición inválida no cambia el estado");
    }

    /** Matriz completa: cada acción permitida lleva al estado esperado; las demás fallan. */
    @Test
    void matrizCompletaDeTransiciones() {
        Map<QuestionStatus, Map<QuestionAction, QuestionStatus>> expected = Map.of(
                QuestionStatus.BORRADOR, Map.of(QuestionAction.SUBMIT, QuestionStatus.PENDIENTE_REVISION,
                        QuestionAction.ARCHIVE, QuestionStatus.ARCHIVADA),
                QuestionStatus.PENDIENTE_REVISION, Map.of(QuestionAction.START_REVIEW, QuestionStatus.EN_REVISION,
                        QuestionAction.ARCHIVE, QuestionStatus.ARCHIVADA),
                QuestionStatus.EN_REVISION, Map.of(QuestionAction.APPROVE, QuestionStatus.APROBADA,
                        QuestionAction.REJECT, QuestionStatus.RECHAZADA),
                QuestionStatus.APROBADA, Map.of(QuestionAction.PUBLISH, QuestionStatus.PUBLICADA,
                        QuestionAction.ARCHIVE, QuestionStatus.ARCHIVADA),
                QuestionStatus.RECHAZADA, Map.of(QuestionAction.RETURN_TO_DRAFT, QuestionStatus.BORRADOR,
                        QuestionAction.ARCHIVE, QuestionStatus.ARCHIVADA),
                QuestionStatus.PUBLICADA, Map.of(QuestionAction.ARCHIVE, QuestionStatus.ARCHIVADA),
                QuestionStatus.ARCHIVADA, Map.of());

        for (QuestionStatus from : QuestionStatus.values()) {
            Set<QuestionAction> allowed = QuestionStates.of(from).allowedActions();
            assertEquals(expected.get(from).keySet(), allowed.isEmpty() ? Set.of() : EnumSet.copyOf(allowed), from.name());
            for (QuestionAction action : QuestionAction.values()) {
                Ctx q = new Ctx(from);
                if (expected.get(from).containsKey(action)) {
                    q.apply(action);
                    assertEquals(expected.get(from).get(action), q.status, from + " + " + action);
                } else {
                    assertThrows(InvalidStateTransitionException.class, () -> q.apply(action));
                    assertTrue(q.status == from);
                }
            }
        }
        assertFalse(QuestionStates.of(QuestionStatus.PUBLICADA).allowedActions().contains(QuestionAction.SUBMIT));
    }
}
