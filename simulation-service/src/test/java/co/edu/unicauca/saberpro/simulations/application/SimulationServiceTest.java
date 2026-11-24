package co.edu.unicauca.saberpro.simulations.application;

import co.edu.unicauca.saberpro.simulations.application.dto.AnswerOutcome;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationResultView;
import co.edu.unicauca.saberpro.simulations.application.dto.SimulationView;
import co.edu.unicauca.saberpro.simulations.application.dto.StudentRef;
import co.edu.unicauca.saberpro.simulations.application.service.QuestionReplicaService;
import co.edu.unicauca.saberpro.simulations.application.service.SimulationService;
import co.edu.unicauca.saberpro.simulations.domain.exception.ActiveSimulationExistsException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationAccessDeniedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationClosedException;
import co.edu.unicauca.saberpro.simulations.domain.model.QuestionSnapshot;
import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationConfig;
import co.edu.unicauca.saberpro.simulations.domain.model.SimulationStatus;
import co.edu.unicauca.saberpro.simulations.domain.model.StrategyType;
import co.edu.unicauca.saberpro.simulations.domain.scoring.ScoringPolicy;
import co.edu.unicauca.saberpro.simulations.domain.strategy.SelectionStrategyResolver;
import co.edu.unicauca.saberpro.simulations.support.Fakes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

import static co.edu.unicauca.saberpro.simulations.support.Fakes.question;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** B06–B09 y B05 sobre el hexágono con adaptadores en memoria. */
class SimulationServiceTest {

    private Fakes.InMemoryReplica replica;
    private Fakes.InMemorySimulations simulations;
    private Fakes.RecordingPublisher publisher;
    private Fakes.MutableClock clock;
    private SimulationService service;
    private final StudentRef student = new StudentRef(UUID.randomUUID(), "est1");

    @BeforeEach
    void setUp() {
        replica = new Fakes.InMemoryReplica();
        for (int i = 0; i < 3; i++) replica.add(question("Lectura crítica", "Inferencia", "MEDIA", "A"));
        for (int i = 0; i < 2; i++) replica.add(question("Razonamiento cuantitativo", "Estadística", "MEDIA", "B"));
        simulations = new Fakes.InMemorySimulations();
        publisher = new Fakes.RecordingPublisher();
        clock = new Fakes.MutableClock(Instant.parse("2026-11-25T15:00:00Z"));
        service = new SimulationService(simulations, replica, publisher, new SelectionStrategyResolver(),
                new ScoringPolicy(), clock, Duration.ofSeconds(2), new Random(7));
    }

    private SimulationView start(int n, int minutes) {
        return service.start(student, new SimulationConfig(StrategyType.MIXED, null, null, null, n, minutes));
    }

    @Test
    void elSimulacroNoExponeLasRespuestasCorrectasYFijaElTiempoEnServidor() {
        SimulationView v = start(5, 10);
        assertEquals("IN_PROGRESS", v.status());
        assertEquals(Instant.parse("2026-11-25T15:10:00Z"), v.expiresAt());
        assertEquals(600, v.remainingSeconds());
        assertEquals(5, v.items().size());
        assertTrue(v.items().stream().allMatch(i -> i.selectedOption() == null && i.options().size() == 4));
    }

    @Test
    void calificaPorCompetenciaYPublicaSimulacroFinalizado() {
        SimulationView v = start(5, 10);
        Simulation sim = simulations.store.get(v.id());
        // Responde bien todas las de Lectura crítica (A) y mal las de Razonamiento (responde A en vez de B)
        sim.getItems().forEach(i -> service.answer(v.id(), i.getQuestionId(), "A", student.id(), null));
        clock.advance(Duration.ofMinutes(3));

        SimulationResultView r = service.finish(v.id(), student.id(), "cid");

        assertEquals("FINISHED", r.status());
        assertEquals(3, r.correctAnswers());
        assertEquals(60.0, r.globalScore(), 0.001);
        SimulationResultView.CompetenceScoreView lectura = r.competenceScores().stream()
                .filter(c -> c.competence().equals("Lectura crítica")).findFirst().orElseThrow();
        assertEquals(100.0, lectura.score(), 0.001);
        SimulationResultView.CompetenceScoreView rq = r.competenceScores().stream()
                .filter(c -> c.competence().equals("Razonamiento cuantitativo")).findFirst().orElseThrow();
        assertEquals(0.0, rq.score(), 0.001);
        assertEquals(1, publisher.published.size());
        assertTrue(r.items().stream().allMatch(i -> i.justification() != null && i.correctOption() != null));
    }

    @Test
    void sinResponderCuentaComoIncorrecta() {
        SimulationView v = start(4, 10);
        SimulationResultView r = service.finish(v.id(), student.id(), null);
        assertEquals(0, r.correctAnswers());
        assertEquals(0, r.answeredQuestions());
        assertEquals(0.0, r.globalScore(), 0.001);
    }

