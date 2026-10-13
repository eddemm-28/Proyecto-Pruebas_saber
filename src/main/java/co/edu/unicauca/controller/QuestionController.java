package co.edu.unicauca.controller;

import co.edu.unicauca.domain.PageResult;
import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionFilter;
import co.edu.unicauca.domain.QuestionService;
import co.edu.unicauca.domain.User;
import co.edu.unicauca.infra.Observer;

/**
 * Controlador (MVC) de las historias del autor de preguntas: crear, editar,
 * enviar a revisión y listar sus preguntas. Las vistas solo hablan con él.
 */
public class QuestionController {

    private final QuestionService service;
    private final User author;

    public QuestionController(QuestionService service, User author) {
        this.service = service;
        this.author = author;
    }

    public User getAuthor() {
        return author;
    }

    public Question crear(Question question) {
        question.setAutor(author.getLogin());
        return service.createQuestion(question);
    }

    public Question actualizar(Question question) {
        return service.updateQuestion(question, author.getLogin());
    }

    public void enviarARevision(String questionId) {
        service.sendToReview(questionId, author.getLogin());
    }

    public PageResult<Question> listarMisPreguntas(QuestionFilter filter, int page, int pageSize) {
        return service.searchByAuthor(author.getLogin(), filter, page, pageSize);
    }

    public Question obtener(String questionId) {
        return service.getQuestionById(questionId);
    }

    public void addObserver(Observer observer) {
        service.addObserver(observer);
    }

    public void removeObserver(Observer observer) {
        service.removeObserver(observer);
    }
}
