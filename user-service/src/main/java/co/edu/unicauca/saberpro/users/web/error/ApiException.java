package co.edu.unicauca.saberpro.users.web.error;

import org.springframework.http.HttpStatus;

import java.util.List;

/** Excepción de negocio que se traduce a RFC 7807 por {@link GlobalExceptionHandler}. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final List<String> errors;

    public ApiException(HttpStatus status, String message) {
        this(status, message, List.of());
    }

    public ApiException(HttpStatus status, String message, List<String> errors) {
        super(message);
        this.status = status;
        this.errors = List.copyOf(errors);
    }

    public static ApiException notFound(String message) { return new ApiException(HttpStatus.NOT_FOUND, message); }
    public static ApiException conflict(String message) { return new ApiException(HttpStatus.CONFLICT, message); }
    public static ApiException unauthorized(String message) { return new ApiException(HttpStatus.UNAUTHORIZED, message); }
    public static ApiException forbidden(String message) { return new ApiException(HttpStatus.FORBIDDEN, message); }
    public static ApiException validation(List<String> errors) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "La solicitud tiene errores de validación", errors);
    }

    public HttpStatus getStatus() { return status; }
    public List<String> getErrors() { return errors; }
}
