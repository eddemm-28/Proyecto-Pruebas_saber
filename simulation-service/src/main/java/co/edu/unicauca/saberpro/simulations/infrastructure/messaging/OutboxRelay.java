package co.edu.unicauca.saberpro.simulations.infrastructure.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity.OutboxEventEntity;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/** Envía a RabbitMQ los eventos pendientes del outbox (at-least-once, en orden). */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventJpaRepository repository;
    private final EventPublisher publisher;
    private final EventSerializer serializer;
    private final int batchSize;

    public OutboxRelay(OutboxEventJpaRepository repository, EventPublisher publisher, EventSerializer serializer,
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

    public int relayPending() {
        List<OutboxEventEntity> pending = repository.findPending(PageRequest.of(0, batchSize));
        int sent = 0;
        for (OutboxEventEntity event : pending) {
            try {
                EventEnvelope<JsonNode> envelope = serializer.fromJson(event.payload, JsonNode.class);
                publisher.publish(event.routingKey, envelope);
                event.publishedAt = Instant.now();
                event.attempts++;
                event.lastError = null;
                repository.save(event);
                sent++;
            } catch (RuntimeException e) {
                event.attempts++;
                String msg = String.valueOf(e.getMessage());
                event.lastError = msg.length() > 500 ? msg.substring(0, 500) : msg;
                repository.save(event);
                log.warn("No se pudo publicar {} {} (intento {}): {}", event.eventType, event.id, event.attempts, msg);
                break;
            }
        }
        if (sent > 0) {
            log.info("Outbox: {} evento(s) SimulacroFinalizado publicados", sent);
        }
        return sent;
    }
}
