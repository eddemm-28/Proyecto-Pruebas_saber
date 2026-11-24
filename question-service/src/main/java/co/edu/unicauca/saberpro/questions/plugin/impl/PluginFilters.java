package co.edu.unicauca.saberpro.questions.plugin.impl;

import co.edu.unicauca.saberpro.questions.domain.model.Difficulty;
import co.edu.unicauca.saberpro.questions.plugin.api.PluginRequest;
import co.edu.unicauca.saberpro.questions.plugin.pipeline.Filter;

import java.util.List;

/**
 * Filtros reutilizables por los plugins. A diferencia del Corte 1, los filtros ya no lanzan
 * excepciones de validación: solo normalizan. La validación es única y está en el núcleo
 * ({@code QuestionValidator}), así el resultado es el mismo con o sin plugin.
 */
final class PluginFilters {

    private PluginFilters() {
    }

    /** Recorta espacios y colapsa espacios dobles en todos los textos. */
    static final Filter<PluginRequest> NORMALIZE_WHITESPACE = r -> new PluginRequest(
            clean(r.title()), clean(r.context()), clean(r.statement()),
            r.options() == null ? null : r.options().stream().map(PluginFilters::clean).toList(),
            clean(r.correctAnswer()), clean(r.justification()), clean(r.bibliography()),
            clean(r.competence()), clean(r.topic()), clean(r.subtopic()), clean(r.difficulty()), clean(r.mediaUrl()));

    /** Clasificación por defecto (heredado de ClassificationFilter): dificultad Media si no viene. */
    static final Filter<PluginRequest> DEFAULT_CLASSIFICATION = r -> new PluginRequest(
            r.title(), r.context(), r.statement(), r.options(), r.correctAnswer(), r.justification(),
            r.bibliography(), r.competence(), r.topic(), r.subtopic(),
            Difficulty.tryParse(r.difficulty()).map(Difficulty::name).orElse(Difficulty.MEDIA.name()),
            r.mediaUrl());

    static List<String> fourOptions(List<String> options) {
        return options == null ? List.of() : options;
    }

    private static String clean(String s) {
        return s == null ? null : s.trim().replaceAll("[ \\t]{2,}", " ");
    }
}