    @Test
    void respuestaFueraDeTiempoSeRechazaYCierraComoExpirado() {
        SimulationView v = start(3, 1);
        UUID q = v.items().get(0).questionId();
        clock.advance(Duration.ofSeconds(61)); // dentro del margen de 2 s
        assertTrue(service.answer(v.id(), q, "A", student.id(), null).accepted());

        clock.advance(Duration.ofSeconds(5)); // ya fuera del margen
        AnswerOutcome late = service.answer(v.id(), q, "B", student.id(), null);
        assertFalse(late.accepted());
        assertEquals("EXPIRED", late.status());
        assertEquals(SimulationStatus.EXPIRED, simulations.store.get(v.id()).getStatus());
        assertEquals("A", simulations.store.get(v.id()).getItems().stream()
                .filter(i -> i.getQuestionId().equals(q)).findFirst().orElseThrow().getSelectedOption(),
                "la respuesta tardía no reemplaza a la anterior");
        assertEquals(1, publisher.published.size());
    }

    @Test
    void elPlanificadorCierraLosVencidos() {
        start(2, 5);
        clock.advance(Duration.ofMinutes(6));
        assertEquals(1, service.expireOverdue());
        assertEquals(0, service.expireOverdue());
        assertEquals(1, publisher.published.size());
    }

    @Test
    void unSoloSimulacroActivoPorEstudianteSalvoQueHayaVencido() {
        start(2, 5);
        assertThrows(ActiveSimulationExistsException.class, () -> start(2, 5));
        clock.advance(Duration.ofMinutes(10));
        SimulationView nuevo = start(2, 5);
        assertEquals("IN_PROGRESS", nuevo.status());
        assertEquals(1, publisher.published.size(), "el vencido se cerró y se publicó");
    }

    @Test
    void otroEstudianteNoPuedeVerNiResponder() {
        SimulationView v = start(2, 5);
        UUID otro = UUID.randomUUID();
        assertThrows(SimulationAccessDeniedException.class, () -> service.get(v.id(), otro, null));
        assertThrows(SimulationAccessDeniedException.class,
                () -> service.answer(v.id(), v.items().get(0).questionId(), "A", otro, null));
    }

    @Test
    void noSePuedeFinalizarDosVeces() {
        SimulationView v = start(2, 5);
        service.finish(v.id(), student.id(), null);
        assertThrows(SimulationClosedException.class, () -> service.finish(v.id(), student.id(), null));
    }

    @Test
    void resultadoSoloCuandoTermina() {
        SimulationView v = start(2, 5);
        assertThrows(SimulationClosedException.class, () -> service.result(v.id(), student.id()));
    }

    @Test
    void activoPermiteRetomar() {
        SimulationView v = start(2, 5);
        assertEquals(v.id(), service.active(student.id(), null).orElseThrow().id());
        clock.advance(Duration.ofMinutes(10));
        assertTrue(service.active(student.id(), null).isEmpty());
    }

    // ---------------------------------------------------------------- B05

    @Test
    void replicaEsIdempotenteYDescartaVersionesViejas() {
        Fakes.InMemoryProcessed processed = new Fakes.InMemoryProcessed();
        QuestionReplicaService sync = new QuestionReplicaService(replica, processed);
        QuestionSnapshot base = question("Inglés", "Lectura", "FACIL", "C");
        QuestionSnapshot v3 = new QuestionSnapshot(base.questionId(), base.code(), base.context(), base.statement(),
                base.options(), base.correctOption(), base.justification(), base.competence(), base.topic(),
                base.subtopic(), base.difficulty(), 3, true, base.publishedAt());
        UUID e1 = UUID.randomUUID();

        assertTrue(sync.onQuestionPublished(e1, v3));
        assertFalse(sync.onQuestionPublished(e1, v3), "evento duplicado");
        assertFalse(sync.onQuestionPublished(UUID.randomUUID(), base), "versión 1 < 3: se ignora");
        assertTrue(replica.findById(base.questionId()).orElseThrow().available());

        assertTrue(sync.onQuestionArchived(UUID.randomUUID(), base.questionId(), 4));
        assertFalse(replica.findById(base.questionId()).orElseThrow().available());
        assertFalse(sync.onQuestionArchived(UUID.randomUUID(), UUID.randomUUID(), 1), "pregunta desconocida");
    }

    @Test
    void snapshotsDeUnSimulacroSobrevivenAlArchivado() {
        SimulationView v = start(5, 10);
        replica.store.replaceAll((id, q) -> q.archived(9));
        SimulationResultView r = service.finish(v.id(), student.id(), null);
        assertEquals(5, r.items().size());
        assertTrue(r.items().stream().allMatch(i -> i.statement() != null));
        assertNull(r.items().get(0).selectedOption());
    }
}
