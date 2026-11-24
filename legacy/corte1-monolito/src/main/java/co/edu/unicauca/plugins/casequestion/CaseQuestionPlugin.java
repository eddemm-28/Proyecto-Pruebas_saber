package co.edu.unicauca.plugins.casequestion;

import co.edu.unicauca.domain.Question;
import co.edu.unicauca.domain.QuestionDistractors;
import co.edu.unicauca.plugin.api.QuestionPlugin;
import co.edu.unicauca.plugin.api.QuestionRequest;

import java.util.List;
import java.util.UUID;

public class CaseQuestionPlugin implements QuestionPlugin {

    @Override
    public String getName() {
        return "case-question";
    }

    @Override
    public boolean supports(String type) {
        return "CASE".equalsIgnoreCase(type);
    }

    @Override
    public Question generate(QuestionRequest request) {
        System.out.println("Generando pregunta basada en caso...");

        String content = "CASO DE ESTUDIO:\n" + request.getContent()
                + "\n\nA partir del caso anterior, responda:";

        List<String> opts = request.getOptions();
        QuestionDistractors distractors = new QuestionDistractors(
                opts.get(0), opts.get(1), opts.get(2), opts.get(3));

        return new Question(
                UUID.randomUUID().toString().substring(0, 8),
                request.getTitle(),
                content,
                distractors,
                request.getCorrectAnswer(),
                Question.STATE_BORRADOR
        );
    }
}
