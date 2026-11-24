package co.edu.unicauca.saberpro.events.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;

/**
 * Puerto genérico de publicación (A07). El dominio y la aplicación dependen de esta interfaz;
 * RabbitMQ es solo un adaptador ({@code RabbitEventPublisher}).
 */
public interface EventPublisher {

    /**
     * Publica el evento en el bus.
     *
     * @throws EventPublishingException si el broker no confirma la entrega
     */
    void publish(String routingKey, EventEnvelope<?> envelope);
}
