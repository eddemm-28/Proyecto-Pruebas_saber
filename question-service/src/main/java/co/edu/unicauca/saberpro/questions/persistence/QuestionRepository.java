package co.edu.unicauca.saberpro.questions.persistence;

import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    @Query(value = "SELECT nextval('question_code_seq')", nativeQuery = true)
    long nextCodeNumber();

    @Query("SELECT q.status AS status, COUNT(q) AS total FROM Question q GROUP BY q.status")
    List<StatusCount> countByStatus();

    interface StatusCount {
        QuestionStatus getStatus();

        long getTotal();
    }
}
