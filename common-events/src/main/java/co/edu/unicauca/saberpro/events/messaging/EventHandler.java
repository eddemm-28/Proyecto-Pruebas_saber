package co.edu.unicauca.saberpro.events.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;

/**
 * Puerto genérico de consumo (A07). Cada servicio implementa un handler por evento;
 * el adaptador de RabbitMQ deserializa el mensaje y se lo entrega.
 *
 * <p>Las implementaciones deben ser idempotentes: la entrega es <em>at-least-once</em>.</p>
 */
public interface EventHandler<T> {

    String eventType();

    Class<T> payloadType();

    void handle(EventEnvelope<T> envelope);
}
