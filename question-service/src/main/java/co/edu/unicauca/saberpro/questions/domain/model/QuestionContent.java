package co.edu.unicauca.saberpro.questions.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Contenido editable de una pregunta (RF-05), independiente de la persistencia.
 * Es lo que validan los validadores estructurales y lo que generan los plugins.
 *
 * @param options       exactamente 4 textos, en orden A, B, C, D
 * @param correctOption letra A–D (formato estándar de respuesta correcta, A14)
 */
public record QuestionContent(
        String title,
        QuestionType type,
        String context,
        String statement,
        List<String> options,
        String correctOption,
        String justification,
        String bibliography,
        String competence,
        String topic,
        String subtopic,
        String difficulty,
        String mediaUrl) {

    public static final List<String> LABELS = List.of("A", "B", "C", "D");

    public QuestionContent {
        type = type == null ? QuestionType.MULTIPLE_CHOICE : type;
        options = options == null ? List.of() : options.stream().map(o -> o == null ? null : o.trim()).toList();
        correctOption = correctOption == null ? null : correctOption.trim().toUpperCase();
        title = trim(title);
        context = trim(context);
        statement = trim(statement);
        justification = trim(justification);
        bibliography = trim(bibliography);
        competence = trim(competence);
        topic = trim(topic);
        subtopic = trim(subtopic);
        difficulty = trim(difficulty);
        mediaUrl = trim(mediaUrl);
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public String optionText(String label) {
        int i = LABELS.indexOf(label);
        return i >= 0 && i < options.size() ? options.get(i) : null;
    }

    public boolean sameAs(QuestionContent other) {
        return Objects.equals(this, other);
    }
}
