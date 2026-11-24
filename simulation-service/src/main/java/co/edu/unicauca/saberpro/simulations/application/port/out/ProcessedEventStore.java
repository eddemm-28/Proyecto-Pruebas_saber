package co.edu.unicauca.saberpro.simulations.application.port.out;

import java.util.UUID;

/** Puerto de salida para la idempotencia de consumidores. */
public interface ProcessedEventStore {

    /** @return {@code true} si el evento no se había procesado (y queda marcado) */
    boolean markProcessed(UUID eventId, String eventType);
}
