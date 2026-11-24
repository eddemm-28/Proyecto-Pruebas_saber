package co.edu.unicauca.saberpro.simulations.infrastructure.persistence;

import co.edu.unicauca.saberpro.simulations.application.port.out.ProcessedEventStore;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.ProcessedEventEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Idempotencia: la fila se inserta en la misma transacción que el efecto del evento. Si dos
 * entregas del mismo evento llegan a la vez, la PK hace fallar una y RabbitMQ la reintenta,
 * momento en que ya aparece como procesada.
 */
@Component
public class ProcessedEventPersistenceAdapter implements ProcessedEventStore {

    private final ProcessedEventJpaRepository jpa;

    public ProcessedEventPersistenceAdapter(ProcessedEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean markProcessed(UUID eventId, String eventType) {
        if (jpa.existsById(eventId)) {
            return false;
        }
        jpa.save(new ProcessedEventEntity(eventId, eventType));
        return true;
    }
}
