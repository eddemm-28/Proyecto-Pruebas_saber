package co.edu.unicauca.plugin.api;

import co.edu.unicauca.domain.Question;

public interface QuestionPlugin {

    String getName();

    boolean supports(String type);

    Question generate(QuestionRequest request);
}
