package co.edu.unicauca.saberpro.simulations.infrastructure.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import co.edu.unicauca.saberpro.events.payload.SimulacroFinalizadoEvent;
import co.edu.unicauca.saberpro.simulations.application.port.out.SimulationEventPublisher;
import co.edu.unicauca.saberpro.simulations.domain.model.Simulation;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.OutboxEventEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * B09: adaptador del puerto {@link SimulationEventPublisher}. Escribe {@code SimulacroFinalizado}
 * en el outbox dentro de la transacción del cierre; {@link OutboxRelay} lo envía a RabbitMQ.
 */
@Component
public class OutboxSimulationEventPublisher implements SimulationEventPublisher {

    static final String PRODUCER = "simulation-service";

    private final OutboxEventJpaRepository outbox;
    private final EventSerializer serializer;

    public OutboxSimulationEventPublisher(OutboxEventJpaRepository outbox, EventSerializer serializer) {
        this.outbox = outbox;
        this.serializer = serializer;
    }

    @Override
    public void publishFinished(Simulation s, String correlationId) {
        EventEnvelope<SimulacroFinalizadoEvent> envelope = EventEnvelope.of(UUID.randomUUID(),
                EventTypes.SIMULACRO_FINALIZADO, PRODUCER, correlationId, toEvent(s));
        OutboxEventEntity e = new OutboxEventEntity();
        e.id = envelope.eventId();
        e.aggregateId = s.getId();
        e.eventType = envelope.eventType();
        e.routingKey = EventTopology.RK_SIMULATION_FINISHED;
        e.payload = serializer.toJson(envelope);
        e.createdAt = Instant.now();
        outbox.save(e);
    }

    static SimulacroFinalizadoEvent toEvent(Simulation s) {
        return new SimulacroFinalizadoEvent(
                s.getId(), s.getStudentId(), s.getStudentLogin(), s.getStatus().name(),
                s.getConfig().strategy().name(), s.getStartedAt(), s.getFinishedAt(), s.getItems().size(),
                s.getCorrectCount(), s.getGlobalScore(),
                s.getCompetenceScores().stream().map(c -> new SimulacroFinalizadoEvent.CompetenceScorePayload(
                        c.competence(), c.totalQuestions(), c.correctAnswers(), c.score())).toList(),
                s.getItems().stream().map(i -> new SimulacroFinalizadoEvent.AnswerPayload(i.getQuestionId(),
                        i.getCompetence(), i.getTopic(), i.getDifficulty(), i.getSelectedOption(),
                        Boolean.TRUE.equals(i.getCorrect()))).toList());
    }
}
