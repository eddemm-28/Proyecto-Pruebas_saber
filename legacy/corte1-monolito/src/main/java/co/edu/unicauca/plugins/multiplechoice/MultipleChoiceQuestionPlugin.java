package co.edu.unicauca.plugins.multiplechoice;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import co.edu.unicauca.pipeline.Filter;
import co.edu.unicauca.pipeline.Pipeline;
import co.edu.unicauca.plugin.api.QuestionPlugin;
import co.edu.unicauca.plugin.api.QuestionRequest;

import java.util.List;
import java.util.UUID;

public class MultipleChoiceQuestionPlugin implements QuestionPlugin {

    private final Pipeline<QuestionRequest> pipeline;

    public MultipleChoiceQuestionPlugin() {
        List<Filter<QuestionRequest>> filters = List.of(
                new ContentValidationFilter(),
                new OptionsValidationFilter(),
                new ClassificationFilter(),
                new CorrectAnswerValidationFilter()
        );
        this.pipeline = new Pipeline<>(filters);
    }

    @Override
    public String getName() {
        return "multiple-choice";
    }

    @Override
    public boolean supports(String type) {
        return "MULTIPLE_CHOICE".equalsIgnoreCase(type);
    }

    @Override
    public Question generate(QuestionRequest request) {
        System.out.println("Generando pregunta de selección múltiple...");
        QuestionRequest validated = pipeline.execute(request);

        List<String> opts = validated.getOptions();
        QuestionDistractors distractors = new QuestionDistractors(
                opts.get(0), opts.get(1), opts.get(2), opts.get(3));

        return new Question(
                UUID.randomUUID().toString().substring(0, 8),
                validated.getTitle(),
                validated.getContent(),
                distractors,
                validated.getCorrectAnswer(),
                Question.STATE_BORRADOR
        );
    }
}
