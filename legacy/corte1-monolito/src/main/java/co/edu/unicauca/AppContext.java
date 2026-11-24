package co.edu.unicauca;

import co.edu.unicauca.access.IUserRepository;
import co.edu.unicauca.access.SqliteDatabase;
import co.edu.unicauca.access.SqliteQuestionRepository;
import co.edu.unicauca.access.SqliteReviewAssignmentRepository;
import co.edu.unicauca.access.SqliteUserRepository;
import co.edu.unicauca.domain.INotificador;
import co.edu.unicauca.domain.IPasswordHasher;
import co.edu.unicauca.domain.IPasswordValidator;
import co.edu.unicauca.domain.PasswordHasher;
import co.edu.unicauca.domain.PasswordValidator;
import co.edu.unicauca.domain.QuestionRepository;
import co.edu.unicauca.domain.Role;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.domain.UserStatus;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.domain.ReviewAssignmentRepository;
import co.edu.unicauca.domain.ReviewService;
import co.edu.unicauca.domain.UserService;
import co.edu.unicauca.infra.NotificadorFactory;

/**
 * Raíz de composición: aquí se crean los objetos concretos y se inyectan
 * en los servicios. El resto de la aplicación solo conoce abstracciones.
 */
public class AppContext {

    private final UserService userService;
    private final QuestionService questionService;
    private final ReviewService reviewService;

    public AppContext() {
        SqliteDatabase db = new SqliteDatabase();

        IUserRepository userRepository = new SqliteUserRepository(SqliteDatabase.DEFAULT_URL);
        QuestionRepository questionRepository = new SqliteQuestionRepository(db);
        ReviewAssignmentRepository assignmentRepository = new SqliteReviewAssignmentRepository(db);
        INotificador notificador = NotificadorFactory.crear();

        IPasswordHasher hasher = new PasswordHasher();
        IPasswordValidator validator = new PasswordValidator();

        this.userService = new UserService(userRepository, hasher, validator);
        this.questionService = new QuestionService(questionRepository);
        this.reviewService = new ReviewService(questionRepository, assignmentRepository, userRepository, notificador);

        if (userRepository.findAll().isEmpty()) {
            sembrarUsuariosDePrueba();
        }

        // Si cambia una pregunta (por ejemplo pasa a Pendiente), la vista del administrador se entera
        this.questionService.addObserver(obj -> reviewService.notifyAllObservers());
    }

    /** Crea los usuarios de prueba del README cuando la base de datos está vacía. */
    private void sembrarUsuariosDePrueba() {
        userService.register(new User("admin", "Administrador", Role.ADMINISTRADOR, UserStatus.ACTIVO, null,
                "admin@unicauca.edu.co"), "Admin123!");
        userService.register(new User("autor1", "Autor Uno", Role.AUTOR_DE_PREGUNTAS, UserStatus.ACTIVO, null,
                "autor1@unicauca.edu.co"), "Autor123!");
        userService.register(new User("revisor1", "Revisor Uno", Role.REVISOR, UserStatus.ACTIVO, null,
                "revisor1@unicauca.edu.co"), "Revis123!");
        userService.register(new User("docente1", "Docente Uno", Role.DOCENTE, UserStatus.ACTIVO, null,
                "docente1@unicauca.edu.co"), "Docen123!");
        userService.register(new User("estudiante1", "Estudiante Uno", Role.ESTUDIANTE, UserStatus.ACTIVO, null,
                "estudiante1@unicauca.edu.co"), "Estud123!");
    }

    public UserService getUserService() {
        return userService;
    }

    public QuestionService getQuestionService() {
        return questionService;
    }

    public ReviewService getReviewService() {
        return reviewService;
    }
}
