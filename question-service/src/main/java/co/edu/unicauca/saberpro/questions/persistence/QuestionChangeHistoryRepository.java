package co.edu.unicauca.saberpro.questions.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionChangeHistoryRepository extends JpaRepository<QuestionChangeHistory, Long> {

    List<QuestionChangeHistory> findByQuestionIdOrderByChangedAtDescIdDesc(UUID questionId);
}
