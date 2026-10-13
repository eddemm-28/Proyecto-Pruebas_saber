package co.edu.unicauca.pipeline;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.plugin.api.QuestionRequest;
import co.edu.unicauca.plugins.multiplechoice.MultipleChoiceQuestionPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MultipleChoicePipelineTest {

    @Test
    void generateValidQuestion() {
        MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();
        QuestionRequest req = new QuestionRequest(
                "Patrones",
                "¿Cuál patrón desacopla emisor y receptor?",
                "MULTIPLE_CHOICE",
                List.of("Observer", "Singleton", "Adapter", "Strategy"),
                "Observer",
                "Diseño",
                "Media"
        );
        Question q = plugin.generate(req);
        assertNotNull(q);
        assertNotNull(q.getId());
    }

    @Test
    void failsOnEmptyContent() {
        MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();
        QuestionRequest req = new QuestionRequest(
                "T", "", "MULTIPLE_CHOICE",
                List.of("A", "B", "C", "D"),
                "A", "Diseño", "Media");
        assertThrows(IllegalArgumentException.class, () -> plugin.generate(req));
    }

    @Test
    void failsOnDuplicatedOptions() {
        MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();
        QuestionRequest req = new QuestionRequest(
                "Título válido", "Enunciado con longitud suficiente para pasar", "MULTIPLE_CHOICE",
                List.of("Observer", "Observer", "Adapter", "Strategy"),
                "Observer", "Diseño", "Media");
        assertThrows(IllegalArgumentException.class, () -> plugin.generate(req));
    }

    @Test
    void failsWhenCorrectAnswerNotInOptions() {
        MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();
        QuestionRequest req = new QuestionRequest(
                "Título válido", "Enunciado con longitud suficiente", "MULTIPLE_CHOICE",
                List.of("A", "B", "C", "D"),
                "Z", "Diseño", "Media");
        assertThrows(IllegalArgumentException.class, () -> plugin.generate(req));
    }
}
