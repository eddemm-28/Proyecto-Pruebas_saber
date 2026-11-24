package co.edu.unicauca.saberpro.client;

import co.edu.unicauca.saberpro.client.controller.QuestionController;
import co.edu.unicauca.saberpro.client.controller.SimulationController;
import co.edu.unicauca.saberpro.client.controller.SimulationTimer;
import co.edu.unicauca.saberpro.client.infra.ApiException;
import co.edu.unicauca.saberpro.client.model.Dtos;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A20/A22: el cliente habla con un gateway simulado (HttpServer del JDK), sin Docker. */
class ClientApiTest {

    /** Servidor HTTP mínimo con respuestas fijas por ruta. */
    static final class FakeGateway implements AutoCloseable {
        final HttpServer server;
        final List<String> requests = new ArrayList<>();

        FakeGateway() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.start();
        }

        FakeGateway on(String path, int status, String body) {
            server.createContext(path, ex -> respond(ex, status, body));
            return this;
        }

        private void respond(HttpExchange ex, int status, String body) throws IOException {
            requests.add(ex.getRequestMethod() + " " + ex.getRequestURI() + " auth=" + ex.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
            ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) ex.getResponseBody().write(bytes);
            ex.close();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    @Test
    void loginGuardaElTokenYLoEnviaEnLasSiguientesPeticiones() throws Exception {
        String user = "{\"id\":\"" + UUID.randomUUID() + "\",\"login\":\"autor1\",\"fullName\":\"Autor\",\"role\":\"AUTOR\"}";
        try (FakeGateway gw = new FakeGateway()
                .on("/api/auth/login", 200, "{\"accessToken\":\"jwt-123\",\"tokenType\":\"Bearer\",\"expiresIn\":3600,\"user\":" + user + "}")
                .on("/api/questions", 200, "{\"content\":[],\"page\":0,\"size\":15,\"totalElements\":0,\"totalPages\":0}")) {
            AppContext ctx = new AppContext(gw.url());
            Dtos.User u = ctx.auth().login("autor1", "Clave1*");
            assertEquals("AUTOR", u.role());
            assertEquals("jwt-123", ctx.session().token());

            ctx.questions().search(QuestionController.Filter.none(true), 0, 15);
            String last = gw.requests.get(gw.requests.size() - 1);
            assertTrue(last.contains("scope=mine"), last);
            assertTrue(last.endsWith("auth=Bearer jwt-123"), last);
        }
    }

    @Test
    void erroresRfc7807SeTraducenConSuLista() throws Exception {
        try (FakeGateway gw = new FakeGateway().on("/api/questions", 422,
                "{\"title\":\"Unprocessable Entity\",\"status\":422,\"detail\":\"La pregunta no cumple las reglas\","
                        + "\"errors\":[\"RF-08: el contexto es obligatorio.\",\"RF-12: la opción D...\"]}")) {
            AppContext ctx = new AppContext(gw.url());
            ApiException ex = assertThrows(ApiException.class, () -> ctx.questions().create(new Dtos.QuestionRequest(
                    "t", "MULTIPLE_CHOICE", "", "", List.of(), "A", "", "", "", "", "", "MEDIA", null, null)));
            assertEquals(422, ex.getStatus());
            assertEquals(2, ex.getErrors().size());
            assertTrue(ex.userMessage().contains("RF-08"));
        }
    }

    @Test
    void sinServidorDaErrorDeRedLegible() {
        AppContext ctx = new AppContext("http://127.0.0.1:1");
        ApiException ex = assertThrows(ApiException.class, () -> ctx.auth().login("a", "b"));
        assertTrue(ex.isNetworkError());
    }

    @Test
    void respuestaFueraDeTiempoNotificaExpiracion() throws Exception {
        UUID simId = UUID.randomUUID();
        UUID qId = UUID.randomUUID();
        String sim = "{\"id\":\"" + simId + "\",\"status\":\"IN_PROGRESS\",\"strategy\":\"MIXED\","
                + "\"startedAt\":\"2026-11-25T15:00:00Z\",\"expiresAt\":\"2026-11-25T15:10:00Z\","
                + "\"serverTime\":\"2026-11-25T15:00:00Z\",\"remainingSeconds\":600,\"questionCount\":1,\"answeredCount\":0,"
                + "\"items\":[{\"position\":1,\"questionId\":\"" + qId + "\",\"options\":[]}]}";
        try (FakeGateway gw = new FakeGateway()
                .on("/api/simulations", 201, sim)
                .on("/api/simulations/" + simId + "/answers/", 409,
                        "{\"accepted\":false,\"status\":\"EXPIRED\",\"remainingSeconds\":0,\"message\":\"Se acabó el tiempo\"}")) {
            AppContext ctx = new AppContext(gw.url());
            SimulationController controller = ctx.simulations();
            List<String> events = new ArrayList<>();
            controller.addObserver((src, event, data) -> events.add(event));

            controller.start(new Dtos.StartSimulation("MIXED", null, null, null, 1, 10));
            assertFalse(controller.answer(qId, "A"));
            Thread.sleep(300); // las notificaciones se entregan en el EDT
            javax.swing.SwingUtilities.invokeAndWait(() -> { });
            assertEquals(List.of(SimulationController.STARTED, SimulationController.EXPIRED), events);
        }
    }

    @Test
    void cronometroUsaElRelojDelServidor() {
        // El reloj local va 2 minutos atrasado respecto al servidor
        Clock local = Clock.fixed(Instant.parse("2026-11-25T14:58:00Z"), ZoneOffset.UTC);
        SimulationTimer timer = new SimulationTimer(local);
        timer.sync(Instant.parse("2026-11-25T15:00:00Z"), Instant.parse("2026-11-25T15:10:00Z"));
        assertEquals(600, timer.remainingSeconds());
        assertEquals("10:00", SimulationTimer.format(600));
        assertEquals("1:01:05", SimulationTimer.format(3665));
        timer.sync(Instant.parse("2026-11-25T15:20:00Z"), Instant.parse("2026-11-25T15:10:00Z"));
        assertTrue(timer.isOver());
    }
}
