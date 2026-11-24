package co.edu.unicauca.saberpro.events.rabbit;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.messaging.EventPublishingException;
import co.edu.unicauca.saberpro.events.messaging.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Adaptador RabbitMQ de {@link EventPublisher}. Mensajes persistentes y espera del
 * <em>publisher confirm</em> para que el outbox solo marque como enviado lo que el broker aceptó.
 */
public class RabbitEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;
    private final EventSerializer serializer;
    private final String exchange;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, EventSerializer serializer) {
        this(rabbitTemplate, serializer, EventTopology.EXCHANGE);
    }

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, EventSerializer serializer, String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.serializer = serializer;
        this.exchange = exchange;
    }

    @Override
    public void publish(String routingKey, EventEnvelope<?> envelope) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setMessageId(envelope.eventId().toString());
        props.setTimestamp(Date.from(envelope.occurredAt()));
        props.setHeader(EventTopology.HEADER_EVENT_TYPE, envelope.eventType());
        if (envelope.correlationId() != null) {
            props.setCorrelationId(envelope.correlationId());
        }
        Message message = new Message(serializer.toJson(envelope).getBytes(StandardCharsets.UTF_8), props);

        CorrelationData correlation = new CorrelationData(envelope.eventId().toString());
        try {
            rabbitTemplate.send(exchange, routingKey, message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture().get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new EventPublishingException("El broker rechazó el evento " + envelope.eventId() + ": " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new EventPublishingException("Evento " + envelope.eventId() + " sin cola destino para " + routingKey);
            }
            log.debug("Evento {} {} publicado con routing key {}", envelope.eventType(), envelope.eventId(), routingKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException("Publicación interrumpida", e);
        } catch (EventPublishingException e) {
            throw e;
        } catch (Exception e) {
            throw new EventPublishingException("No se pudo publicar el evento " + envelope.eventId(), e);
        }
    }
}
