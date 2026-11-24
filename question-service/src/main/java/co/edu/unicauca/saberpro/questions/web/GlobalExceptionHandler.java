package co.edu.unicauca.saberpro.questions.web;

import co.edu.unicauca.saberpro.questions.domain.policy.ForbiddenOperationException;
import co.edu.unicauca.saberpro.questions.domain.state.InvalidStateTransitionException;
import co.edu.unicauca.saberpro.questions.domain.validation.QuestionValidationException;
import co.edu.unicauca.saberpro.questions.service.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Errores en formato RFC 7807. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(QuestionValidationException.class)
    ProblemDetail validation(QuestionValidationException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail beanValidation(MethodArgumentNotValidException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "La solicitud tiene errores de validación",
                ex.getBindingResult().getFieldErrors().stream().map(f -> f.getField() + ": " + f.getDefaultMessage()).toList());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    ProblemDetail invalidTransition(InvalidStateTransitionException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, ex.getMessage(), List.of());
        pd.setProperty("currentState", ex.getFrom().name());
        return pd;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail optimistic(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT, "La pregunta fue modificada por otra persona. Recargue e intente de nuevo.", List.of());
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    ProblemDetail forbidden(RuntimeException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ProblemDetail badRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, List<String> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        if (!errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return pd;
    }
}
