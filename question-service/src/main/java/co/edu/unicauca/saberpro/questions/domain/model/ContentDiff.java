package co.edu.unicauca.saberpro.questions.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** A17 / RF-29: diferencias campo a campo entre dos versiones del contenido. */
public final class ContentDiff {

    public record FieldChange(String field, String oldValue, String newValue) {
    }

    private ContentDiff() {
    }

    public static List<FieldChange> between(QuestionContent before, QuestionContent after) {
        List<FieldChange> changes = new ArrayList<>();
        compare(changes, "title", before, after, QuestionContent::title);
        compare(changes, "type", before, after, q -> q.type().name());
        compare(changes, "context", before, after, QuestionContent::context);
        compare(changes, "statement", before, after, QuestionContent::statement);
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            compare(changes, "option" + QuestionContent.LABELS.get(i), before, after,
                    q -> idx < q.options().size() ? q.options().get(idx) : null);
        }
        compare(changes, "correctOption", before, after, QuestionContent::correctOption);
        compare(changes, "justification", before, after, QuestionContent::justification);
        compare(changes, "bibliography", before, after, QuestionContent::bibliography);
        compare(changes, "competence", before, after, QuestionContent::competence);
        compare(changes, "topic", before, after, QuestionContent::topic);
        compare(changes, "subtopic", before, after, QuestionContent::subtopic);
        compare(changes, "difficulty", before, after, QuestionContent::difficulty);
        compare(changes, "mediaUrl", before, after, QuestionContent::mediaUrl);
        return changes;
    }

    private static void compare(List<FieldChange> out, String field, QuestionContent a, QuestionContent b,
                                Function<QuestionContent, String> getter) {
        String va = getter.apply(a);
        String vb = getter.apply(b);
        if (!Objects.equals(va, vb)) {
            out.add(new FieldChange(field, va, vb));
        }
    }
}
