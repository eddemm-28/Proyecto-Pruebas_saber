package co.edu.unicauca.saberpro.tracking.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "simulation_results")
public class SimulationResultEntity {

    @Id
    @Column(name = "simulation_id")
    public UUID simulationId;

    @Column(name = "student_id", nullable = false)
    public UUID studentId;

    @Column(name = "student_login", length = 50)
    public String studentLogin;

    @Column(name = "final_status", nullable = false, length = 15)
    public String finalStatus;

    @Column(length = 20)
    public String strategy;

    @Column(name = "total_questions", nullable = false)
    public int totalQuestions;

    @Column(name = "correct_answers", nullable = false)
    public int correctAnswers;

    @Column(name = "global_score", nullable = false, precision = 5, scale = 2)
    public BigDecimal globalScore;

    @Column(name = "duration_seconds", nullable = false)
    public int durationSeconds;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    public Instant finishedAt;

    @Column(name = "received_at", nullable = false)
    public Instant receivedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "result_competence_scores", joinColumns = @JoinColumn(name = "simulation_id"))
    public List<CompetenceScore> competenceScores = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "result_answers", joinColumns = @JoinColumn(name = "simulation_id"))
    public List<Answer> answers = new ArrayList<>();

    @Embeddable
    public static class CompetenceScore {
        @Column(nullable = false, length = 100)
        public String competence;
        @Column(name = "total_questions", nullable = false)
        public int totalQuestions;
        @Column(name = "correct_answers", nullable = false)
        public int correctAnswers;
        @Column(nullable = false, precision = 5, scale = 2)
        public BigDecimal score;

        public CompetenceScore() {
        }

        public CompetenceScore(String competence, int totalQuestions, int correctAnswers, BigDecimal score) {
            this.competence = competence;
            this.totalQuestions = totalQuestions;
            this.correctAnswers = correctAnswers;
            this.score = score;
        }
    }

    @Embeddable
    public static class Answer {
        @Column(name = "question_id", nullable = false)
        public UUID questionId;
        @Column(nullable = false, length = 100)
        public String competence;
        @Column(length = 100)
        public String topic;
        @Column(length = 10)
        public String difficulty;
        @Column(nullable = false)
        public boolean answered;
        @Column(name = "is_correct", nullable = false)
        public boolean correct;

        public Answer() {
        }

        public Answer(UUID questionId, String competence, String topic, String difficulty, boolean answered, boolean correct) {
            this.questionId = questionId;
            this.competence = competence;
            this.topic = topic;
            this.difficulty = difficulty;
            this.answered = answered;
            this.correct = correct;
        }
    }
}
