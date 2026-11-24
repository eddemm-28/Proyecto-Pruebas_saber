package co.edu.unicauca.saberpro.events.rabbit;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.messaging.EventHandler;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

/**
 * Adaptador genérico de consumo: convierte el {@link Message} AMQP en {@link EventEnvelope}
 * y lo entrega al {@link EventHandler}. Un JSON corrupto va directo a la DLQ (no tiene sentido
 * reintentarlo); los errores del handler se propagan para que actúe la política de reintentos.
 */
public class RabbitEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventDispatcher.class);

    private final EventSerializer serializer;

    public RabbitEventDispatcher(EventSerializer serializer) {
        this.serializer = serializer;
    }

    public <T> void dispatch(Message message, EventHandler<T> handler) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        EventEnvelope<T> envelope;
        try {
            envelope = serializer.fromJson(body, handler.payloadType());
        } catch (IllegalArgumentException e) {
            log.error("Mensaje inválido descartado a la DLQ: {}", body, e);
            throw new AmqpRejectAndDontRequeueException("JSON inválido", e);
        }
        if (!handler.eventType().equals(envelope.eventType())) {
            log.warn("Evento {} ignorado: el handler espera {}", envelope.eventType(), handler.eventType());
            return;
        }
        log.debug("Procesando {} {}", envelope.eventType(), envelope.eventId());
        handler.handle(envelope);
    }
}
