package co.edu.unicauca.saberpro.questions.service;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.payload.PreguntaPublicadaEvent;
import co.edu.unicauca.saberpro.questions.TestQuestions;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.policy.ForbiddenOperationException;
import co.edu.unicauca.saberpro.questions.domain.state.InvalidStateTransitionException;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidationException;
import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidator;
import co.edu.unicauca.saberpro.questions.outbox.OutboxWriter;
import co.edu.unicauca.saberpro.questions.persistence.Question;
import co.edu.unicauca.saberpro.questions.persistence.QuestionChangeHistory;
import co.edu.unicauca.saberpro.questions.persistence.QuestionChangeHistoryRepository;
import co.edu.unicauca.saberpro.questions.persistence.QuestionRepository;
import co.edu.unicauca.saberpro.questions.persistence.QuestionStateAudit;
import co.edu.unicauca.saberpro.questions.persistence.QuestionStateAuditRepository;
import co.edu.unicauca.saberpro.questions.plugin.impl.MultipleChoiceQuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.registry.QuestionPluginRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestionServiceTest {

    private QuestionRepository repo;
    private QuestionChangeHistoryRepository historyRepo;
    private QuestionStateAuditRepository auditRepo;
    private OutboxWriter outbox;
    private QuestionService service;

    private final CurrentUser autor = new CurrentUser(UUID.randomUUID(), "autor1", "AUTOR");
    private final CurrentUser revisor = new CurrentUser(UUID.randomUUID(), "rev1", "REVISOR");
    private final CurrentUser admin = new CurrentUser(UUID.randomUUID(), "admin", "ADMINISTRADOR");

    @BeforeEach
    void setUp() {
        repo = mock(QuestionRepository.class);
        historyRepo = mock(QuestionChangeHistoryRepository.class);
        auditRepo = mock(QuestionStateAuditRepository.class);
        outbox = mock(OutboxWriter.class);
        service = new QuestionService(repo, historyRepo, auditRepo, new QuestionValidator(),
                QuestionPluginRegistry.of(List.of(new MultipleChoiceQuestionPlugin())), outbox);
        when(repo.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repo.saveAndFlush(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repo.nextCodeNumber()).thenReturn(7L);
    }

    private Question stored(QuestionStatus status) {
        Question q = Question.create(TestQuestions.valid(), "Q-2026-000007", autor.id(), autor.login(), "MANUAL");
        if (status != QuestionStatus.BORRADOR) {
            q.transitionTo(status);
        }
        when(repo.findById(q.getId())).thenReturn(Optional.of(q));
        return q;
    }

    @Test
    void crearDejaLaPreguntaEnBorradorConCodigoYAuditoria() {
        Question q = service.create(TestQuestions.valid(), autor);
        assertEquals(QuestionStatus.BORRADOR, q.getStatus());
        assertTrue(q.getCode().endsWith("-000007"));
        ArgumentCaptor<QuestionStateAudit> captor = ArgumentCaptor.forClass(QuestionStateAudit.class);
        verify(auditRepo).save(captor.capture());
        assertEquals(null, captor.getValue().getFromState());
        assertEquals(QuestionStatus.BORRADOR, captor.getValue().getToState());
    }

    @Test
    void crearInvalidaNoPersiste() {
        QuestionContent invalida = TestQuestions.withOptions(List.of("Uno", "Dos", "Tres", "Todas las anteriores"), "A");
        assertThrows(QuestionValidationException.class, () -> service.create(invalida, autor));
        verify(repo, never()).save(any());
    }

    @Test
    void estudianteODocenteNoCrean() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.create(TestQuestions.valid(), new CurrentUser(UUID.randomUUID(), "doc", "DOCENTE")));
    }

    @Test
    void pluginGeneraYPersisteConSuId() {
        Question q = service.generateWithPlugin("multiple-choice",
                TestQuestions.pluginRequest("State", TestQuestions.valid().context(), null), autor);
        assertEquals("multiple-choice", q.getPluginId());
        assertEquals("B", q.getCorrectOption());
    }

    @Test
    void editarEnBorradorRegistraHistorialPorCampo() {
        Question q = stored(QuestionStatus.BORRADOR);
        service.update(q.getId(), TestQuestions.withOptions(List.of("Singleton", "State", "Adapter", "Observer"), "B"),
                null, autor);
        ArgumentCaptor<QuestionChangeHistory> captor = ArgumentCaptor.forClass(QuestionChangeHistory.class);
        verify(historyRepo, times(1)).save(captor.capture());
        assertEquals("optionD", captor.getValue().getFieldName());
    }

    @Test
    void noSeEditaFueraDeBorrador() {
        Question q = stored(QuestionStatus.PENDIENTE_REVISION);
        assertThrows(InvalidStateTransitionException.class, () -> service.update(q.getId(), TestQuestions.valid(), null, autor));
    }

    @Test
    void otroAutorNoEdita() {
        Question q = stored(QuestionStatus.BORRADOR);
        CurrentUser otro = new CurrentUser(UUID.randomUUID(), "otro", "AUTOR");
        assertThrows(ForbiddenOperationException.class, () -> service.update(q.getId(), TestQuestions.valid(), null, otro));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicarEncolaPreguntaPublicadaEnElOutbox() {
        Question q = stored(QuestionStatus.APROBADA);
        service.transition(q.getId(), QuestionAction.PUBLISH, null, admin, "cid-1");

        assertEquals(QuestionStatus.PUBLICADA, q.getStatus());
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(outbox).enqueue(eq(q.getId()), eq(EventTopology.RK_QUESTION_PUBLISHED), captor.capture());
        EventEnvelope<PreguntaPublicadaEvent> env = (EventEnvelope<PreguntaPublicadaEvent>) captor.getValue();
        assertEquals(EventTypes.PREGUNTA_PUBLICADA, env.eventType());
        assertEquals("cid-1", env.correlationId());
        assertEquals("B", env.payload().correctOption());
        assertEquals(4, env.payload().options().size());
    }

    @Test
    void archivarPublicadaEmitePreguntaArchivadaPeroArchivarBorradorNo() {
        Question publicada = stored(QuestionStatus.PUBLICADA);
        service.transition(publicada.getId(), QuestionAction.ARCHIVE, null, admin, null);
        verify(outbox).enqueue(eq(publicada.getId()), eq(EventTopology.RK_QUESTION_ARCHIVED), any());

        Question borrador = stored(QuestionStatus.BORRADOR);
        service.transition(borrador.getId(), QuestionAction.ARCHIVE, null, autor, null);
        verify(outbox, never()).enqueue(eq(borrador.getId()), any(), any());
        assertEquals(QuestionStatus.ARCHIVADA, borrador.getStatus());
    }

    @Test
    void flujoMinimoA19ConAuditoria() {
        Question q = stored(QuestionStatus.BORRADOR);
        service.transition(q.getId(), QuestionAction.SUBMIT, null, autor, null);
        service.transition(q.getId(), QuestionAction.START_REVIEW, null, revisor, null);
        service.transition(q.getId(), QuestionAction.APPROVE, "Cumple el formato", revisor, null);
        assertEquals(QuestionStatus.APROBADA, q.getStatus());
        verify(auditRepo, times(3)).save(any(QuestionStateAudit.class));
    }

    @Test
    void transicionInvalidaNoAudita() {
        Question q = stored(QuestionStatus.BORRADOR);
        assertThrows(InvalidStateTransitionException.class,
                () -> service.transition(q.getId(), QuestionAction.PUBLISH, null, admin, null));
        verify(auditRepo, never()).save(any());
        verify(outbox, never()).enqueue(any(), any(), any());
    }
}
