package co.edu.unicauca.access;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import co.edu.unicauca.domain.QuestionRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementación del repositorio de preguntas en memoria.
 */
public class QuestionImplRepository implements QuestionRepository {
    private final Map<String, Question> questionsMap = new LinkedHashMap<>();

    public QuestionImplRepository() {
        initSampleData();
    }

    private void initSampleData() {
        Question q1 = new Question(
                "P-001",
                "Pregunta sobre DDD",
                "¿Cuál es el objetivo principal de DDD?",
                new QuestionDistractors(
                        "Diseñar bases de datos",
                        "Modelar el dominio del negocio",
                        "Eliminar UML",
                        "Crear interfaces gráficas"
                ),
                "B",
                Question.STATE_BORRADOR
        );

        Question q2 = new Question(
                "P-002",
                "Arquitectura en capas",
                "¿Qué capa es responsable de abstraer el acceso a datos y persistencia?",
                new QuestionDistractors(
                        "Capa de Presentación",
                        "Capa de Dominio",
                        "Capa de Acceso a Datos",
                        "Capa Transversal"
                ),
                "C",
                Question.STATE_BORRADOR
        );

        Question q3 = new Question(
                "P-003",
                "Principios SOLID",
                "¿Qué establece el Principio de Inversión de Dependencias (DIP)?",
                new QuestionDistractors(
                        "Los módulos de alto nivel deben depender de abstracciones",
                        "Las clases deben ser abiertas para modificación",
                        "Usar solo una interfaz por aplicación",
                        "Evitar el patrón de diseño Observer"
                ),
                "A",
                Question.STATE_PENDIENTE_REVISION
        );

        Question q4 = new Question(
                "P-004",
                "Patrón Observer",
                "¿Cuál es el propósito fundamental del patrón de diseño Observer?",
                new QuestionDistractors(
                        "Instanciar objetos de manera dinámica",
                        "Definir una dependencia de uno a muchos entre objetos",
                        "Encapsular algoritmos en clases independientes",
                        "Convertir la interfaz de una clase en otra"
                ),
                "B",
                Question.STATE_PENDIENTE_REVISION
        );

        Question q5 = new Question(
                "P-005",
                "Patrones Creacionales",
                "¿Cuál de los siguientes patrones pertenece a la categoría Creacional?",
                new QuestionDistractors(
                        "Adapter",
                        "Singleton",
                        "Observer",
                        "Strategy"
                ),
                "B",
                Question.STATE_ELIMINADA
        );

        save(q1);
        save(q2);
        save(q3);
        save(q4);
        save(q5);
    }

    @Override
    public List<Question> findAll() {
        return new ArrayList<>(questionsMap.values());
    }

    @Override
    public Question findById(String id) {
        return questionsMap.get(id);
    }

    @Override
    public boolean updateState(String id, String newState) {
        Question question = questionsMap.get(id);
        if (question != null) {
            question.setEstado(newState);
            return true;
        }
        return false;
    }

    @Override
    public boolean save(Question question) {
        if (question != null && question.getId() != null) {
            questionsMap.put(question.getId(), question);
            return true;
        }
        return false;
    }

    @Override
    public boolean update(Question question) {
        if (question != null && question.getId() != null && questionsMap.containsKey(question.getId())) {
            questionsMap.put(question.getId(), question);
            return true;
        }
        return false;
    }

    @Override
    public List<Question> findByAuthor(String authorLogin) {
        List<Question> result = new ArrayList<>();
        for (Question q : questionsMap.values()) {
            if (authorLogin != null && authorLogin.equals(q.getAutor())) {
                result.add(q);
            }
        }
        return result;
    }
}
