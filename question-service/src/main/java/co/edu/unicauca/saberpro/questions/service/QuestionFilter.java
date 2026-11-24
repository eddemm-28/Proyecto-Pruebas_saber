package co.edu.unicauca.saberpro.questions.service;

import co.edu.unicauca.saberpro.questions.domain.model.Difficulty;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import co.edu.unicauca.saberpro.questions.persistence.Question;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A16 / RF-07: filtros dinámicos combinables (JPA Criteria → SQL en PostgreSQL). */
public record QuestionFilter(
        QuestionStatus state,
        String competence,
        String topic,
        String subtopic,
        Difficulty difficulty,
        QuestionType type,
        UUID authorId,
        String text) {

    public static QuestionFilter empty() {
        return new QuestionFilter(null, null, null, null, null, null, null, null);
    }

    public QuestionFilter withAuthor(UUID author) {
        return new QuestionFilter(state, competence, topic, subtopic, difficulty, type, author, text);
    }

    public Specification<Question> toSpecification() {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (state != null) p.add(cb.equal(root.get("status"), state));
            if (notBlank(competence)) p.add(cb.equal(cb.lower(root.get("competence")), competence.trim().toLowerCase()));
            if (notBlank(topic)) p.add(cb.equal(cb.lower(root.get("topic")), topic.trim().toLowerCase()));
            if (notBlank(subtopic)) p.add(cb.equal(cb.lower(root.get("subtopic")), subtopic.trim().toLowerCase()));
            if (difficulty != null) p.add(cb.equal(root.get("difficulty"), difficulty));
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (authorId != null) p.add(cb.equal(root.get("authorId"), authorId));
            if (notBlank(text)) {
                String like = "%" + text.trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("statement")), like),
                        cb.like(cb.lower(root.get("code")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
