package co.edu.unicauca.saberpro.questions.plugin.impl;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.plugin.api.CorrectAnswerNormalizer;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;
import co.edu.unicauca.saberpro.questions.plugin.api.QuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.pipeline.Pipeline;

import java.util.List;

/** Plugin de selección múltiple con única respuesta (tuberías y filtros). */
public class MultipleChoiceQuestionPlugin implements QuestionPlugin {

    private final Pipeline<PluginRequest> pipeline = new Pipeline<>(List.of(
            PluginFilters.NORMALIZE_WHITESPACE,
            PluginFilters.DEFAULT_CLASSIFICATION));

    @Override
    public String id() {
        return "multiple-choice";
    }

    @Override
    public String displayName() {
        return "Selección múltiple";
    }

    @Override
    public QuestionType type() {
        return QuestionType.MULTIPLE_CHOICE;
    }

    @Override
    public QuestionContent generate(PluginRequest request) {
        PluginRequest r = pipeline.execute(request);
        List<String> options = PluginFilters.fourOptions(r.options());
        return new QuestionContent(r.title(), type(), r.context(), r.statement(), options,
                CorrectAnswerNormalizer.toLabel(r.correctAnswer(), options),
                r.justification(), r.bibliography(), r.competence(), r.topic(), r.subtopic(), r.difficulty(), null);
    }
}
