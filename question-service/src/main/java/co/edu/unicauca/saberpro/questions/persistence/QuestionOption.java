package co.edu.unicauca.saberpro.questions.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Fila de {@code question_options}. */
@Embeddable
public class QuestionOption {

    @Column(name = "label", length = 1, nullable = false)
    private String label;

    @Column(name = "text", nullable = false, columnDefinition = "text")
    private String text;

    protected QuestionOption() {
    }

    public QuestionOption(String label, String text) {
        this.label = label;
        this.text = text;
    }

    public String getLabel() { return label; }
    public String getText() { return text; }

    @Override
    public boolean equals(Object o) {
        return o instanceof QuestionOption other && Objects.equals(label, other.label) && Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, text);
    }
}
