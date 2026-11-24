package co.edu.unicauca.saberpro.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sobre común de todos los eventos de dominio que viajan por RabbitMQ.
 *
 * @param eventId       identificador único; los consumidores lo usan para la idempotencia
 * @param eventType     nombre del evento (ver {@link EventTypes})
 * @param eventVersion  versión del contrato del payload
 * @param occurredAt    instante en que ocurrió el hecho de negocio (UTC)
 * @param producer      servicio que lo originó
 * @param correlationId traza de la petición original (puede ser {@code null})
 * @param payload       datos del evento
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String producer,
        String correlationId,
        T payload) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(producer, "producer");
        Objects.requireNonNull(payload, "payload");
    }

    public static <T> EventEnvelope<T> of(String eventType, String producer, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, 1, Instant.now(), producer, null, payload);
    }

    public static <T> EventEnvelope<T> of(UUID eventId, String eventType, String producer, String correlationId, T payload) {
        return new EventEnvelope<>(eventId, eventType, 1, Instant.now(), producer, correlationId, payload);
    }
}
