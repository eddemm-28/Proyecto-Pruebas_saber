package co.edu.unicauca.saberpro.questions.outbox;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Publica en RabbitMQ los eventos pendientes del outbox. Si el broker está caído, los eventos
 * se quedan en la tabla y se reintentan en el siguiente ciclo: no se pierde ninguno.
 * La entrega es at-least-once; los consumidores son idempotentes por {@code eventId}.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final EventPublisher publisher;
    private final EventSerializer serializer;
    private final int batchSize;

    public OutboxRelay(OutboxEventRepository repository, EventPublisher publisher, EventSerializer serializer,
                       @Value("${outbox.batch-size:50}") int batchSize) {
        this.repository = repository;
        this.publisher = publisher;
        this.serializer = serializer;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval-ms:2000}", initialDelay = 5000)
    public void scheduledRelay() {
        relayPending();
    }

    /** @return número de eventos publicados en este ciclo */
    public int relayPending() {
        List<OutboxEvent> pending = repository.findPending(PageRequest.of(0, batchSize));
        int sent = 0;
        for (OutboxEvent event : pending) {
            try {
                EventEnvelope<JsonNode> envelope = serializer.fromJson(event.getPayload(), JsonNode.class);
                publisher.publish(event.getRoutingKey(), envelope);
                event.markPublished();
                sent++;
            } catch (RuntimeException e) {
                event.markFailed(e.getMessage());
                log.warn("No se pudo publicar {} {} (intento {}): {}", event.getEventType(), event.getId(),
                        event.getAttempts(), e.getMessage());
                repository.save(event);
                break; // conserva el orden: no se adelantan eventos posteriores
            }
            repository.save(event);
        }
        if (sent > 0) {
            log.info("Outbox: {} evento(s) publicados", sent);
        }
        return sent;
    }
}
