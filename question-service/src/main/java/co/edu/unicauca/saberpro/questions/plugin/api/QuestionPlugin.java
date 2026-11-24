package co.edu.unicauca.saberpro.questions.plugin.api;

import co.edu.unicauca.saberpro.questions.domain.model.QuestionContent;
import co.edu.unicauca.saberpro.questions.domain.model.QuestionType;

/**
 * Contrato del Microkernel (A14). Un plugin transforma una solicitud en un
 * {@link QuestionContent} estándar; el núcleo lo valida con las mismas reglas que las preguntas
 * manuales y lo persiste en PostgreSQL. Los plugins no conocen la base de datos.
 */
public interface QuestionPlugin {

    /** Identificador estable usado en la URL: {@code /api/plugins/{id}/questions}. */
    String id();

    String displayName();

    QuestionType type();

    QuestionContent generate(PluginRequest request);
}
