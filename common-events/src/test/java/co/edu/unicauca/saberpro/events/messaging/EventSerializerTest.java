package co.edu.unicauca.saberpro.events.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.payload.PreguntaPublicadaEvent;
import co.edu.unicauca.saberpro.events.payload.SimulacroFinalizadoEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSerializerTest {

    private final EventSerializer serializer = new EventSerializer();

    @Test
    void roundTripPreguntaPublicada() {
        UUID id = UUID.randomUUID();
        PreguntaPublicadaEvent payload = new PreguntaPublicadaEvent(id, "Q-2026-000001", 3, "MULTIPLE_CHOICE",
                "Contexto de prueba", "¿Enunciado?",
                List.of(new PreguntaPublicadaEvent.OptionPayload("A", "uno"),
                        new PreguntaPublicadaEvent.OptionPayload("B", "dos"),
                        new PreguntaPublicadaEvent.OptionPayload("C", "tres"),
                        new PreguntaPublicadaEvent.OptionPayload("D", "cuatro")),
                "B", "Porque sí", "Diseño de software", "Patrones", "State", "MEDIA",
                Instant.parse("2026-11-23T22:15:03Z"));
        EventEnvelope<PreguntaPublicadaEvent> envelope =
                EventEnvelope.of(EventTypes.PREGUNTA_PUBLICADA, "question-service", payload);

        String json = serializer.toJson(envelope);
        assertTrue(json.contains("\"eventType\":\"PreguntaPublicada\""));
        assertTrue(json.contains("2026-11-23T22:15:03Z"), "Las fechas viajan en ISO-8601");

        EventEnvelope<PreguntaPublicadaEvent> back = serializer.fromJson(json, PreguntaPublicadaEvent.class);
        assertEquals(envelope.eventId(), back.eventId());
        assertEquals(payload, back.payload());
    }

    @Test
    void roundTripSimulacroFinalizado() {
        SimulacroFinalizadoEvent payload = new SimulacroFinalizadoEvent(UUID.randomUUID(), UUID.randomUUID(), "est1",
                "FINISHED", "COMPETENCE", Instant.now(), Instant.now(), 2, 1, 50.0,
                List.of(new SimulacroFinalizadoEvent.CompetenceScorePayload("Lectura crítica", 2, 1, 50.0)),
                List.of(new SimulacroFinalizadoEvent.AnswerPayload(UUID.randomUUID(), "Lectura crítica", "T", "FACIL", "A", true),
                        new SimulacroFinalizadoEvent.AnswerPayload(UUID.randomUUID(), "Lectura crítica", "T", "FACIL", null, false)));
        EventEnvelope<SimulacroFinalizadoEvent> env = EventEnvelope.of(EventTypes.SIMULACRO_FINALIZADO, "simulation-service", payload);

        EventEnvelope<SimulacroFinalizadoEvent> back = serializer.fromJson(serializer.toJson(env), SimulacroFinalizadoEvent.class);
        assertEquals(payload, back.payload());
    }

    @Test
    void jsonInvalidoLanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> serializer.fromJson("{no-json", PreguntaPublicadaEvent.class));
    }

    @Test
    void nombreDeDeadLetterQueue() {
        assertEquals("tracking.simulation-finished.dlq",
                EventTopology.deadLetterQueueOf(EventTopology.Q_TRACKING_SIMULATION_FINISHED));
    }
}
