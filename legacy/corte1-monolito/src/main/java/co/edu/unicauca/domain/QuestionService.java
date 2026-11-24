package co.edu.unicauca.domain;

import co.edu.unicauca.infra.Subject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio del dominio para la gestión del banco de preguntas.
 * Extiende de Subject para notificar a los observadores cuando cambie el estado de alguna pregunta.
 */
public class QuestionService extends Subject {
    private final QuestionRepository repository;
    private final QuestionValidator validator = new QuestionValidator();

    public QuestionService(QuestionRepository repository) {
        if (repository == null) {
            throw new IllegalArgumentException("El repositorio no puede ser nulo.");
        }
        this.repository = repository;
    }

    public List<Question> getAllQuestions() {
        return repository.findAll();
    }

    public Question getQuestionById(String id) {
        return repository.findById(id);
    }

    /**
     * Actualiza el estado de una pregunta y notifica a todos los observadores registrados.
     */
    public boolean updateQuestionState(String id, String newState) {
        boolean updated = repository.updateState(id, newState);
        if (updated) {
            notifyAllObservers();
        }
        return updated;
    }

    /**
     * Guarda una nueva pregunta y notifica a los observadores.
     */
    public boolean saveQuestion(Question question) {
        boolean saved = repository.save(question);
        if (saved) {
            notifyAllObservers();
        }
        return saved;
    }

    /**
     * HU1: crea una pregunta nueva en estado Borrador aplicando la validación estructural.
     * El autor debe venir ya asignado en la pregunta.
     */
    public Question createQuestion(Question question) {
        if (question == null) {
            throw new IllegalArgumentException("La pregunta no puede ser nula.");
        }
        validator.validateOrThrow(question);

        question.setId(nextId());
        question.setEstado(Question.STATE_BORRADOR);
        if (!repository.save(question)) {
            throw new IllegalStateException("No se pudo guardar la pregunta.");
        }
        notifyAllObservers();
        return question;
    }

    /**
     * Edita una pregunta. Solo la puede editar su autor y mientras esté en borrador.
     */
    public Question updateQuestion(Question question, String authorLogin) {
        if (question == null || question.getId() == null) {
            throw new IllegalArgumentException("La pregunta no existe.");
        }
        Question existing = getOwnedQuestion(question.getId(), authorLogin);
        if (!Question.STATE_BORRADOR.equals(existing.getEstado())) {
            throw new IllegalStateException("Solo se pueden editar preguntas en estado Borrador.");
        }

        question.setAutor(existing.getAutor());
        question.setEstado(existing.getEstado());
        validator.validateOrThrow(question);

        if (!repository.update(question)) {
            throw new IllegalStateException("No se pudo actualizar la pregunta.");
        }
        notifyAllObservers();
        return question;
    }

    /**
     * HU2: el autor pasa su pregunta de Borrador a Pendiente de revisión.
     */
    public void sendToReview(String questionId, String authorLogin) {
        Question q = getOwnedQuestion(questionId, authorLogin);
        if (!Question.STATE_BORRADOR.equals(q.getEstado())) {
            throw new IllegalStateException("Solo se pueden enviar a revisión preguntas en estado Borrador.");
        }
        repository.updateState(questionId, Question.STATE_PENDIENTE_REVISION);
        notifyAllObservers();
    }

    /**
     * HU3: lista paginada de las preguntas creadas por un autor, con filtros.
     */
    public PageResult<Question> searchByAuthor(String authorLogin, QuestionFilter filter, int page, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("El tamaño de página debe ser mayor a cero.");
        }
        QuestionFilter f = (filter != null) ? filter : new QuestionFilter();

        List<Question> filtered = new ArrayList<>();
        for (Question q : repository.findByAuthor(authorLogin)) {
            if (f.matches(q)) {
                filtered.add(q);
            }
        }

        int total = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int currentPage = Math.min(Math.max(page, 1), totalPages);

        int from = (currentPage - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<Question> items = new ArrayList<>(filtered.subList(from, to));
        return new PageResult<>(items, currentPage, pageSize, total);
    }

    private Question getOwnedQuestion(String questionId, String authorLogin) {
        Question q = repository.findById(questionId);
        if (q == null) {
            throw new IllegalArgumentException("La pregunta " + questionId + " no existe.");
        }
        if (authorLogin == null || !authorLogin.equals(q.getAutor())) {
            throw new IllegalStateException("Solo el autor puede modificar esta pregunta.");
        }
        return q;
    }

    /** Genera el siguiente id con formato P-001, P-002, ... */
    private String nextId() {
        int max = 0;
        for (Question q : repository.findAll()) {
            String id = q.getId();
            if (id != null && id.matches("P-\\d+")) {
                max = Math.max(max, Integer.parseInt(id.substring(2)));
            }
        }
        return String.format("P-%03d", max + 1);
    }

    /**
     * Obtiene el conteo total de preguntas por cada estado.
     */
    public Map<String, Integer> getQuestionCountByState() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(Question.STATE_BORRADOR, 0);
        counts.put(Question.STATE_PENDIENTE_REVISION, 0);
        counts.put(Question.STATE_ELIMINADA, 0);

        List<Question> questions = repository.findAll();
        for (Question q : questions) {
            String state = q.getEstado();
            if (counts.containsKey(state)) {
                counts.put(state, counts.get(state) + 1);
            } else {
                counts.put(state, 1);
            }
        }
        return counts;
    }

    /**
     * Obtiene el porcentaje de preguntas por cada estado.
     */
    public Map<String, Double> getQuestionPercentageByState() {
        Map<String, Integer> counts = getQuestionCountByState();
        Map<String, Double> percentages = new LinkedHashMap<>();
        int total = repository.findAll().size();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double pct = (total == 0) ? 0.0 : (entry.getValue() * 100.0) / total;
            percentages.put(entry.getKey(), Math.round(pct * 10.0) / 10.0);
        }
        return percentages;
    }

    public int getTotalQuestionsCount() {
        return repository.findAll().size();
    }
}
