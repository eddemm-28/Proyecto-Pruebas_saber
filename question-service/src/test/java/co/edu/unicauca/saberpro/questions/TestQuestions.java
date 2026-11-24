package co.edu.unicauca.saberpro.questions;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;

import java.util.List;

/** Datos de prueba válidos según RF-05 y RF-08–RF-13. */
public final class TestQuestions {

    private TestQuestions() {
    }

    public static QuestionContent valid() {
        return new QuestionContent(
                "Patrón State en un flujo de aprobación",
                QuestionType.MULTIPLE_CHOICE,
                "Un equipo modela el ciclo de vida de un documento que pasa por varios estados con reglas distintas.",
                "¿Qué patrón de diseño encapsula el comportamiento dependiente del estado?",
                List.of("Singleton", "State", "Adapter", "Facade"),
                "B",
                "State delega el comportamiento en un objeto por cada estado.",
                "Gamma et al. (1994). Design Patterns.",
                "Diseño de software",
                "Patrones de diseño",
                "Comportamiento",
                "MEDIA",
                null);
    }

    public static QuestionContent withOptions(List<String> options, String correct) {
        QuestionContent v = valid();
        return new QuestionContent(v.title(), v.type(), v.context(), v.statement(), options, correct,
                v.justification(), v.bibliography(), v.competence(), v.topic(), v.subtopic(), v.difficulty(), v.mediaUrl());
    }

    public static QuestionContent withStatement(String statement) {
        QuestionContent v = valid();
        return new QuestionContent(v.title(), v.type(), v.context(), statement, v.options(), v.correctOption(),
                v.justification(), v.bibliography(), v.competence(), v.topic(), v.subtopic(), v.difficulty(), v.mediaUrl());
    }

    public static QuestionContent withContext(QuestionType type, String context, String mediaUrl) {
        QuestionContent v = valid();
        return new QuestionContent(v.title(), type, context, v.statement(), v.options(), v.correctOption(),
                v.justification(), v.bibliography(), v.competence(), v.topic(), v.subtopic(), v.difficulty(), mediaUrl);
    }

    public static PluginRequest pluginRequest(String correctAnswer, String context, String mediaUrl) {
        QuestionContent v = valid();
        return new PluginRequest(v.title(), context, v.statement(), v.options(), correctAnswer,
                v.justification(), v.bibliography(), v.competence(), v.topic(), v.subtopic(), "Difícil", mediaUrl);
    }
}
