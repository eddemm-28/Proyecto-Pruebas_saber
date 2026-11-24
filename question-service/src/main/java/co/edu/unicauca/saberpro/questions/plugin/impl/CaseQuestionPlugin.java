package co.edu.unicauca.saberpro.questions.plugin.impl;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.plugin.api.CorrectAnswerNormalizer;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;
import co.edu.unicauca.saberpro.questions.plugin.api.QuestionPlugin;
import co.edu.unicauca.saberpro.questions.plugin.pipeline.Pipeline;

import java.util.List;

/** Plugin de pregunta basada en caso de estudio: el contexto se rotula como caso. */
public class CaseQuestionPlugin implements QuestionPlugin {

    static final String CASE_PREFIX = "Caso de estudio: ";

    private final Pipeline<PluginRequest> pipeline = new Pipeline<>(List.of(
            PluginFilters.NORMALIZE_WHITESPACE,
            PluginFilters.DEFAULT_CLASSIFICATION));

    @Override
    public String id() {
        return "case-question";
    }

    @Override
    public String displayName() {
        return "Basada en caso";
    }

    @Override
    public QuestionType type() {
        return QuestionType.CASE;
    }

    @Override
    public QuestionContent generate(PluginRequest request) {
        PluginRequest r = pipeline.execute(request);
        String context = r.context();
        if (context != null && !context.isBlank() && !context.startsWith(CASE_PREFIX)) {
            context = CASE_PREFIX + context;
        }
        List<String> options = PluginFilters.fourOptions(r.options());
        return new QuestionContent(r.title(), type(), context, r.statement(), options,
                CorrectAnswerNormalizer.toLabel(r.correctAnswer(), options),
                r.justification(), r.bibliography(), r.competence(), r.topic(), r.subtopic(), r.difficulty(), null);
    }
}
