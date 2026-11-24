package co.edu.unicauca.saberpro.simulations;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.payload.PreguntaPublicadaEvent;
import co.edu.unicauca.saberpro.simulations.application.port.out.QuestionReplicaRepository;
import co.edu.unicauca.saberpro.simulations.infrastructure.messaging.OutboxRelay;
import co.edu.unicauca.saberpro.simulations.infrastructure.persistence.repository.OutboxEventJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B17 (parte servicio): PreguntaPublicada real por RabbitMQ → réplica → simulacro por REST →
 * SimulacroFinalizado en el outbox.
 */
@SpringBootTest(properties = {"outbox.relay-interval-ms=600000", "simulation.expiration-check-ms=600000"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "security.jwt.secret=test-secret-test-secret-test-secret-123456")
class SimulationServiceIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EventPublisher publisher;
    @Autowired QuestionReplicaRepository replica;
    @Autowired OutboxEventJpaRepository outbox;
    @Autowired OutboxRelay relay;
    @Autowired AmqpAdmin amqpAdmin;

    private final UUID studentId = UUID.randomUUID();

    @Test
    void flujoCompletoDelSimulacro() throws Exception {
        // Cola de prueba que hace las veces del tracking-service (el publicador exige una cola destino)
        Queue probe = new Queue("it.simulation-finished.probe", false, false, true);
        amqpAdmin.declareQueue(probe);
        amqpAdmin.declareBinding(BindingBuilder.bind(probe).to(new TopicExchange(EventTopology.EXCHANGE))
                .with(EventTopology.RK_SIMULATION_FINISHED));

        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        for (UUID id : ids) {
            publisher.publish(EventTopology.RK_QUESTION_PUBLISHED, EventEnvelope.of(EventTypes.PREGUNTA_PUBLICADA,
                    "it", new PreguntaPublicadaEvent(id, "Q-IT-" + id.toString().substring(0, 4), 1, "MULTIPLE_CHOICE",
                            "Contexto", "¿Pregunta?",
                            List.of(new PreguntaPublicadaEvent.OptionPayload("A", "a"),
                                    new PreguntaPublicadaEvent.OptionPayload("B", "b"),
                                    new PreguntaPublicadaEvent.OptionPayload("C", "c"),
                                    new PreguntaPublicadaEvent.OptionPayload("D", "d")),
                            "C", "Porque C", "Lectura crítica", "Inferencia", "Sub", "MEDIA", Instant.now())));
        }
        await().atMost(Duration.ofSeconds(15)).until(() -> replica.findAllById(ids).size() == 3);

        mvc.perform(as(get("/api/simulations/catalog"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(3));

        String created = mvc.perform(as(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"COMPETENCE\",\"competence\":\"Lectura crítica\",\"questionCount\":3,\"timeLimitMinutes\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].correctOption").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode sim = json.readTree(created);
        String simId = sim.get("id").asText();

        // Segundo simulacro mientras el primero sigue activo → 409
        mvc.perform(as(post("/api/simulations")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategy\":\"MIXED\",\"questionCount\":1,\"timeLimitMinutes\":5}"))
                .andExpect(status().isConflict());

        for (int i = 0; i < 2; i++) {
            String qid = sim.get("items").get(i).get("questionId").asText();
            mvc.perform(as(put("/api/simulations/" + simId + "/answers/" + qid)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"selectedOption\":\"C\"}")).andExpect(status().isOk());
        }

        mvc.perform(as(post("/api/simulations/" + simId + "/finish")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.correctAnswers").value(2))
                .andExpect(jsonPath("$.globalScore").value(66.67))
                .andExpect(jsonPath("$.items[0].justification").value("Porque C"));

        assertEquals(1, outbox.countByPublishedAtIsNull());
        assertEquals(1, relay.relayPending());
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req) {
        return req.with(jwt().jwt(j -> j.subject(studentId.toString()).claim("login", "est-it").claim("role", "ESTUDIANTE"))
                .authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")));
    }
}
