package co.edu.unicauca.plugins.multiplechoice;

import co.edu.unicauca.pipeline.Filter;
import co.edu.unicauca.plugin.api.QuestionRequest;

public class CorrectAnswerValidationFilter implements Filter<QuestionRequest> {

    @Override
    public QuestionRequest process(QuestionRequest input) {
        System.out.println("Filtro 4: Validando respuesta correcta...");

        String answer = input.getCorrectAnswer();
        if (answer == null || answer.trim().isEmpty()) {
            throw new IllegalArgumentException("Debe existir una respuesta correcta");
        }
        boolean found = false;
        for (String opt : input.getOptions()) {
            if (opt.trim().equalsIgnoreCase(answer.trim())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("La respuesta correcta debe coincidir con una opción");
        }
        System.out.println("Respuesta correcta válida.");
        return input;
    }
}
