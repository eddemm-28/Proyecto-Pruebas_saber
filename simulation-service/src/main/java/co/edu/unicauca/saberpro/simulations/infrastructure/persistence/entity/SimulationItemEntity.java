package co.edu.unicauca.saberpro.simulations.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_items")
public class SimulationItemEntity {

    @Id
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id")
    public SimulationEntity simulation;

    @Column(name = "question_id", nullable = false)
    public UUID questionId;

    @Column(nullable = false)
    public int position;

    @Column(nullable = false, length = 100)
    public String competence;

    @Column(nullable = false, length = 100)
    public String topic;

    @Column(nullable = false, length = 10)
    public String difficulty;

    @Column(name = "correct_option", nullable = false, length = 1)
    public String correctOption;

    @Column(name = "selected_option", length = 1)
    public String selectedOption;

    @Column(name = "is_correct")
    public Boolean correct;

    @Column(name = "answered_at")
    public Instant answeredAt;
}
