package co.edu.unicauca.saberpro.questions;

import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.questions.outbox.OutboxEventRepository;
import co.edu.unicauca.saberpro.questions.outbox.OutboxRelay;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A22: integración real PostgreSQL + RabbitMQ. Recorre el ciclo de vida completo y verifica
 * que {@code PreguntaPublicada} sale por el outbox hasta una cola de RabbitMQ.
 */
@SpringBootTest(properties = "outbox.relay-interval-ms=600000")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "security.jwt.secret=test-secret-test-secret-test-secret-123456")
class QuestionServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired OutboxRelay relay;
    @Autowired OutboxEventRepository outbox;
    @Autowired AmqpAdmin amqpAdmin;
    @Autowired RabbitTemplate rabbitTemplate;

    private final UUID autorId = UUID.randomUUID();
    private final UUID revisorId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @Test
    void cicloDeVidaCompletoPublicaEvento() throws Exception {
        Queue probe = new Queue("it.question-published.probe", false, false, true);
        amqpAdmin.declareQueue(probe);
        amqpAdmin.declareBinding(BindingBuilder.bind(probe).to(new TopicExchange(EventTopology.EXCHANGE))
                .with(EventTopology.RK_QUESTION_PUBLISHED));

        String body = json.writeValueAsString(TestQuestions.valid());
        String created = mvc.perform(as(post("/api/questions"), autorId, "autor1", "AUTOR")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("BORRADOR"))
                .andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();

        // Edición en borrador con historial (RF-06, RF-29)
        String edited = body.replace("Facade", "Observer");
        mvc.perform(as(put("/api/questions/" + id), autorId, "autor1", "AUTOR")
                        .contentType(MediaType.APPLICATION_JSON).content(edited))
                .andExpect(status().isOk());
        mvc.perform(as(get("/api/questions/" + id + "/history"), autorId, "autor1", "AUTOR"))
                .andExpect(jsonPath("$[0].field").value("optionD"));

        mvc.perform(as(post("/api/questions/" + id + "/submit"), autorId, "autor1", "AUTOR")).andExpect(status().isOk());
        // Ya no es editable
        mvc.perform(as(put("/api/questions/" + id), autorId, "autor1", "AUTOR")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(as(post("/api/questions/" + id + "/start-review"), revisorId, "rev1", "REVISOR")).andExpect(status().isOk());
        mvc.perform(as(post("/api/questions/" + id + "/approve"), revisorId, "rev1", "REVISOR")).andExpect(status().isOk());
        // Un revisor no puede publicar
        mvc.perform(as(post("/api/questions/" + id + "/publish"), revisorId, "rev1", "REVISOR")).andExpect(status().isForbidden());
        mvc.perform(as(post("/api/questions/" + id + "/publish"), adminId, "admin", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLICADA"));

        assertEquals(1, outbox.countByPublishedAtIsNull());
        assertEquals(1, relay.relayPending());
        assertEquals(0, outbox.countByPublishedAtIsNull());

        Message msg = rabbitTemplate.receive(probe.getName(), 5000);
        assertNotNull(msg, "PreguntaPublicada debe llegar a RabbitMQ");
        JsonNode event = json.readTree(new String(msg.getBody(), StandardCharsets.UTF_8));
        assertEquals("PreguntaPublicada", event.get("eventType").asText());
        assertEquals(id, event.get("payload").get("questionId").asText());
        assertEquals("Observer", event.get("payload").get("options").get(3).get("text").asText());

        mvc.perform(as(get("/api/questions/" + id + "/state-audit"), adminId, "admin", "ADMINISTRADOR"))
                .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void validacionDevuelve422ConErrores() throws Exception {
        mvc.perform(as(post("/api/questions"), autorId, "autor1", "AUTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"options\":[\"a\",\"b\"]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors").isArray());
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, UUID id, String login, String role) {
        return req.with(jwt().jwt(j -> j.subject(id.toString()).claim("login", login).claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
