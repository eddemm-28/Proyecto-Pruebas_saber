package co.edu.unicauca.saberpro.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * A22: reglas de acceso del gateway. Los servicios destino apuntan a un puerto cerrado,
 * así que una petición autorizada termina en error de conexión (5xx), no en 401/403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.jwt.secret=test-secret-test-secret-test-secret-123456",
        "USER_SERVICE_URL=http://localhost:1", "QUESTION_SERVICE_URL=http://localhost:1",
        "SIMULATION_SERVICE_URL=http://localhost:1", "TRACKING_SERVICE_URL=http://localhost:1"})
class GatewaySecurityTest {

    @Autowired
    WebTestClient client;

    @Test
    void sinTokenDa401() {
        client.get().uri("/api/questions").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void loginEsPublico() {
        client.post().uri("/api/auth/login").exchange().expectStatus().is5xxServerError();
    }

    @Test
    void estudianteNoAccedeAlBancoDePreguntas() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE")))
                .get().uri("/api/questions").exchange().expectStatus().isForbidden();
    }

    @Test
    void autorNoAccedeASimulacros() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_AUTOR")))
                .post().uri("/api/simulations").exchange().expectStatus().isForbidden();
    }

    @Test
    void docenteVeReportes() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_DOCENTE")))
                .get().uri("/api/tracking/reports/competences").exchange().expectStatus().is5xxServerError();
    }

    @Test
    void rutaDesconocidaSeNiega() {
        client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")))
                .get().uri("/api/otra-cosa").exchange().expectStatus().isForbidden();
    }
}
