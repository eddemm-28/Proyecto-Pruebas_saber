package co.edu.unicauca.saberpro.questions.domain;

import co.edu.unicauca.saberpro.questions.TestQuestions;
import co.edu.unicauca.saberpro.questions.domain.model.ContentDiff;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.policy.ForbiddenOperationException;
import co.edu.unicauca.saberpro.questions.domain.policy.QuestionPermissions;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionPermissionsAndDiffTest {

    @Test
    void autorEnviaSuPreguntaPeroNoLaDeOtro() {
        assertDoesNotThrow(() -> QuestionPermissions.checkCanApply(QuestionAction.SUBMIT, QuestionStatus.BORRADOR, "AUTOR", true, null));
        assertThrows(ForbiddenOperationException.class,
                () -> QuestionPermissions.checkCanApply(QuestionAction.SUBMIT, QuestionStatus.BORRADOR, "AUTOR", false, null));
    }

    @Test
    void soloAdminPublica() {
        assertThrows(ForbiddenOperationException.class,
                () -> QuestionPermissions.checkCanApply(QuestionAction.PUBLISH, QuestionStatus.APROBADA, "REVISOR", false, null));
        assertDoesNotThrow(() -> QuestionPermissions.checkCanApply(QuestionAction.PUBLISH, QuestionStatus.APROBADA, "ADMINISTRADOR", false, null));
    }

    @Test
    void autorNoSeAutoapruebaYRechazoExigeMotivo() {
        assertThrows(ForbiddenOperationException.class,
                () -> QuestionPermissions.checkCanApply(QuestionAction.APPROVE, QuestionStatus.EN_REVISION, "ADMINISTRADOR", true, null));
        assertThrows(IllegalArgumentException.class,
                () -> QuestionPermissions.checkCanApply(QuestionAction.REJECT, QuestionStatus.EN_REVISION, "REVISOR", false, " "));
        assertDoesNotThrow(() -> QuestionPermissions.checkCanApply(QuestionAction.REJECT, QuestionStatus.EN_REVISION, "REVISOR", false, "Distractor B ambiguo"));
    }

    @Test
    void autorSoloArchivaBorradores() {
        assertDoesNotThrow(() -> QuestionPermissions.checkCanApply(QuestionAction.ARCHIVE, QuestionStatus.BORRADOR, "AUTOR", true, null));
        assertThrows(ForbiddenOperationException.class,
                () -> QuestionPermissions.checkCanApply(QuestionAction.ARCHIVE, QuestionStatus.PUBLICADA, "AUTOR", true, null));
    }

    @Test
    void diffDetectaSoloLosCamposCambiados() {
        QuestionContent before = TestQuestions.valid();
        QuestionContent after = TestQuestions.withOptions(List.of("Singleton", "State", "Adapter", "Observer"), "B");
        List<ContentDiff.FieldChange> changes = ContentDiff.between(before, after);
        assertEquals(1, changes.size());
        assertEquals("optionD", changes.get(0).field());
        assertEquals("Facade", changes.get(0).oldValue());
        assertEquals("Observer", changes.get(0).newValue());
        assertTrue(ContentDiff.between(before, TestQuestions.valid()).isEmpty());
    }
}
