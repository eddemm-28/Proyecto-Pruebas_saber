package co.edu.unicauca.domain;

import co.edu.unicauca.access.QuestionImplRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de HU4: asignación de revisores y notificación por correo.
 */
class ReviewServiceTest {

    private QuestionService questionService;
    private ReviewService reviewService;
    private FakeAssignmentRepository assignments;
    private FakeNotificador notificador;

    private User admin;
    private String preguntaPendiente;

    @BeforeEach
    void setUp() {
        QuestionRepository questionRepository = new QuestionImplRepository();
        questionService = new QuestionService(questionRepository);

        FakeUserRepository users = new FakeUserRepository();
        admin = TestData.usuario("admin", Role.ADMINISTRADOR, UserStatus.ACTIVO, "admin@unicauca.edu.co");
        users.save(admin);
        users.save(TestData.usuario("rev1", Role.REVISOR, UserStatus.ACTIVO, "rev1@unicauca.edu.co"));
        users.save(TestData.usuario("rev2", Role.REVISOR, UserStatus.ACTIVO, "rev2@unicauca.edu.co"));
        users.save(TestData.usuario("revInactivo", Role.REVISOR, UserStatus.INACTIVO, "inactivo@unicauca.edu.co"));
        users.save(TestData.usuario("revSinCorreo", Role.REVISOR, UserStatus.ACTIVO, null));
        users.save(TestData.usuario("autor1", Role.AUTOR_DE_PREGUNTAS, UserStatus.ACTIVO, "autor1@unicauca.edu.co"));

        assignments = new FakeAssignmentRepository();
        notificador = new FakeNotificador();
        reviewService = new ReviewService(questionRepository, assignments, users, notificador);

        Question q = questionService.createQuestion(TestData.preguntaValida("autor1"));
        questionService.sendToReview(q.getId(), "autor1");
        preguntaPendiente = q.getId();
    }

    @Test
    @DisplayName("Lista como pendientes solo las preguntas Pendiente de revisión")
    void listaPendientes() {
        List<Question> pendientes = reviewService.getPendingQuestions();
        for (Question q : pendientes) {
            assertEquals(Question.STATE_PENDIENTE_REVISION, q.getEstado());
        }
        // P-003 y P-004 del repositorio de ejemplo + la que se creó en setUp
        assertEquals(3, pendientes.size());
    }

    @Test
    @DisplayName("Los revisores disponibles son solo los activos con rol Revisor")
    void revisoresDisponibles() {
        List<User> revisores = reviewService.getAvailableReviewers();
        assertEquals(3, revisores.size());   // rev1, rev2 y revSinCorreo
        for (User u : revisores) {
            assertEquals(Role.REVISOR, u.getRole());
            assertTrue(u.isActive());
        }
    }

    @Test
    @DisplayName("Asigna un revisor y le envía el correo")
    void asignaYNotifica() {
        List<String> fallos = reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1"));

        assertTrue(fallos.isEmpty());
        assertTrue(assignments.exists(preguntaPendiente, "rev1"));
        assertEquals(List.of("rev1@unicauca.edu.co"), notificador.enviadosA);
        assertEquals(List.of(preguntaPendiente), notificador.preguntas);
    }

    @Test
    @DisplayName("Puede asignar varios revisores y notifica a cada uno")
    void asignaVarios() {
        reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1", "rev2"));

        assertEquals(List.of("rev1", "rev2"), reviewService.getAssignedReviewers(preguntaPendiente));
        assertEquals(2, notificador.enviadosA.size());
    }

    @Test
    @DisplayName("Si se vuelve a asignar el mismo revisor no se duplica ni se reenvía el correo")
    void noDuplica() {
        reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1"));
        reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1", "rev2"));

        assertEquals(2, reviewService.getAssignedReviewers(preguntaPendiente).size());
        assertEquals(2, notificador.enviadosA.size());
    }

