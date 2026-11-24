package co.edu.unicauca.saberpro.questions.outbox;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Encola un evento en el outbox; exige una transacción activa (la del cambio de negocio). */
@Component
public class OutboxWriter {

    private final OutboxEventRepository repository;
    private final EventSerializer serializer;

    public OutboxWriter(OutboxEventRepository repository, EventSerializer serializer) {
        this.repository = repository;
        this.serializer = serializer;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(UUID aggregateId, String routingKey, EventEnvelope<?> envelope) {
        repository.save(new OutboxEvent(envelope.eventId(), aggregateId, envelope.eventType(), routingKey,
                serializer.toJson(envelope)));
    }
}
