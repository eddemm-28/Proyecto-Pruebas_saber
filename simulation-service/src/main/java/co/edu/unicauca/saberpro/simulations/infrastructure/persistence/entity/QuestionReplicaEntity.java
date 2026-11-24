package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Entidad JPA de la réplica (solo infraestructura; el dominio usa QuestionSnapshot). */
@Entity
@Table(name = "question_replica")
public class QuestionReplicaEntity {

    @Id
    @Column(name = "question_id")
    public UUID questionId;

    @Column(length = 20)
    public String code;

    @Column(nullable = false, columnDefinition = "text")
    public String context;

    @Column(nullable = false, columnDefinition = "text")
    public String statement;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_replica_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("label ASC")
    public List<OptionEmbeddable> options = new ArrayList<>();

    @Column(name = "correct_option", nullable = false, length = 1)
    public String correctOption;

    @Column(columnDefinition = "text")
    public String justification;

    @Column(nullable = false, length = 100)
    public String competence;

    @Column(nullable = false, length = 100)
    public String topic;

    @Column(length = 100)
    public String subtopic;

    @Column(nullable = false, length = 10)
    public String difficulty;

    @Column(nullable = false)
    public boolean available;

    @Column(name = "source_version", nullable = false)
    public long sourceVersion;

    @Column(name = "published_at")
    public Instant publishedAt;

    @Column(name = "synced_at", nullable = false)
    public Instant syncedAt;

    @Embeddable
    public static class OptionEmbeddable {
        @Column(name = "label", length = 1, nullable = false)
        public String label;

        @Column(name = "text", nullable = false, columnDefinition = "text")
        public String text;

        public OptionEmbeddable() {
        }

        public OptionEmbeddable(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }
}
