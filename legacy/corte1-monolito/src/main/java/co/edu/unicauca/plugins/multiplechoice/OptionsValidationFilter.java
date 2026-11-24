package co.edu.unicauca.plugins.multiplechoice;

import co.edu.unicauca.pipeline.Filter;
import co.edu.unicauca.plugin.api.QuestionRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OptionsValidationFilter implements Filter<QuestionRequest> {

    @Override
    public QuestionRequest process(QuestionRequest input) {
        System.out.println("Filtro 2: Validando opciones...");

        List<String> options = input.getOptions();
        if (options == null || options.size() != 4) {
            throw new IllegalArgumentException("Debe haber exactamente 4 opciones");
        }
        Set<String> unique = new HashSet<>();
        for (String opt : options) {
            if (opt == null || opt.trim().isEmpty()) {
                throw new IllegalArgumentException("Ninguna opción puede estar vacía");
            }
            if (!unique.add(opt.trim().toLowerCase())) {
                throw new IllegalArgumentException("Las opciones no pueden repetirse");
            }
        }
        System.out.println("Opciones válidas.");
        return input;
    }
}