    @Test
    @DisplayName("Debe seleccionarse al menos un revisor")
    void sinRevisores() {
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, preguntaPendiente, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, preguntaPendiente, null));
    }

    @Test
    @DisplayName("Solo el administrador puede asignar")
    void soloAdmin() {
        User autor = TestData.usuario("autor1", Role.AUTOR_DE_PREGUNTAS, UserStatus.ACTIVO, null);
        assertThrows(IllegalStateException.class,
                () -> reviewService.assignReviewers(autor, preguntaPendiente, List.of("rev1")));
        assertThrows(IllegalStateException.class,
                () -> reviewService.assignReviewers(null, preguntaPendiente, List.of("rev1")));
    }

    @Test
    @DisplayName("Solo se asignan preguntas en estado Pendiente de revisión")
    void soloPendientes() {
        // P-001 está en Borrador en los datos de ejemplo
        assertThrows(IllegalStateException.class,
                () -> reviewService.assignReviewers(admin, "P-001", List.of("rev1")));
    }

    @Test
    @DisplayName("No se asigna una pregunta que no existe")
    void preguntaInexistente() {
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, "P-999", List.of("rev1")));
    }

    @Test
    @DisplayName("Rechaza usuarios que no son revisores o están inactivos, sin dejar asignaciones a medias")
    void revisoresInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1", "autor1")));
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, preguntaPendiente, List.of("revInactivo")));
        assertThrows(IllegalArgumentException.class,
                () -> reviewService.assignReviewers(admin, preguntaPendiente, List.of("noExiste")));

        assertTrue(reviewService.getAssignedReviewers(preguntaPendiente).isEmpty());
        assertTrue(notificador.enviadosA.isEmpty());
    }

    @Test
    @DisplayName("Si falla el correo de un revisor la asignación se mantiene y se informa el fallo")
    void fallaElCorreo() {
        notificador.loginQueFalla = "rev2";

        List<String> fallos = reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1", "rev2"));

        assertEquals(1, fallos.size());
        assertTrue(fallos.get(0).startsWith("rev2"));
        assertEquals(2, reviewService.getAssignedReviewers(preguntaPendiente).size());
        assertEquals(List.of("rev1@unicauca.edu.co"), notificador.enviadosA);
    }

    @Test
    @DisplayName("Un revisor sin correo registrado queda asignado pero se informa que no se pudo notificar")
    void sinCorreo() {
        // El notificador de verdad lanza NotificacionException cuando no hay correo
        INotificador estricto = new co.edu.unicauca.infra.ConsoleNotificador();
        FakeUserRepository users = new FakeUserRepository();
        users.save(admin);
        users.save(TestData.usuario("revSinCorreo", Role.REVISOR, UserStatus.ACTIVO, null));
        ReviewService servicio = new ReviewService(new QuestionImplRepository(), assignments, users, estricto);

        List<String> fallos = servicio.assignReviewers(admin, "P-003", List.of("revSinCorreo"));

        assertEquals(1, fallos.size());
        assertTrue(assignments.exists("P-003", "revSinCorreo"));
    }

    @Test
    @DisplayName("El autor de la pregunta no puede ser su propio revisor")
    void autorNoEsRevisor() {
        // Se crea una pregunta cuyo autor también tiene rol Revisor
        FakeUserRepository users = new FakeUserRepository();
        users.save(admin);
        users.save(TestData.usuario("autorRevisor", Role.REVISOR, UserStatus.ACTIVO, "x@unicauca.edu.co"));

        QuestionRepository repo = new QuestionImplRepository();
        QuestionService qs = new QuestionService(repo);
        Question q = qs.createQuestion(TestData.preguntaValida("autorRevisor"));
        qs.sendToReview(q.getId(), "autorRevisor");

        ReviewService servicio = new ReviewService(repo, new FakeAssignmentRepository(), users, notificador);
        assertThrows(IllegalArgumentException.class,
                () -> servicio.assignReviewers(admin, q.getId(), List.of("autorRevisor")));
    }

    @Test
    @DisplayName("Asignar revisores notifica a los observadores de la vista")
    void notificaObservadores() {
        int[] llamadas = {0};
        reviewService.addObserver(obj -> llamadas[0]++);
        reviewService.assignReviewers(admin, preguntaPendiente, List.of("rev1"));
        assertEquals(1, llamadas[0]);
    }
}
