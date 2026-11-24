package co.edu.unicauca.saberpro.simulations.infrastructure.web;

import co.edu.unicauca.saberpro.simulations.domain.exception.ActiveSimulationExistsException;
import co.edu.unicauca.saberpro.simulations.domain.exception.InsufficientQuestionsException;
import co.edu.unicauca.saberpro.simulations.domain.exception.InvalidSimulationConfigException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationAccessDeniedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationClosedException;
import co.edu.unicauca.saberpro.simulations.domain.exception.SimulationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Traduce excepciones de dominio a RFC 7807. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidSimulationConfigException.class)
    ProblemDetail invalidConfig(InvalidSimulationConfigException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(InsufficientQuestionsException.class)
    ProblemDetail insufficient(InsufficientQuestionsException ex) {
        ProblemDetail pd = problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), List.of());
        pd.setProperty("available", ex.getAvailable());
        return pd;
    }

    @ExceptionHandler(ActiveSimulationExistsException.class)
    ProblemDetail active(ActiveSimulationExistsException ex) {
        ProblemDetail pd = problem(HttpStatus.CONFLICT, ex.getMessage(), List.of());
        pd.setProperty("activeSimulationId", ex.getActiveSimulationId());
        return pd;
    }

    @ExceptionHandler({SimulationClosedException.class, OptimisticLockingFailureException.class,
            DataIntegrityViolationException.class})
    ProblemDetail conflict(RuntimeException ex) {
        return problem(HttpStatus.CONFLICT, ex instanceof SimulationClosedException ? ex.getMessage()
                : "Conflicto de concurrencia; intente de nuevo.", List.of());
    }

    @ExceptionHandler(SimulationNotFoundException.class)
    ProblemDetail notFound(SimulationNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
    }

    @ExceptionHandler({SimulationAccessDeniedException.class, AccessDeniedException.class})
    ProblemDetail forbidden(RuntimeException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage(), List.of());
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
