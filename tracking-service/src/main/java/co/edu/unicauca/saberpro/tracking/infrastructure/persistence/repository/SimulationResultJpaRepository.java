package co.edu.unicauca.saberpro.tracking.infrastructure.persistence.repository;

import co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity.SimulationResultEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SimulationResultJpaRepository extends JpaRepository<SimulationResultEntity, UUID> {

    Page<SimulationResultEntity> findByStudentIdOrderByFinishedAtDesc(UUID studentId, Pageable pageable);

    List<SimulationResultEntity> findByStudentIdOrderByFinishedAtAsc(UUID studentId);

    long countByStudentId(UUID studentId);

    /** [competence, students, simulations, totalQuestions, correctAnswers] */
    @Query("""
            SELECT c.competence, COUNT(DISTINCT r.studentId), COUNT(r), SUM(c.totalQuestions), SUM(c.correctAnswers)
            FROM SimulationResultEntity r JOIN r.competenceScores c
            WHERE r.finishedAt >= :from AND r.finishedAt < :to
            GROUP BY c.competence""")
    List<Object[]> competenceAggregates(@Param("from") Instant from, @Param("to") Instant to);

    /** [studentId, studentLogin, simulations, avgScore, bestScore, lastFinishedAt] */
    @Query("""
            SELECT r.studentId, MAX(r.studentLogin), COUNT(r), AVG(r.globalScore), MAX(r.globalScore), MAX(r.finishedAt)
            FROM SimulationResultEntity r
            WHERE r.finishedAt >= :from AND r.finishedAt < :to
            GROUP BY r.studentId""")
    List<Object[]> studentAggregates(@Param("from") Instant from, @Param("to") Instant to);

    /** [competence, topic, totalAnswers, correctAnswers] */
    @Query("""
            SELECT a.competence, a.topic, COUNT(a), SUM(CASE WHEN a.correct = true THEN 1 ELSE 0 END)
            FROM SimulationResultEntity r JOIN r.answers a
            WHERE r.finishedAt >= :from AND r.finishedAt < :to
            GROUP BY a.competence, a.topic""")
    List<Object[]> topicAggregates(@Param("from") Instant from, @Param("to") Instant to);
}
