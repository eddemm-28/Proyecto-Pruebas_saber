package co.edu.unicauca.saberpro.events.messaging;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Serialización JSON de {@link EventEnvelope} independiente del broker. */
public class EventSerializer {

    private final ObjectMapper mapper;

    public EventSerializer() {
        this(defaultMapper());
    }

    public EventSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public static ObjectMapper defaultMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public String toJson(EventEnvelope<?> envelope) {
        try {
            return mapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar el evento " + envelope.eventType(), e);
        }
    }

    public String payloadToJson(Object payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se pudo serializar el payload", e);
        }
    }

    public <T> EventEnvelope<T> fromJson(String json, Class<T> payloadType) {
        JavaType type = mapper.getTypeFactory().constructParametricType(EventEnvelope.class, payloadType);
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mensaje JSON inválido para " + payloadType.getSimpleName(), e);
        }
    }

    public <T> T payloadFromJson(String json, Class<T> payloadType) {
        try {
            return mapper.readValue(json, payloadType);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload JSON inválido para " + payloadType.getSimpleName(), e);
        }
    }
}
