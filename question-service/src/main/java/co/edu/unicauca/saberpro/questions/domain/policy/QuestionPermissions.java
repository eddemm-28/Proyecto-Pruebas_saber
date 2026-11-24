package co.edu.unicauca.saberpro.questions.domain.policy;

import co.edu.unicauca.saberpro.questions.domain.state.QuestionAction;
import co.edu.unicauca.saberpro.questions.domain.state.QuestionStatus;

import java.util.Set;

/**
 * Quién puede ejecutar cada acción (complementa al patrón State, que decide <em>cuándo</em>).
 * Roles como texto para no acoplar el dominio a Spring Security.
 */
public final class QuestionPermissions {

    public static final String ADMIN = "ADMINISTRADOR";
    public static final String AUTOR = "AUTOR";
    public static final String REVISOR = "REVISOR";
    public static final String DOCENTE = "DOCENTE";

    private static final Set<String> REVIEW_ROLES = Set.of(ADMIN, REVISOR);
    private static final Set<String> READ_ALL_ROLES = Set.of(ADMIN, REVISOR, DOCENTE);
    private static final Set<String> CREATE_ROLES = Set.of(ADMIN, AUTOR);

    private QuestionPermissions() {
    }

    public static boolean canCreate(String role) {
        return CREATE_ROLES.contains(role);
    }

    public static boolean canReadAll(String role) {
        return READ_ALL_ROLES.contains(role);
    }

    public static boolean canRead(String role, boolean owner) {
        return owner || canReadAll(role);
    }

    /** RF-06: solo el autor dueño edita (y solo en Borrador, lo valida el State). */
    public static void checkCanEdit(String role, boolean owner) {
        if (!owner) {
            throw new ForbiddenOperationException("Solo el autor de la pregunta puede editarla.");
        }
    }

    public static void checkCanApply(QuestionAction action, QuestionStatus current, String role, boolean owner,
                                     String comment) {
        switch (action) {
            case SUBMIT, RETURN_TO_DRAFT -> {
                if (!owner && !ADMIN.equals(role)) {
                    throw new ForbiddenOperationException("Solo el autor puede enviar o devolver a borrador su pregunta.");
                }
            }
            case START_REVIEW, APPROVE, REJECT -> {
                if (!REVIEW_ROLES.contains(role)) {
                    throw new ForbiddenOperationException("Solo un revisor o administrador puede revisar preguntas.");
                }
                if (owner && action != QuestionAction.START_REVIEW) {
                    throw new ForbiddenOperationException("El autor no puede aprobar ni rechazar su propia pregunta.");
                }
                if (action == QuestionAction.REJECT && (comment == null || comment.isBlank())) {
                    throw new IllegalArgumentException("Para rechazar una pregunta debe indicar el motivo.");
                }
            }
            case PUBLISH -> {
                if (!ADMIN.equals(role)) {
                    throw new ForbiddenOperationException("Solo un administrador puede publicar preguntas.");
                }
            }
            case ARCHIVE -> {
                boolean ownerDraft = owner && current == QuestionStatus.BORRADOR;
                if (!ADMIN.equals(role) && !ownerDraft) {
                    throw new ForbiddenOperationException(
                            "Solo un administrador puede archivar; el autor solo puede archivar sus borradores.");
                }
            }
            default -> throw new ForbiddenOperationException("Acción no soportada: " + action);
        }
    }
}
