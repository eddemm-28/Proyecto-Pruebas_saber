package co.edu.unicauca.domain;

import co.edu.unicauca.access.IUserRepository;
import co.edu.unicauca.infra.Subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de dominio para la asignación de revisores (HU4).
 * Depende solo de abstracciones (DIP). Extiende Subject para que las vistas se refresquen.
 */
public class ReviewService extends Subject {

    private final QuestionRepository questionRepository;
    private final ReviewAssignmentRepository assignmentRepository;
    private final IUserRepository userRepository;
    private final INotificador notificador;

    public ReviewService(QuestionRepository questionRepository,
                         ReviewAssignmentRepository assignmentRepository,
                         IUserRepository userRepository,
                         INotificador notificador) {
        this.questionRepository = questionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.notificador = notificador;
    }

    /** Preguntas que están esperando revisor. */
    public List<Question> getPendingQuestions() {
        List<Question> pendientes = new ArrayList<>();
        for (Question q : questionRepository.findAll()) {
            if (Question.STATE_PENDIENTE_REVISION.equals(q.getEstado())) {
                pendientes.add(q);
            }
        }
        return pendientes;
    }

    /** Usuarios con rol Revisor que estén activos. */
    public List<User> getAvailableReviewers() {
        List<User> revisores = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            if (u.getRole() == Role.REVISOR && u.isActive()) {
                revisores.add(u);
            }
        }
        return revisores;
    }

    /** Logins de los revisores ya asignados a una pregunta. */
    public List<String> getAssignedReviewers(String questionId) {
        List<String> logins = new ArrayList<>();
        for (ReviewAssignment a : assignmentRepository.findByQuestion(questionId)) {
            logins.add(a.getReviewerLogin());
        }
        return logins;
    }

    /**
     * Asigna uno o más revisores a una pregunta pendiente y les envía un correo.
     * Si el correo de alguno falla, la asignación se mantiene.
     *
     * @return lista de mensajes de los revisores a los que no se pudo notificar (vacía si todo salió bien)
     */
    public List<String> assignReviewers(User admin, String questionId, List<String> reviewerLogins) {
        if (admin == null || admin.getRole() != Role.ADMINISTRADOR) {
            throw new IllegalStateException("Solo el administrador puede asignar revisores.");
        }

        Question question = questionRepository.findById(questionId);
        if (question == null) {
            throw new IllegalArgumentException("La pregunta " + questionId + " no existe.");
        }
        if (!Question.STATE_PENDIENTE_REVISION.equals(question.getEstado())) {
            throw new IllegalStateException("Solo se asignan revisores a preguntas en estado Pendiente de revisión.");
        }
        if (reviewerLogins == null || reviewerLogins.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un revisor.");
        }

        // Primero validamos a todos para no dejar la asignación a medias
        List<User> nuevos = new ArrayList<>();
        for (String login : reviewerLogins) {
            User revisor = userRepository.findByLogin(login);
            if (revisor == null || revisor.getRole() != Role.REVISOR) {
                throw new IllegalArgumentException("El usuario " + login + " no es un revisor.");
            }
            if (!revisor.isActive()) {
                throw new IllegalArgumentException("El revisor " + login + " está inactivo.");
            }
            if (login.equals(question.getAutor())) {
                throw new IllegalArgumentException("El autor de la pregunta no puede ser su propio revisor.");
            }
            if (!assignmentRepository.exists(questionId, login) && !nuevos.contains(revisor)) {
                nuevos.add(revisor);
            }
        }

        List<String> fallos = new ArrayList<>();
        for (User revisor : nuevos) {
            assignmentRepository.save(
                    new ReviewAssignment(questionId, revisor.getLogin(), admin.getLogin(), LocalDateTime.now()));
            try {
                notificador.notificarAsignacion(revisor, question);
            } catch (NotificacionException e) {
                fallos.add(revisor.getLogin() + ": " + e.getMessage());
            }
        }

        notifyAllObservers();
        return fallos;
    }
}
