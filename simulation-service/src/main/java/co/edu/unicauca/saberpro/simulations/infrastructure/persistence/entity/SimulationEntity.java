package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "simulations")
public class SimulationEntity {

    @Id
    public UUID id;

    @Column(name = "student_id", nullable = false)
    public UUID studentId;

    @Column(name = "student_login", length = 50)
    public String studentLogin;

    @Column(nullable = false, length = 20)
    public String strategy;

    @Column(name = "filter_competence", length = 100)
    public String filterCompetence;

    @Column(name = "filter_topic", length = 100)
    public String filterTopic;

    @Column(name = "filter_difficulty", length = 10)
    public String filterDifficulty;

    @Column(name = "question_count", nullable = false)
    public int questionCount;

    @Column(name = "time_limit_minutes", nullable = false)
    public int timeLimitMinutes;

    @Column(nullable = false, length = 15)
    public String status;

    @Column(name = "started_at", nullable = false)
    public Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    public Instant expiresAt;

    @Column(name = "finished_at")
    public Instant finishedAt;

    @Column(name = "correct_count", nullable = false)
    public int correctCount;

    @Column(name = "global_score", nullable = false, precision = 5, scale = 2)
    public BigDecimal globalScore = BigDecimal.ZERO;

    // LAZY: el adaptador mapea dentro de la transacción; dos colecciones EAGER tipo "bag" no se pueden
    // traer a la vez en Hibernate (MultipleBagFetchException)
    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("position ASC")
    public List<SimulationItemEntity> items = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "competence_scores", joinColumns = @JoinColumn(name = "simulation_id"))
    public List<CompetenceScoreEmbeddable> competenceScores = new ArrayList<>();

    @Version
    public long version;

    @Embeddable
    public static class CompetenceScoreEmbeddable {
        @Column(nullable = false, length = 100)
        public String competence;

        @Column(name = "total_questions", nullable = false)
        public int totalQuestions;

        @Column(name = "correct_answers", nullable = false)
        public int correctAnswers;

        @Column(nullable = false, precision = 5, scale = 2)
        public BigDecimal score;

        public CompetenceScoreEmbeddable() {
        }

        public CompetenceScoreEmbeddable(String competence, int totalQuestions, int correctAnswers, BigDecimal score) {
            this.competence = competence;
            this.totalQuestions = totalQuestions;
            this.correctAnswers = correctAnswers;
            this.score = score;
        }
    }
}
