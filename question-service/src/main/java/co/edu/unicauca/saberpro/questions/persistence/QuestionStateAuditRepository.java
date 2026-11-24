package co.edu.unicauca.saberpro.questions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionStateAuditRepository extends JpaRepository<QuestionStateAudit, Long> {

    List<QuestionStateAudit> findByQuestionIdOrderByChangedAtAscIdAsc(UUID questionId);
}
