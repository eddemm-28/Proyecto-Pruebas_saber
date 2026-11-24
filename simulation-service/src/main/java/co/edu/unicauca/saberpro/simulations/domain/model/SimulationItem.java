package co.edu.unicauca.saberpro.simulations.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pregunta dentro de un simulacro. Guarda una "foto" de la clasificación y de la respuesta
 * correcta al generarse: si la pregunta se archiva después, el simulacro sigue siendo calificable.
 */
public final class SimulationItem {

    private final UUID id;
    private final UUID questionId;
    private final int position;
    private final String competence;
    private final String topic;
    private final String difficulty;
    private final String correctOption;
    private String selectedOption;
    private Boolean correct;
    private Instant answeredAt;

    public SimulationItem(UUID id, UUID questionId, int position, String competence, String topic, String difficulty,
                          String correctOption, String selectedOption, Boolean correct, Instant answeredAt) {
        this.id = Objects.requireNonNull(id);
        this.questionId = Objects.requireNonNull(questionId);
        this.position = position;
        this.competence = competence;
        this.topic = topic;
        this.difficulty = difficulty;
        this.correctOption = correctOption;
        this.selectedOption = selectedOption;
        this.correct = correct;
        this.answeredAt = answeredAt;
    }

    public static SimulationItem from(QuestionSnapshot q, int position) {
        return new SimulationItem(UUID.randomUUID(), q.questionId(), position, q.competence(), q.topic(),
                q.difficulty(), q.correctOption(), null, null, null);
    }

    void answer(String option, Instant at) {
        this.selectedOption = option;
        this.answeredAt = at;
    }

    void grade() {
        this.correct = selectedOption != null && selectedOption.equals(correctOption);
    }

    public boolean isAnswered() {
        return selectedOption != null;
    }

    public UUID getId() { return id; }
    public UUID getQuestionId() { return questionId; }
    public int getPosition() { return position; }
    public String getCompetence() { return competence; }
    public String getTopic() { return topic; }
    public String getDifficulty() { return difficulty; }
    public String getCorrectOption() { return correctOption; }
    public String getSelectedOption() { return selectedOption; }
    public Boolean getCorrect() { return correct; }
    public Instant getAnsweredAt() { return answeredAt; }
}
