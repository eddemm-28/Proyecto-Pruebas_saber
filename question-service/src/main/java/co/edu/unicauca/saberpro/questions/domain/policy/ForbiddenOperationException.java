package co.edu.unicauca.saberpro.questions.domain.policy;

/** Operación no permitida para el usuario actual → HTTP 403. */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
