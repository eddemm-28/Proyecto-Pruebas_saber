package co.edu.unicauca.plugins.multiplechoice;

import co.edu.unicauca.pipeline.Filter;
import co.edu.unicauca.plugin.api.QuestionRequest;

public class ClassificationFilter implements Filter<QuestionRequest> {

    @Override
    public QuestionRequest process(QuestionRequest input) {
        System.out.println("Filtro 3: Clasificando pregunta...");

        if (input.getCompetence() == null || input.getCompetence().trim().isEmpty()) {
            input.setCompetence("General");
        }
        String diff = input.getDifficulty();
        if (diff == null || (!diff.equalsIgnoreCase("Fácil")
                && !diff.equalsIgnoreCase("Media")
                && !diff.equalsIgnoreCase("Difícil"))) {
            input.setDifficulty("Media");
        }
        System.out.println("Competencia: " + input.getCompetence() + " | Dificultad: " + input.getDifficulty());
        return input;
    }
}
