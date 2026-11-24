package co.edu.unicauca.saberpro.questions.service;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.payload.PreguntaArchivadaEvent;
import co.edu.unicauca.saberpro.events.payload.PreguntaPublicadaEvent;
import co.edu.unicauca.saberpro.questions.persistence.Question;

import java.util.UUID;

/** A18: construcción de los eventos de dominio a partir del agregado. */
final class QuestionEvents {

    static final String PRODUCER = "question-service";

    private QuestionEvents() {
    }

    static EventEnvelope<PreguntaPublicadaEvent> published(Question q, String correlationId) {
        PreguntaPublicadaEvent payload = new PreguntaPublicadaEvent(
                q.getId(), q.getCode(), q.getVersion(), q.getType().name(), q.getContext(), q.getStatement(),
                q.getOptions().stream().map(o -> new PreguntaPublicadaEvent.OptionPayload(o.getLabel(), o.getText())).toList(),
                q.getCorrectOption(), q.getJustification(), q.getCompetence(), q.getTopic(), q.getSubtopic(),
                q.getDifficulty().name(), q.getPublishedAt());
        return EventEnvelope.of(UUID.randomUUID(), EventTypes.PREGUNTA_PUBLICADA, PRODUCER, correlationId, payload);
    }

    static EventEnvelope<PreguntaArchivadaEvent> archived(Question q, String archivedBy, String correlationId) {
        PreguntaArchivadaEvent payload = new PreguntaArchivadaEvent(q.getId(), q.getVersion(), q.getArchivedAt(), archivedBy);
        return EventEnvelope.of(UUID.randomUUID(), EventTypes.PREGUNTA_ARCHIVADA, PRODUCER, correlationId, payload);
    }
}
