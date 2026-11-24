package co.edu.unicauca.saberpro.events.payload;

import java.time.Instant;
import java.util.UUID;

/** Payload de {@code PreguntaArchivada} (v1). */
public record PreguntaArchivadaEvent(
        UUID questionId,
        long version,
        Instant archivedAt,
        String archivedBy) {
}
