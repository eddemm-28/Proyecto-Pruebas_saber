package co.edu.unicauca.plugins.multiplechoice;

import co.edu.unicauca.pipeline.Filter;
import co.edu.unicauca.plugin.api.QuestionRequest;

public class ContentValidationFilter implements Filter<QuestionRequest> {

    @Override
    public QuestionRequest process(QuestionRequest input) {
        System.out.println("Filtro 1: Validando contenido...");

        if (input.getContent() == null || input.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("El contenido no puede estar vacío");
        }
        if (input.getContent().trim().length() < 10) {
            throw new IllegalArgumentException("El contenido es demasiado corto");
        }
        if (input.getTitle() == null || input.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("El título no puede estar vacío");
        }
        System.out.println("Contenido válido.");
        return input;
    }
}
