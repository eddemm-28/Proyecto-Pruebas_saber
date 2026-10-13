package co.edu.unicauca.domain;

import java.util.List;

/**
 * Interfaz para la abstracción de persistencia de preguntas.
 */
public interface QuestionRepository {
    List<Question> findAll();
    Question findById(String id);
    boolean updateState(String id, String newState);
    boolean save(Question question);
    boolean update(Question question);
    List<Question> findByAuthor(String authorLogin);
}
