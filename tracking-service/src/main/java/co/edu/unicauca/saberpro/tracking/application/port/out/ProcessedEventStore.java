package co.edu.unicauca.saberpro.tracking.application.port.out;

import java.util.UUID;

public interface ProcessedEventStore {

    /** @return {@code true} si el evento es nuevo (y queda marcado como procesado) */
    boolean markProcessed(UUID eventId, String eventType);
}
