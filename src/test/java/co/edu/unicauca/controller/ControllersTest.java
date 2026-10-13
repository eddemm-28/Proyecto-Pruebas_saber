package co.edu.unicauca.controller;

import co.edu.unicauca.access.IUserRepository;
import co.edu.unicauca.access.QuestionImplRepository;
import co.edu.unicauca.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba el flujo completo a través de los controladores: autor crea y envía, admin asigna.
 */
class ControllersTest {

    private QuestionController autorController;
    private ReviewController adminController;
    private final List<String> correos = new ArrayList<>();

    @BeforeEach
    void setUp() {
        QuestionRepository questions = new QuestionImplRepository();
        QuestionService questionService = new QuestionService(questions);

        User autor = new User("autor1", "Autora Uno", Role.AUTOR_DE_PREGUNTAS, UserStatus.ACTIVO, "h", "a@unicauca.edu.co");
        User admin = new User("admin", "Admin", Role.ADMINISTRADOR, UserStatus.ACTIVO, "h", "ad@unicauca.edu.co");
        User revisor = new User("rev1", "Revisor Uno", Role.REVISOR, UserStatus.ACTIVO, "h", "rev1@unicauca.edu.co");

        List<User> todos = List.of(autor, admin, revisor);
        IUserRepository users = new IUserRepository() {
            public boolean save(User u) { return true; }
            public User findByLogin(String login) {
                for (User u : todos) {
                    if (u.getLogin().equals(login)) return u;
                }
                return null;
            }
            public List<User> findAll() { return new ArrayList<>(todos); }
        };

        ReviewAssignmentRepository assignments = new ReviewAssignmentRepository() {
            private final List<ReviewAssignment> items = new ArrayList<>();
            public boolean save(ReviewAssignment a) { return items.add(a); }
            public boolean exists(String q, String r) {
                return items.stream().anyMatch(a -> a.getQuestionId().equals(q) && a.getReviewerLogin().equals(r));
            }
            public List<ReviewAssignment> findByQuestion(String q) {
                List<ReviewAssignment> r = new ArrayList<>();
                items.stream().filter(a -> a.getQuestionId().equals(q)).forEach(r::add);
                return r;
            }
            public List<ReviewAssignment> findByReviewer(String l) { return new ArrayList<>(); }
        };

        INotificador notificador = (rev, preg) -> correos.add(rev.getEmail() + " <- " + preg.getId());

        ReviewService reviewService = new ReviewService(questions, assignments, users, notificador);
        autorController = new QuestionController(questionService, autor);
        adminController = new ReviewController(reviewService, admin);
    }

    @Test
    @DisplayName("El controlador asigna el autor logueado a la pregunta que crea")
    void crearAsignaAutor() {
        Question q = autorController.crear(new Question(null, "Título", "Contexto de la pregunta", "¿Qué es un patrón?",
                new QuestionDistractors("Uno", "Dos", "Tres", "Cuatro"), "A", "Porque sí y con razón",
                "Libro GoF", "Diseño", "Patrones", "Observer", "Fácil", null, "otroAutor"));

        assertEquals("autor1", q.getAutor());
        assertEquals(1, autorController.listarMisPreguntas(null, 1, 10).getTotalItems());
    }

    @Test
    @DisplayName("Flujo completo: autor crea, envía a revisión y el admin asigna revisor")
    void flujoCompleto() {
        Question q = autorController.crear(new Question(null, "Título", "Contexto de la pregunta", "¿Qué es un patrón?",
                new QuestionDistractors("Uno", "Dos", "Tres", "Cuatro"), "A", "Porque sí y con razón",
                "Libro GoF", "Diseño", "Patrones", "Observer", "Fácil", null, null));

        assertFalse(adminController.listarPendientes().stream().anyMatch(p -> p.getId().equals(q.getId())));

        autorController.enviarARevision(q.getId());
        assertTrue(adminController.listarPendientes().stream().anyMatch(p -> p.getId().equals(q.getId())));

        List<String> fallos = adminController.asignar(q.getId(), List.of("rev1"));

        assertTrue(fallos.isEmpty());
        assertEquals(List.of("rev1"), adminController.revisoresAsignados(q.getId()));
        assertEquals(List.of("rev1@unicauca.edu.co <- " + q.getId()), correos);
        assertEquals(1, adminController.listarRevisores().size());
    }
}
