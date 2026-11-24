package co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_competence_stats")
@IdClass(StudentCompetenceStatEntity.Key.class)
public class StudentCompetenceStatEntity {

    @Id
    @Column(name = "student_id")
    public UUID studentId;

    @Id
    @Column(length = 100)
    public String competence;

    @Column(name = "student_login", length = 50)
    public String studentLogin;

    @Column(name = "simulations_count", nullable = false)
    public int simulationsCount;

    @Column(name = "total_questions", nullable = false)
    public int totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    public int correctAnswers;

    @Column(name = "average_score", nullable = false, precision = 5, scale = 2)
    public BigDecimal averageScore;

    @Column(name = "last_score", nullable = false, precision = 5, scale = 2)
    public BigDecimal lastScore;

    @Column(nullable = false, length = 15)
    public String classification;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public static class Key implements Serializable {
        public UUID studentId;
        public String competence;

        public Key() {
        }

        public Key(UUID studentId, String competence) {
            this.studentId = studentId;
            this.competence = competence;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(studentId, k.studentId) && Objects.equals(competence, k.competence);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studentId, competence);
        }
    }
}
