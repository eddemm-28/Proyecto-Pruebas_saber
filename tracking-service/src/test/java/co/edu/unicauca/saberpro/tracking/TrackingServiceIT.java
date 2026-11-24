package co.edu.unicauca.saberpro.tracking;

import co.edu.unicauca.saberpro.events.EventEnvelope;
import co.edu.unicauca.saberpro.events.EventTopology;
import co.edu.unicauca.saberpro.events.EventTypes;
import co.edu.unicauca.saberpro.events.messaging.EventPublisher;
import co.edu.unicauca.saberpro.events.payload.SimulacroFinalizadoEvent;
import co.edu.unicauca.saberpro.tracking.application.port.out.SimulationResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B17 (parte servicio): SimulacroFinalizado por RabbitMQ → historial, estadísticas y reportes. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = "security.jwt.secret=test-secret-test-secret-test-secret-123456")
class TrackingServiceIT {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    @Container @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired MockMvc mvc;
    @Autowired EventPublisher publisher;
    @Autowired SimulationResultRepository results;

    private final UUID student = UUID.randomUUID();

    @Test
    void consumeEventoYExponeEstadisticas() throws Exception {
        SimulacroFinalizadoEvent payload = new SimulacroFinalizadoEvent(UUID.randomUUID(), student, "est-it", "FINISHED",
                "MIXED", Instant.now().minusSeconds(600), Instant.now(), 10, 8, 80.0,
                List.of(new SimulacroFinalizadoEvent.CompetenceScorePayload("Lectura crítica", 6, 6, 100.0),
                        new SimulacroFinalizadoEvent.CompetenceScorePayload("Inglés", 4, 2, 50.0)),
                List.of(new SimulacroFinalizadoEvent.AnswerPayload(UUID.randomUUID(), "Inglés", "Gramática", "MEDIA", "A", false)));
        EventEnvelope<SimulacroFinalizadoEvent> env = EventEnvelope.of(EventTypes.SIMULACRO_FINALIZADO, "it", payload);
        publisher.publish(EventTopology.RK_SIMULATION_FINISHED, env);
        publisher.publish(EventTopology.RK_SIMULATION_FINISHED, env); // duplicado: idempotente

        await().atMost(Duration.ofSeconds(15)).until(() -> results.countByStudent(student) == 1);

        mvc.perform(as(get("/api/tracking/students/me/history"), student, "ESTUDIANTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].globalScore").value(80.0));

        mvc.perform(as(get("/api/tracking/students/me/stats"), student, "ESTUDIANTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strengths[0]").value("Lectura crítica"))
                .andExpect(jsonPath("$.competences.length()").value(2));

        // Un estudiante no ve reportes; un docente sí
        mvc.perform(as(get("/api/tracking/reports/competences"), student, "ESTUDIANTE")).andExpect(status().isForbidden());
        mvc.perform(as(get("/api/tracking/reports/topics"), UUID.randomUUID(), "DOCENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].topic").value("Gramática"))
                .andExpect(jsonPath("$[0].accuracy").value(0.0));
    }

    private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder req, UUID id, String role) {
        return req.with(jwt().jwt(j -> j.subject(id.toString()).claim("login", "u").claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
