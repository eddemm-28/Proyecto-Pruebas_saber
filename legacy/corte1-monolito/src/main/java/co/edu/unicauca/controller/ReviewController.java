package co.edu.unicauca.controller;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.ReviewService;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.infra.Observer;

import java.util.List;

/**
 * Controlador (MVC) de la asignación de revisores por parte del administrador.
 */
public class ReviewController {

    private final ReviewService service;
    private final User admin;

    public ReviewController(ReviewService service, User admin) {
        this.service = service;
        this.admin = admin;
    }

    public List<Question> listarPendientes() {
        return service.getPendingQuestions();
    }

    public List<User> listarRevisores() {
        return service.getAvailableReviewers();
    }

    public List<String> revisoresAsignados(String questionId) {
        return service.getAssignedReviewers(questionId);
    }

    /** @return mensajes de los revisores a los que no se pudo enviar el correo */
    public List<String> asignar(String questionId, List<String> reviewerLogins) {
        return service.assignReviewers(admin, questionId, reviewerLogins);
    }

    public void addObserver(Observer observer) {
        service.addObserver(observer);
    }

    public void removeObserver(Observer observer) {
        service.removeObserver(observer);
    }
}
